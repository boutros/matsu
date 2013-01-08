(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter concat group-by])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures
; -----------------------------------------------------------------------------

(defn empty-query []
  "query-map constructor"
  {:base nil
   :from nil
   :from-named []
   :query-form {:form nil :content []}
   :where []
   ;:where {:keyword true :content [] }
   :order-by []
   :group-by []
   :having []
   :offset nil
   :limit nil})

(def PREFIXES (atom {}))

; -----------------------------------------------------------------------------
; Namespace functions
; -----------------------------------------------------------------------------

(defn register-namespaces [m]
  {:pre [(map? m)]}
  (swap! PREFIXES merge m))

(defn- ns-or-error [k]
  {:pre [(keyword? k)]}
  (if-let [v (k @PREFIXES)]
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
    (vector? x) (cond
                  (not (next x)) (str \< (name (first x)) \>)
                  (string? (first x)) (str \" (first x) "\"@" (name (second x)))
                  (and (map? (first x)) (keyword? (second x))) (str "( " (:content (first x)) " AS " (encode (second x)) " )")
                  :else (str (name (first x)) \: (second x)))
    (map? x) (:content x)
    :else (throw (Exception. (format "Don't know how to encode %s into RDF literal!" x)))))

(defn encode-with-comma [x]
  (str (encode x) ","))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into a vectors of strings, or nil if the
; particular function is not used in the query

(defn- group-subcompile [v]
  {:pre [(vector? v)]
   :post [(vector? %)]}
  (conj ["{"] (map encode v) "}"))

(defn- query-form-compile [q]
  {:pre [(map? q)]}
  (when-let [form (get-in q [:query-form :form])]
    (conj []
      (case form
        "ASK" (conj ["ASK"] (group-subcompile (get-in q [:query-form :content])))
        "CONSTRUCT" (conj ["CONSTRUCT" "{"] (vec (map encode (get-in q [:query-form :content]))) "}")
        (conj [] (get-in q [:query-form :form])
                (vec (map encode (get-in q [:query-form :content]))))))))

(defn- from-compile [q]
  {:pre [(map? q)]}
  (when-not (nil? (:from q))
    (conj ["FROM"] (encode (:from q)))))

(defn- from-named-compile [q]
  {:pre [(map? q)]}
  (when-let [graphs (seq (:from-named q))]
    (conj []
      (for [g graphs] ["FROM NAMED" (encode g)]))))

(defn- where-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:where q))]
    (conj ["WHERE" "{"] (vec (map encode xs)) "}")))

(defn- limit-compile [q]
  {:pre [(map? q)]}
  (when-let [n (:limit q)]
    (conj [] "LIMIT" n)))

(defn- offset-compile [q]
  {:pre [(map? q)]}
  (when-let [n (:offset q)]
    (conj [] "OFFSET" n)))

(defn- order-by-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:order-by q))]
    (conj ["ORDER BY"] (vec (map encode xs)))))

(defn- group-by-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:group-by q))]
    (conj ["GROUP BY"] (vec (map encode xs)))))

(defn- having-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:having q))]
    (conj ["HAVING("] (vec (map encode xs)) ")" )))

(defn- infer-prefixes [s]
  {:pre [(string? s)]
   :post [(string? %)]}
  (str
    (apply str (apply sorted-set (for [[_ p] (re-seq #"(\b\w+):\w" s) :when (not= p (name :mailto))]
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
         (string/join " ")
         (infer-prefixes)
         (add-base base))))

; -----------------------------------------------------------------------------
; SPARQL query DSL functions
; -----------------------------------------------------------------------------

; These all takes a map of the query and returns a modified query-map:

(declare desc)

(defn base [q uri]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :base uri))

(defn ask [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "ASK" :content (vec vars)}))

(defn from [q graph]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :from graph))

(defn from-named [q & graphs]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :from-named (vec graphs)))

(defn select [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "SELECT" :content (vec vars)}))

(defn select-distinct [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "SELECT DISTINCT" :content (vec vars)}))

(defn select-reduced [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "SELECT REDUCED" :content (vec vars)}))

(defn construct [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "CONSTRUCT" :content (vec vars)}))

(defn where [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (update-in q [:where] into vars))

(defn limit [q & n]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :limit n))

(defn offset [q & n]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :offset n))

(defn order-by [q & expr]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :order-by (vec expr)))

(defn order-by-desc [q v]
  (order-by q (desc v)))

(defn group-by [q & expr]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :group-by (vec expr)))

(defn having [q & expr]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :having (vec expr)))

; Functions which compile inline groups inside select/where clauses:

(defn union [& groups]
  {:post [(map? %)]}
  {:content (string/join " UNION " (vec (map encode groups))) })

(defn group [& vars]
  {:post [(map? %)]}
   {:content (str "{ " (string/join " " (vec (map encode vars))) " }" )})

(defn graph [& vars]
  {:post [(map? %)]}
   {:content (str "GRAPH " (string/join " " (vec (map encode vars))))})

(defn filter [& vars]
  {:post [(map? %)]}
   {:content (str "FILTER(" (string/join " " (vec (map encode vars))) ")" )})

(defn filter-not-exists [& vars]
  {:post [(map? %)]}
   {:content (str "FILTER NOT EXISTS { " (string/join " " (vec (map encode vars))) " }" )})

(defn filter-exists [& vars]
  {:post [(map? %)]}
   {:content (str "FILTER EXISTS { " (string/join " " (vec (map encode vars))) " }" )})

(defn minus [& vars]
  {:post [(map? %)]}
   {:content (str "MINUS { " (string/join " " (vec (map encode vars))) " }" )})

(defn filter-regex [v regex & flags]
  {:post [(map? %)]}
   {:content (str "FILTER regex(" (encode v)
                  ", "
                  (encode regex)
                  (when flags (str ", " (encode (first flags))))
                  ")" )})

(defn optional [& vars]
  {:post [(map? %)]}
   {:content (str "OPTIONAL { " (string/join " " (vec (map encode vars))) " }" )})

(defn raw [string]
  {:pre [(string? string)]
   :post [(map? %)]}
   {:content string })

(defn desc [v]
  {:post [(map? %)]}
   {:content (str "DESC(" (encode v) ")" )})

(defn asc [v]
  {:post [(map? %)]}
   {:content (str "ASC(" (encode v) ")" )})

(defn sum [v]
  {:post [(map? %)]}
   {:content (str "SUM(" (encode v) ")" )})

(defn avg [v]
  {:post [(map? %)]}
   {:content (str "AVG(" (encode v) ")" )})

(defn concat [& vars]
  {:post [(map? %)]}
  {:content (str "CONCAT("(string/join " " (map encode-with-comma (butlast vars)))
                 " " (encode (last vars))")") })

(defn bind [v]
  {:pre [(vector? v)]
   :post [(map? %)]}
  (let [[expr name] v]
    {:content (str "BIND(" (encode expr) " AS " (encode name) ")") }))
