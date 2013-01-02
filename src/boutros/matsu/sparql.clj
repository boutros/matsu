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
; rename to *NAMESPACES*
(def ^:dynamic *PREFIXES* {:dbpedia "<http://dbpedia.org/resource/>" :foaf "<http://xmlns.com/foaf/0.1/>"})

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
  with the first value as namespace."
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
    :else (throw (new Exception "Don't know how to encode that into RDF literal!"))))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into vectors of strings

(defn- query-form-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (empty? (get-in q [:query-form :form]))
    []
    (conj [] (get-in q [:query-form :form]) (vec (map encode (get-in q [:query-form :content]))))))

(defn- from-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (nil? (:from q))
    []
    (conj ["FROM"] (str \< (:from q) \>))))

(defn- prefix-compile [q]
  (if (empty? (:prefixes q))
    []
    (for [p (:prefixes q)]
      ["PREFIX" (str (name p) \:) (*PREFIXES* p)])))

(defn- ask-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (:ask q) ["ASK"] []))

(defn- select-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (empty? (:select q))
    []
    (conj ["SELECT"] (vec (map encode (:select q))))))

(defn- where-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (empty? (:where q))
    []
    (conj ["WHERE" "{"] (vec (map encode (:where q))) "}")))

(defn compile-query [q]
  {:pre [(map? q)]
   :post [(string? %)]}
  "Takes a map representing SPARQL graph patterns, bindings and modifiers and
  returns a vaild SPARQL 1.1 query string"
  (->> (conj []
             (prefix-compile q)
             (query-form-compile q)
             (from-compile q)
             (where-compile q))
       (flatten)
       (string/join " ")))

; -----------------------------------------------------------------------------
; SPARQL query DSL functions
; -----------------------------------------------------------------------------
; These all takes a map of the query and returns a modified query-map

(defn ask [q]
  {:pre [(map? q)
         (empty? (:select q))]
   :post [(map? q)]}
  (assoc q :query-form {:form "ASK" :content []}))

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
  {:pre [(map? q) ]
   :post [(map? %)]}
  (update-in q [:where] into vars))

(defn prefix [q & prefixes]
  (update-in q [:prefixes] into prefixes))

; (defn optional [& args]
;   (into ["OPTIONAL"] args))

