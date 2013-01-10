(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter concat group-by max min])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures
; -----------------------------------------------------------------------------

(defn empty-query []
  "Query-map constructor

  Each key is populated with the following map:

    {:tag nil :content [] :bounds ["" ""] :sep " "}

  The :content field can contain other such maps, so the datastructure
  can be arbiritarily deeply nested. "
  {:base nil
   :from nil
   :from-named nil
   :query-form  nil
   :where nil
   :order-by []
   :group-by []
   :having []
   :offset nil
   :limit nil})

(def prefixes (atom {}))

;; Used for specifying query-local namespaces with the function with-prefixes
(def local-prefixes {})

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
    (map? x) (if (:tag x) (sub-compiler x) "sprudlevann!")
    :else (throw (Exception.
                   (format "Don't know how to encode %s in an SPARQL context" x)))))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into a vectors of strings, or nil if the
; particular function is not used in the query

(defn- compiler [q what]
  (when-let [m (what q)]
    (sub-compiler m)))

(defn- sub-compiler [m]
  (conj []
        (:tag m)
        (first (:bounds m))
        (interpose
          (:sep m)
          (map encode (:content m)))
        (last (:bounds m))))

(defn- query-form-compile [q]
  (compiler q :query-form))

(defn- where-compile [q]
  (compiler q :where))

(defn- from-compile [q]
  (compiler q :from))

(defn- from-named-compile [q]
  (compiler q :from-named))

(defn- group-by-compile [q]
  (compiler q :group-by))

(defn- having-compile [q]
  (compiler q :having))

(defn- limit-compile [q]
  (compiler q :limit))

(defn- offset-compile [q]
  (compiler q :offset))

(defn- order-by-compile [q]
  (compiler q :order-by))


;; Add namespaces to query

(defn- infer-prefixes [s]
  (str
    (apply str (apply sorted-set (for [[_ p] (re-seq #"(\b[a-zA-Z0-9]+):[a-zA-Z]" s) :when (not= p (name :mailto))]
             (str "PREFIX " p ": " (ns-or-error (keyword p)) " " ))))
    s))

(defn- add-base [uri s]
  (if (nil? uri)
    s
    (str  "BASE " (encode uri) " " s)))


;; Main compiler function

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

;; Matsu syntax helpers

(defn raw [string]
  {:tag "" :sep "" :bounds "" :content string })

;(defn with-prefixes)

;; Namespaces

(defn base [q uri]
  (assoc q :base uri))


;; Query forms

(defn select [q & more]
  (assoc q :query-form {:tag "SELECT" :content (vec more)
                        :bounds [" "] :sep " "}))

(defn select-distinct [q & more]
  (assoc q :query-form {:tag "SELECT DISTINCT" :content (vec more)
                        :bounds [" "] :sep " "}))

(defn select-reduced [q & more]
  (assoc q :query-form {:tag "SELECT REDUCED" :content (vec more)
                        :bounds [" "] :sep " "}))

(defn construct [q & more]
  (assoc q :query-form {:tag "CONSTRUCT" :content (vec more)
                        :bounds [" { " " } "] :sep " "}))

(defn ask [q & more]
  (assoc q :query-form {:tag "ASK" :content (vec more)
                      :bounds [" { " " } "] :sep " "}))

(defn describe [q & more]
  (assoc q :query-form {:tag "DESCRIBE" :content (vec more)
                        :bounds [" "] :sep " "}))


;; Graph pattern matching

(defn where [q & more]
  (assoc q :where {:tag "WHERE" :content (vec more)
                   :bounds [" { " " } "] :sep " "}))

(defn where- [q & more]
  "where clause without the optional WHERE keyword"
  (assoc q :where {:tag "" :content (vec more)
                   :bounds [" { " " } "] :sep " "}))

(defn group [& more]
  "group delimits a graph pattern within braces: { }"
  {:tag "" :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn optional [& more]
  {:tag "OPTIONAL " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn union [& more]
  {:tag "" :content (interpose 'UNION (vec more)) :bounds [""] :sep " "})

(defn graph [& more]
  {:tag "GRAPH " :content (vec more) :bounds [""] :sep " "})


;; Negation

(defn filter [& more]
  {:tag "FILTER" :content (vec more) :bounds ["(" ")"] :sep " "})

(defn filter-not-exists [& more]
  {:tag "FILTER NOT EXISTS " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn filter-exists [& more]
  {:tag "FILTER EXISTS " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn minus [& more]
  {:tag "MINUS " :content (vec more) :bounds ["{ " " }"] :sep " "})


;; Specifying datasets

(defn from [q graph]
  (assoc q :from {:tag "FROM" :content [graph]
                  :bounds [" " " "] :sep " "}))

(defn from-named [q & graphs]
  (assoc q :from-named {:tag "" :bounds ["" " "] :sep " "
                        :content (interleave (repeat (raw "FROM NAMED")) (vec graphs))}))


;; Solution sequences

(defn limit [q n]
  (assoc q :limit {:tag "LIMIT" :bounds [" "] :sep " " :content [n]}))

(defn offset [q n]
  (assoc q :offset {:tag "OFFSET" :bounds [" "] :sep " " :content [n]}))

(defn desc [v] {:tag "DESC" :bounds ["(" ")"] :sep " " :content [v]})

(defn asc [v] {:tag "ASC" :bounds ["(" ")"] :sep " " :content [v]})

(defn order-by [q & expr]
  (assoc q :order-by {:tag "ORDER BY" :bounds [" "] :sep " "
                      :content (vec expr)}))

(defn order-by-desc [q v]
  (order-by q (desc v)))

;; Aggregation
;; COUNT, GROUP_CONCAT, and SAMPLE
(defn group-by [q & expr]
  (assoc q :group-by {:tag "GROUP BY" :bounds [" "] :sep " "
                      :content (vec expr)}))

(defn having [q & expr]
  (assoc q :having {:tag "HAVING" :bounds ["(" ")"] :sep " "
                    :content (vec expr)}))

(defn sum [v] {:tag "SUM" :bounds ["(" ")"] :sep " " :content [v]})

(defn avg [v] {:tag "AVG" :bounds ["(" ")"] :sep " " :content [v]})

(defn min [v] {:tag "MIN" :bounds ["(" ")"] :sep " " :content [v]})

(defn max [v] {:tag "MAX" :bounds ["(" ")"] :sep " " :content [v]})

;(defn sample [v])
;(defn count)
;(defn group_concat)


;; Assingment

(defn bind [v]
  (let [[expr name] v]
    {:tag "BIND" :content [expr 'AS  name]  :bounds ["(" ")"] :sep " "}))

;(defn values)



;; Functional forms

(defn bound [v]
  {:tag "bound" :content [v] :bounds ["(" ")"] :sep " "})

(defn !bound [v]
  {:tag "!bound" :content [v] :bounds ["(" ")"] :sep " "})


;; Functions on strings

(defn concat [& more]
  {:tag "CONCAT" :content (vec more) :bounds ["(" ")"] :sep ", "})

(defn filter-regex [v regex & flags]
  (if flags
    {:tag "FILTER regex" :bounds ["(" ")"] :sep ", "
    :content [v regex (first flags)]}
    {:tag "FILTER regex" :bounds ["(" ")"] :sep ", "
    :content [v regex]}))

;;STRLEN, SUBSTR, UCASE, LCASE, STRSTARS, STRENDS, CONTAINS, STRBEFORE, STRAFTER, ENCODE_FOR_URI
;;langMatches, REGEX, REPLACE


;; Functions on RDF terms
;; isIRI, isBlank, isLiteral, isNumeric, str, lang, datatype, IRI, BNODE, STRDT
;; STRLANG, UUID, STRUUID
(defn same-term [& more]
  {:tag "sameTerm" :content (vec more) :bounds ["(" ")"] :sep ", "})

(defn !same-term [& more]
  {:tag "!sameTerm" :content (vec more) :bounds ["(" ")"] :sep ", "})

;; Functions on Numerics

;; Functions on Dates and Times

;; Hash functions
