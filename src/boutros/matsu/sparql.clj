(ns boutros.matsu.sparql
  (:refer-clojure :exclude [filter])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Datastructures and vars
; -----------------------------------------------------------------------------

; *PREFIXES* is a global map of all namespaces you intend to use
; TODO make an atom?

(def ^:dynamic *PREFIXES* {:dbpedia "<http://dbpedia.org/resource/>"
                           :foaf "<http://xmlns.com/foaf/0.1/>"
                           :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
                           :prop "<http://dbpedia.org/property/>"})

(defn empty-query []
  "query-map constructor"
  {:prefixes []
   :from nil
   :query-form {:form nil :content []}
   :where []
   :order-by nil
   :limit nil
   :offset nil}
  )


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
  when applicable. Vectors are treated as namespace-qualified and resolves
  with the first value as namespace.

  Maps are used for special cases, to avoid compilation inside where-clauses."
  (cond
    (char? x) x
    (keyword? x) (str \? (name x))
    (integer? x) (str  \" x \" "^^xsd:integer")
    (float? x) (str  \" x \" "^^xsd:decimal")
    (true? x) "\"true\"^^xsd:boolean"
    (false? x) "\"false\"^^xsd:boolean"
    (string? x) (str \" x \" )
    (= java.net.URI (type x)) (str "<" x ">")
    ;(= java.util.Date (type x)) (str \" x \" "^^xsd:dateTime")
    (vector? x) (str (name (first x)) \: (second x))
    (map? x) (:content x)
    :else (throw (new Exception "Don't know how to encode that into RDF literal!"))))

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
      (if
        (= form "ASK") (conj ["ASK"] (group-subcompile (get-in q [:query-form :content])))
        (conj [] (get-in q [:query-form :form])
              (vec (map encode (get-in q [:query-form :content]))))))))

(defn- from-compile [q]
  {:pre [(map? q)]}
  (when-not (nil? (:from q))
    (conj ["FROM"] (str \< (:from q) \>))))

(defn- prefix-compile [q]
  (when-not (empty? (:prefixes q))
    (for [p (:prefixes q)]
      ["PREFIX" (str (name p) \:) (*PREFIXES* p)])))

(defn- where-compile [q]
  {:pre [(map? q)]}
  (when-let [xs (seq (:where q))]
    (conj ["WHERE" "{"] (vec (map encode xs)) "}")))

(defn- limit-compile [q]
  {:pre [(map? q)]}
  (when-let [n (:limit q)]
    (conj [] "LIMIT" n)))

(defn compile-query [q]
  {:pre [(map? q)]
   :post [(string? %)]}
  "Takes a map representing SPARQL graph patterns, bindings and modifiers and
  returns a vaild SPARQL 1.1 query string"
  (->> (conj []
             (prefix-compile q)
             (query-form-compile q)
             (from-compile q)
             (where-compile q)
             (limit-compile q))
       (flatten)
       (remove nil?)
       (string/join " ")))

; -----------------------------------------------------------------------------
; SPARQL query DSL functions
; -----------------------------------------------------------------------------

; These all takes a map of the query and returns a modified query-map:

(defn ask [q & vars]
  {:pre [(map? q)]
   :post [(map? q)]}
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

(defn where [q & vars]
  {:pre [(map? q)]
   :post [(map? %)]}
  (update-in q [:where] into vars))

(defn prefix [q & prefixes]
  (update-in q [:prefixes] into prefixes))

(defn limit [q & n]
  {:pre [(map? q)]
  :post [(map? %)]}
  (assoc q :limit n))

; Special functions which copmile inline groups inside where clauses:

(defn filter [& vars]
  {:post [(map? %)]}
   {:content (str "FILTER(" (string/join " " (vec (map encode vars))) ")" )})

(defn optional [& vars]
  {:post [(map? %)]}
   {:content (str "OPTIONAL { " (string/join " " (vec (map encode vars))) " }" )})

; (defn group [q & vars]
;   {:pre [(map? q)]
;    :post [(map? q)]}
;    (update-in q [:group] into vars))
