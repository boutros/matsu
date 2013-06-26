(ns boutros.matsu.sparql
  "Matsu SPARQL query DSL functions"
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [clojure.walk :refer [postwalk-replace]]
            [boutros.matsu.compiler :refer [compile-query encode]]
            [boutros.matsu.core :refer [empty-query]]))


;; ----------------------------------------------------------------------------
;; Vars (shortcuts)
;; ----------------------------------------------------------------------------

(def a \a) ; abberviation for rdf:type

;; Vars to avoid quoting in queries:

(def && '&&)

(def || '||)

(def != '!=)

;; ----------------------------------------------------------------------------
;; Main macros
;; ----------------------------------------------------------------------------

(defmacro defquery
  "Defines a query-map and binds it to a function taking [args]"
  [name args & body]
  `(defn ~name [~@args]
     (query
       (-> (~empty-query) ~@body))))

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


;; ----------------------------------------------------------------------------
;; SPARQL query DSL
;; ----------------------------------------------------------------------------

;; Matsu (i.e non-SPARQL) syntax helpers

(defn raw [string]
  {:tag "" :sep "" :bounds "" :content string })

(defn group
  "Delimits a graph pattern within curly braces. (Not a SPARQL keyword.)"
  [& more]
  {:tag "" :content (vec more) :bounds ["{ " " }"] :sep " "})

;; Namespaces-related

(defn base [q uri]
  (assoc q :base uri))

;; Query forms

(defn select [q & more]
  (assoc q :query-form {:tag "SELECT" :bounds [" "] :sep " "
                        :content (vec more)}))

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

(defn optional [& more]
  {:tag "OPTIONAL " :content (vec more) :bounds ["{ " " }"] :sep " "})

(defn union [& more]
  {:tag "" :content (interpose 'UNION (vec more)) :bounds [""] :sep " "})

(defn graph [g & more]
  {:tag (str "GRAPH " (encode g)) :content (vec more) :bounds [" { " " }"] :sep " "})

;; Negation

(def no-bounds
  "Don't use parentheses if FILTER is to be followed by one of these functions."
  #{"regex" "isIRI" "isLiteral" "isBlank" "langMatches"})

(defn filter [& more]
  (if (and (map? (first more)) (contains? no-bounds (:tag (first more))))
    {:tag "FILTER" :content (vec more) :bounds [" " ""] :sep " "}
    {:tag "FILTER" :content (vec more) :bounds ["(" ")"] :sep " "}))

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

;; Solution sequences and modifiers

(defn select-distinct [q & more]
  (assoc q :query-form {:tag "SELECT DISTINCT" :content (vec more)
                        :bounds [" "] :sep " "}))

(defn select-reduced [q & more]
  (assoc q :query-form {:tag "SELECT REDUCED" :content (vec more)
                        :bounds [" "] :sep " "}))

(defn limit [q n]
  (assoc q :limit {:tag "LIMIT" :bounds [" "] :sep " " :content [n]}))

(defn offset [q n]
  (assoc q :offset {:tag "OFFSET" :bounds [" "] :sep " " :content [n]}))

(defn desc [v] {:tag "DESC" :bounds ["(" ")"] :sep " " :content [v]})

(defn asc [v] {:tag "ASC" :bounds ["(" ")"] :sep " " :content [v]})

(defn order-by [q & expr]
  (assoc q :order-by
    {:tag "ORDER BY" :bounds [" "] :sep " " :content (vec expr)}))

(defn order-by-desc [q v] (order-by q (desc v)))

(defn order-by-asc [q v] (order-by q (asc v)))

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

(defn sum [v] {:tag "SUM" :bounds ["(" ")"] :sep " " :content [v]})

(defn count [v] {:tag "COUNT" :bounds ["(" ")"] :sep " " :content [v]})

(defn count-distinct [v] {:tag "COUNT" :bounds ["(DISTINCT " ")"] :sep " " :content [v]})

(defn group-concat [v sep]
  {:tag "sql:GROUP_CONCAT" :bounds ["(" ")"] :sep ", " :content [v sep]})

(defn sample [v]
  {:tag "sql:SAMPLE" :bounds ["(" ")"] :sep "" :content [v]})

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

(defn lang-matches [& more]
  {:tag "langMatches" :content (vec more) :bounds ["(" ")"] :sep ", "})

; TODO: STRLEN, SUBSTR, UCASE, LCASE, STRSTARS, STRENDS, CONTAINS, STRBEFORE,
;       STRAFTER, ENCODE_FOR_URI, REPLACE


;; Functions on RDF terms

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

(defn str2 [term] ; clojure.core/str is to usefull to do without
  {:tag "str" :content [term] :bounds ["(" ")"] :sep ""})

(defn lang [literal]
  {:tag "lang" :content [literal] :bounds ["(" ")"] :sep ""})

(defn datatype [literal]
  {:tag "datatype" :content [literal] :bounds ["(" ")"] :sep ""})

; TODO: datatype, IRI, BNODE, STRDT, STRLANG, UUID, STRUUID

; TODO: Functions on Numerics

; TODO: Functions on Dates and Times

; TODO: Hash functions

;; SPARQL UPDATE

(defn with [q uri]
  (assoc q :with {:tag "WITH" :bounds [" "] :sep " " :content [uri]}))

(defn insert-data [q & more]
  (assoc q :insert {:tag "INSERT DATA" :bounds [" { " " }"] :sep " "
                    :content (vec more)}))

(defn delete-data [q & more]
  (assoc q :insert {:tag "DELETE DATA" :bounds [" { " " }"] :sep " "
                    :content (vec more)}))

(defn insert [q & more]
  (assoc q :insert {:tag "INSERT" :bounds [" { " " } "] :sep " "
                    :content (vec more)}))

(defn delete [q & more]
  (assoc q :delete {:tag "DELETE" :bounds [" { " " } "] :sep " "
                    :content (vec more)}))

;; SPARQL Grahp managment
; TODO clear, load, create, move, copy, drop, add