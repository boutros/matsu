(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter concat group-by])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures
; -----------------------------------------------------------------------------

(defn empty-query []
  "Query-map constructor

  Each key is populated with the following map:

    {:tag nil :content [] :bounds ["" ""] :separator " "}

  The :content field can contain other such maps, so the datastructure
  can be arbiritarily deeply nested. "
  {:base nil
   :from nil
   :from-named []
   :query-form  nil
   :where nil
   :order-by []
   :group-by []
   :having []
   :offset nil
   :limit nil})

(def prefixes (atom {}))

; -----------------------------------------------------------------------------
; Namespace functions
; -----------------------------------------------------------------------------

(defn register-namespaces [m]
  {:pre [(map? m)]}
  (swap! prefixes merge m))

(defn- ns-or-error [k]
  {:pre [(keyword? k)]}
  (if-let [v (k @prefixes)]
    v
   (throw (IllegalArgumentException. (str "Cannot resolve namespace: " k)))))

; -----------------------------------------------------------------------------
; Macros
; -----------------------------------------------------------------------------

(defmacro defquery [name & body]
          "Defines a query-map and binds it to a var"
          `(let [q# (-> (~empty-query) ~@body)]
            (def ~name q#)))

(defmacro query [q & body]
          "Let you craft a SPARQL-query by composing functions, like this:
            (query
              (select :s)
              (where :s :p :p))
          And returns a valid SPARQL 1.1 string:
            'SELECT ?s WHERE { ?s ?p ?o }'

          The macro can also be called with a query-map as its first argument,
          alowing you to modify saved queries."
          (if (list? q)
            `(-> (empty-query) ~q ~@body (compile-query))
            `(-> ~q ~@body (compile-query))))

; -----------------------------------------------------------------------------
; Util functions
; -----------------------------------------------------------------------------
(declare sub-compiler)

(defn encode [x]
  "Encodes keywords to ?-prefixed variables and other values to RDF literals
  when applicable. Vectors are treated as namespace-qualified if first value is
  a keyword, or as string with language tag if the second value is a keyword.

  Maps are used for special cases, to avoid compilation inside where-clauses."
  (cond
    (char? x) x
    (symbol? x) x
    (keyword? x) (str \? (name x))
    (integer? x) x                    ;(str  \" x \" "^^xsd:integer")
    (float? x) x                      ;(str  \" x \" "^^xsd:decimal")
    (true? x) x                       ;"\"true\"^^xsd:boolean"
    (false? x) x                      ;"\"false\"^^xsd:boolean"
    (string? x) (str \" x \" )
    (= java.net.URI (type x)) (str "<" x ">")
    ;(= java.util.Date (type x)) (str \" x \" "^^xsd:dateTime")
    (vector? x) (let [[a b] x]
                  (cond
                    (not b) (str \< (name a) \>)
                    (string? a) (str \" a "\"@" (name b))
                    (and (map? a)
                         (keyword? b)) (into ["("] (conj (sub-compiler a)
                                                         " AS "
                                                         (encode b)
                                                         ")" ))
                    :else (str (name a) \: b)))
    (map? x) (if (:tag x)
               (sub-compiler x)
               (:content x))
    :else (throw (Exception. (format "Don't know how to encode %s into RDF literal!" x)))))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into a vectors of strings, or nil if the
; particular function is not used in the query


(defn- compiler [q what]
  (when-let [m (what q)]
    (conj []
          (:tag m)
          (first (:bounds m))
          (interpose
                 (:separator m)
                 (map encode (:content m)))
          (last (:bounds m)))))

(defn- sub-compiler [m]
  (conj []
        (:tag m)
        (first (:bounds m))
        (interpose
          (:separator m)
          (map encode (:content m)))
        (last (:bounds m))))

(defn- query-form-compile [q]
  (compiler q :query-form))

(defn- where-compile [q]
  (compiler q :where))

(defn- from-compile [q]
  (when-not (nil? (:from q))
    (conj ["FROM"] (encode (:from q)))))

(defn- from-named-compile [q]
  (when-let [graphs (seq (:from-named q))]
    (conj []
      (for [g graphs] ["FROM NAMED" (encode g)]))))


(defn- limit-compile [q]
  (when-let [n (:limit q)]
    (conj [] "LIMIT" n)))

(defn- offset-compile [q]
  (when-let [n (:offset q)]
    (conj [] "OFFSET" n)))

(defn- order-by-compile [q]
  (when-let [xs (seq (:order-by q))]
    (conj ["ORDER BY"] (vec (map encode xs)))))

(defn- group-by-compile [q]
  (when-let [xs (seq (:group-by q))]
    (conj ["GROUP BY"] (vec (map encode xs)))))

(defn- having-compile [q]
  (when-let [xs (seq (:having q))]
    (conj ["HAVING("] (vec (map encode xs)) ")" )))

(defn- infer-prefixes [s]
  (str
    (apply str (apply sorted-set (for [[_ p] (re-seq #"(\b[a-zA-Z0-9]+):[a-zA-Z]" s) :when (not= p (name :mailto))]
             (str "PREFIX " p ": " (ns-or-error (keyword p)) " " ))))
    s))

(defn- add-base [uri s]
  (if (nil? uri)
    s
    (str  "BASE " (encode uri) " " s)))

(defn compile-query [q]
  {:pre [(map? q)]
   :post [(string? %)]}
  "Takes a map representing SPARQL graph patterns, bindings and modifiers and
  returns a vaild SPARQL 1.1 query string"
  (let [base (get q :base)]
    (->> (conj []
               (query-form-compile q)
               (from-compile q)
               (from-named-compile q)
               (where-compile q)
               (order-by-compile q)
               (limit-compile q)
               (offset-compile q)
               (group-by-compile q)
               (having-compile q))
         (flatten)
         (remove nil?)
         (string/join "")
         (string/trim)
         (infer-prefixes)
         (add-base base))))

;; ----------------------------------------------------------------------------
;; SPARQL query DSL functions
;; ----------------------------------------------------------------------------

; These all takes a map of the query and returns a modified query-map:

;; Query forms

(defn select [q & more]
  (assoc q :query-form {:tag "SELECT" :content (vec more)
                        :bounds [" "] :separator " "}))

(defn select-distinct [q & more]
  (assoc q :query-form {:tag "SELECT DISTINCT" :content (vec more)
                        :bounds [" "] :separator " "}))

(defn construct [q & more]
  (assoc q :query-form {:tag "CONSTRUCT" :content (vec more)
                        :bounds [" { " " } "] :separator " "}))

;; Where

(defn where [q & more]
  (assoc q :where {:tag "WHERE" :content (vec more)
                   :bounds [" { " " } "] :separator " "}))

(defn base [q uri]
  (assoc q :base uri))

(defn ask [q & vars]
  (assoc q :query-form {:form "ASK" :content (vec vars)}))

(defn describe [q & vars]
  (assoc q :query-form {:form "DESCRIBE" :content (vec vars)}))

(defn from [q graph]
  (assoc q :from graph))

(defn from-named [q & graphs]
  (assoc q :from-named (vec graphs)))




(defn select-reduced [q & vars]
  (assoc q :query-form {:form "SELECT REDUCED" :content (vec vars)}))


(defn limit [q & n]
  (assoc q :limit n))

(defn offset [q & n]
  (assoc q :offset n))

(defn order-by [q & expr]
  (assoc q :order-by (vec expr)))

(declare desc)
(defn order-by-desc [q v]
  (order-by q (desc v)))

;; Aggregation

(defn group-by [q & expr]
  (assoc q :group-by (vec expr)))

(defn having [q & expr]
  (assoc q :having (vec expr)))

;;; Functions which return a map to be nested in the query-map

;; Assingment

(defn bind [v]
  (let [[expr name] v]
    {:tag "BIND" :content [expr 'AS  name]  :bounds ["(" ")"] :separator " "}))


;; Functions on strings

(defn concat [& more]
  {:tag "CONCAT" :content (vec more) :bounds ["(" ")"] :separator ", "})


;; Graph pattern matching

(defn group [& more]
  {:tag "" :content (vec more) :bounds ["{ " " }"] :separator " "})

(defn optional [& more]
  {:tag "OPTIONAL " :content (vec more) :bounds ["{ " " }"] :separator " "})

(defn union [& more]
  {:tag "" :content (interpose 'UNION (vec more)) :bounds [""] :separator " "})

(defn graph [& vars]
  {:content (str "GRAPH " (string/join " " (vec (map encode vars))))})


;; Negation

(defn filter [& more]
  {:tag "FILTER" :content (vec more) :bounds ["(" ")"] :separator " "})

(defn filter-not-exists [& more]
  {:tag "FILTER NOT EXISTS " :content (vec more) :bounds ["{ " " }"] :separator " "})

(defn filter-exists [& more]
  {:tag "FILTER EXISTS " :content (vec more) :bounds ["{ " " }"] :separator " "})

(defn minus [& more]
  {:tag "MINUS " :content (vec more) :bounds ["{ " " }"] :separator " "})

(defn filter-regex [v regex & flags]
  {:content (str "FILTER regex(" (encode v)
                 ", "
                 (encode regex)
                 (when flags (str ", " (encode (first flags))))
                 ")" )})


;;;;;;
(defn raw [string]
  {:tag nil :separator "" :bounds "" :content string })

(defn desc [v]
  {:content (str "DESC(" (encode v) ")" )})

(defn asc [v]
  {:content (str "ASC(" (encode v) ")" )})

(defn sum [v]
  {:content (str "SUM(" (encode v) ")" )})

(defn avg [v]
  {:content (str "AVG(" (encode v) ")" )})


(defn bound [v]
  {:content (str "bound(" (encode v) ")" )})

(defn !bound [v]
  {:content (str "!bound(" (encode v) ")" )})

(defn same-term [& vars]
  {:content (str "sameTerm("(string/join ", " (map encode vars)) ")") })

(defn !same-term [& vars]
  {:content (str "!sameTerm("(string/join ", " (map encode vars)) ")") })