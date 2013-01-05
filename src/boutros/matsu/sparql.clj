(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter concat])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures
; -----------------------------------------------------------------------------

(defn empty-query []
  "query-map constructor"
  {:from nil
   :query-form {:form nil :content []}
   :where []
   :order-by nil
   :limit nil
   :offset nil})

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
    (integer? x) x ;(str  \" x \" "^^xsd:integer")
    (float? x) x ;(str  \" x \" "^^xsd:decimal")
    (true? x) x ;"\"true\"^^xsd:boolean"
    (false? x) x ;"\"false\"^^xsd:boolean"
    (string? x) (str \" x \" )
    (= java.net.URI (type x)) (str "<" x ">")
    ;(= java.util.Date (type x)) (str \" x \" "^^xsd:dateTime")
    (vector? x) (if (string? (first x))
                  (str \" (first x) "\"@" (name (second x)))
                  (str (name (first x)) \: (second x)))
    (map? x) (:content x)
    :else (throw (new Exception "Don't know how to encode that into RDF literal!"))))

(defn encode-comma [x]
  "adds a commma after the encoded value"
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
      (cond
        (= form "ASK") (conj ["ASK"] (group-subcompile (get-in q [:query-form :content])))
        (= form "CONSTRUCT") (conj ["CONSTRUCT" "{"] (vec (map encode (get-in q [:query-form :content]))) "}")
        :else (conj [] (get-in q [:query-form :form])
                (vec (map encode (get-in q [:query-form :content]))))))))

(defn- from-compile [q]
  {:pre [(map? q)]}
  (when-not (nil? (:from q))
    (conj ["FROM"] (str \< (:from q) \>))))

(defn- where-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:where q))]
    (conj ["WHERE" "{"] (vec (map encode xs)) "}")))

(defn- limit-compile [q]
  {:pre [(map? q)]}
  (when-let [n (:limit q)]
    (conj [] "LIMIT" n)))

(defn- infer-prefixes   [s]
  {:pre [(string? s)]
   :post [(string? %)]}
  (str
    (first (for [p
                 (->> s (re-seq #"(\b\w+):\w") (map last))]
             (str "PREFIX " p ": " (ns-or-error (keyword p)) " " )))
    s))

(defn compile-query [q]
  {:pre [(map? q)]
   :post [(string? %)]}
  "Takes a map representing SPARQL graph patterns, bindings and modifiers and
  returns a vaild SPARQL 1.1 query string"
  (->> (conj []
             (query-form-compile q)
             (from-compile q)
             (where-compile q)
             (limit-compile q))
       (flatten)
       (remove nil?)
       (string/join " ")
       (infer-prefixes)))

; -----------------------------------------------------------------------------
; SPARQL query DSL functions
; -----------------------------------------------------------------------------

; These all takes a map of the query and returns a modified query-map:

(defn ask [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "ASK" :content (vec vars)}))

(defn from [q graph]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :from graph))

(defn select [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "SELECT" :content (vec vars)}))

(defn select-distinct [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :query-form {:form "SELECT DISTINCT" :content (vec vars)}))

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

; Special functions which compile inline groups inside select/where clauses:

(defn filter [& vars]
  {:post [(map? %)]}
   {:content (str "FILTER(" (string/join " " (vec (map encode vars))) ")" )})

(defn optional [& vars]
  {:post [(map? %)]}
   {:content (str "OPTIONAL { " (string/join " " (vec (map encode vars))) " }" )})

(defn raw [string]
  {:pre [(string? string)]
   :post [(map? %)]}
   {:content string })

(defn concat [& vars]
  {:post [(map? %)]}
  {:content (str "CONCAT("(string/join " " (map encode-comma (butlast vars)))
                 " " (encode (last vars))")") })

(defn bind [& vars]
  {:post [(map? %)]}
  {:content (str "BIND("(string/join " " (map encode vars)) ")") })
