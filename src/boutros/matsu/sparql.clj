(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter concat group-by max min])
  (:require [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures
; -----------------------------------------------------------------------------

(defn empty-query []
  "Query-map constructor

  Each key is populated with the following map:

    {:tag nil :content [] :bounds ["" ""] :sep " "}

  The :content field can contain other such maps, so the datastructure
  can be arbiritarily deeply nested."
  {:local-prefixes {}
   :base nil
   :from nil
   :from-named nil
   :query-form  nil
   :where nil
   :order-by nil
   :group-by nil
   :having nil
   :offset nil
   :limit nil})

(def prefixes (atom {}))

; -----------------------------------------------------------------------------
; Namespace functions
; -----------------------------------------------------------------------------

(defn register-namespaces [m]
  (swap! prefixes merge m))

(defn- ns-or-error
  "Resolves prefixes. Throws an error if the namespace cannot be resolved."
  [k m]
  (if-let [v (k m)] ; check for query-local prefixes first
    v
    (if-let [v (k @prefixes)]
      v
     (throw (IllegalArgumentException. (str "Cannot resolve namespace: " k))))))

; -----------------------------------------------------------------------------
; Main macros
; -----------------------------------------------------------------------------

(defmacro defquery
  "Defines a query-map and binds it to a var"
  [name & body]
  `(let [q# (-> (~empty-query) ~@body)]
    (def ~name q#)))

(defmacro query
  "Let you craft a SPARQL-query by composing functions

    ex:  (query
           (select :s)
           (where :s :p :p))

  Which will return a valid SPARQL 1.1 string:
    'SELECT ?s WHERE { ?s ?p ?o }'

  The macro can also be called with a query-map as its first argument,
  allowing you to work on saved queries."
  [q & body]
  (if (list? q)
    `(-> (empty-query) ~q ~@body (compile-query))
    `(-> ~q ~@body (compile-query))))

(defmacro query-with-prefixes
  [m & body]
  `(query (assoc (empty-query) :local-prefixes ~m) ~@body))

; -----------------------------------------------------------------------------
; Encoder
; -----------------------------------------------------------------------------

(declare sub-compiler)

(defn encode
  "Encodes keywords to ?-prefixed variables and other values to RDF literals
  when applicable.

  Vectors are interpreted differently according to their contents:

    [keyword string] => prefixed name
    [string keyword] => string with language tag
    [keyword]        => <keyword> - to be used with BASE
    [map keyword]    => (:content map) AS keyword

  Maps are expaned and compiled according to its contents, tag, bounds and
  separator"
  [x]
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
                         (keyword? b)) (into ["("]
                                             (conj (sub-compiler a) " AS " (encode b) ")" ))
                    :else (str (name a) \: b)))
    (map? x) (sub-compiler x)
    :else (throw (Exception.
                   (format "Don't know how to encode %s in an SPARQL context" x)))))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into a vectors of strings, or nil if the
; particular function is not used in the query

(defn- compiler [q part]
  (when-let [m (part q)]
    (sub-compiler m)))

(defn- sub-compiler [m]
  (conj []
        (:tag m)
        (first (:bounds m))
        (interpose
          (:sep m)
          (map encode (:content m)))
        (last (:bounds m))))


;; Functions to add namespaces to query string

(defn- infer-prefixes
  "Finds all occurences of the pattern 'prefix:name' in the query string.
  Returns the query string with the namespaces prefixed."
  [m s]
  (str
    (apply str (apply sorted-set
                      (for [[_ p] (re-seq #"(\b[a-zA-Z0-9]+):[a-zA-Z]" s) :when (not= p (name :mailto))]
                        (str "PREFIX " p ": " (ns-or-error (keyword p) m) " " ))))
    s))

(defn- add-base
  "Prefixes the query string with 'BASE <uri>'"
  [uri s]
  (if (nil? uri)
    s
    (str  "BASE " (encode uri) " " s)))


;; Main compiler function

(defn compile-query
  "Takes a map representing SPARQL graph patterns, bindings and modifiers and
  returns a vaild SPARQL 1.1 query string"
  [q]
  (let [base (:base q) local-prefixes (:local-prefixes q)]
    (->> (conj []
               (for [part [:query-form :from :from-named :where :order-by
                           :limit :offset :group-by :having]]
                (compiler q part)))
         (flatten)
         (string/join)
         (infer-prefixes local-prefixes)
         (add-base base)
         (string/trim))))


;; ----------------------------------------------------------------------------
;; SPARQL query DSL
;; ----------------------------------------------------------------------------


;; Matsu (i.e non-SPARQL) syntax helpers

(defn raw [string]
  {:tag "" :sep "" :bounds "" :content string })

(defn with-prefixes [q m]
  (assoc q :local-prefixes m))


;; Namespaces-related

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
  (assoc q :where {:tag "WHERE" :content (vec more) :bounds [" { " " } "] :sep " "}))

(defn where-
  "Where clause without the optional WHERE keyword"
  [q & more]
  (assoc q :where {:tag "" :content (vec more) :bounds ["{ " " } "] :sep " "}))

(defn group
  "Delimits a graph pattern within curly braces. (Not a SPARQL keyword.)"
  [& more]
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

(defn filter-
  "Function to be used when FILTER followed by another SPARQL function.

  ex: (filter- (is-iri :p))"
  [& more]
  {:tag "FILTER" :content (vec more) :bounds [" " ""] :sep " "})

(defn filter-not-exists [& more]
  {:tag "FILTER NOT EXISTS " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn filter-exists [& more]
  {:tag "FILTER EXISTS " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn minus [& more]
  {:tag "MINUS " :content (vec more) :bounds ["{ " " }"] :sep " "})


;; Specifying datasets

(defn from [q graph]
  (assoc q :from {:tag "FROM" :content [graph] :bounds [" " " "] :sep " "}))

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
  (assoc q :order-by
    {:tag "ORDER BY" :bounds [" "] :sep " " :content (vec expr)}))

(defn order-by-desc [q v]
  (order-by q (desc v)))

;; Aggregation

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
;(defn group-concat)


;; Assingment

(defn bind [v]
  (let [[expr name] v]
    {:tag "BIND" :content [expr 'AS  name]  :bounds ["(" ")"] :sep " "}))

;(defn values)


;; Functional forms

(defn bound [v] {:tag "bound" :content [v] :bounds ["(" ")"] :sep " "})

(defn !bound [v] {:tag "!bound" :content [v] :bounds ["(" ")"] :sep " "})


;; Functions on strings

(defn concat [& more]
  {:tag "CONCAT" :content (vec more) :bounds ["(" ")"] :sep ", "})

(defn regex [v regex & flags]
  (if flags
    {:tag "regex" :bounds ["(" ")"] :sep ", " :content [v regex (first flags)]}
    {:tag "regex" :bounds ["(" ")"] :sep ", " :content [v regex]}))

;(defn lang-matches [& more]
;  {:tag "langMatches" :content (vec more) :bounds ["(" ")"] :sep ", "})

;;STRLEN, SUBSTR, UCASE, LCASE, STRSTARS, STRENDS, CONTAINS, STRBEFORE, STRAFTER, ENCODE_FOR_URI
;;langMatches, REGEX, REPLACE


;; Functions on RDF terms
;;  lang, datatype, IRI, BNODE, STRDT
;; STRLANG, UUID, STRUUID
(defn same-term [& more]
  {:tag "sameTerm" :content (vec more) :bounds ["(" ")"] :sep ", "})

(defn !same-term [& more]
  {:tag "!sameTerm" :content (vec more) :bounds ["(" ")"] :sep ", "})

(defn is-iri [term]
  {:tag "isIRI" :content [term] :bounds ["(" ")"] :sep ""})

(defn is-blank [term]
  {:tag "isBlank" :content [term] :bounds ["(" ")"] :sep ""})

(defn is-literal [term]
  {:tag "isLiteral" :content [term] :bounds ["(" ")"] :sep ""})

(defn is-numeric [term]
  {:tag "isNumeric" :content [term] :bounds ["(" ")"] :sep ""})

(defn is-literal [term]
  {:tag "isLiteral" :content [term] :bounds ["(" ")"] :sep ""})

;; clojure.core/str is to usefull
(defn str2 [term]
  {:tag "str" :content [term] :bounds ["(" ")"] :sep ""})

;; Functions on Numerics

;; Functions on Dates and Times

;; Hash functions
