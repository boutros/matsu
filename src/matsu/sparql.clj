(ns matsu.sparql
  (:refer-clojure :exclude [filter])
  (:require [clojure.set :as set]
            [clojure.string :as string])
  (:import (java.net URI)))

; -----------------------------------------------------------------------------
; Vars & Types
; -----------------------------------------------------------------------------

; *PREFIXES* is a global map of all namespaces you intend to use
; TODO make an atom?
(def ^:dynamic *PREFIXES* {:dbpedia "<http://dbpedia.org/resource/>" :foaf "<http://xmlns.com/foaf/0.1/>"})

(defn empty-query []
  "starting query"
  {:prefixes []
   :from nil
   :ask nil
   :select []
   :where []
   :order-by nil
   :limit nil
   :offset nil}
  )

; thinking about the map^
; :query-form = one of ASK | SELECT | DESCRIBE | CONSTRUCT
; :query-form {:type "SELECT" :contents []}
; defrecord?

; -----------------------------------------------------------------------------
; Macros
; -----------------------------------------------------------------------------

(defmacro query [& body]
          `(-> ~@body (compile-query)))

;; todo add empty-query if not first param is query map (record?)

; -----------------------------------------------------------------------------
; RDF util functions
; -----------------------------------------------------------------------------

(defn encode [x]
  "Encodes keywords to ?-prefixed variables and other values to RDF literals.
  Vectors are treated as namespace-qualified and resolves with the first value
  as namespace."
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
    :else (throw (new Exception "Invalid SPARQL variable/literal"))))

; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------
; Transforms the various query parts into vectors of strings

(defn from-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (nil? (:from q))
    []
    (conj ["FROM"] (str \< (:from q) \>))))

(defn prefix-compile [q]
  (if (empty? (:prefixes q))
    []
    (for [p (:prefixes q)]
      ["PREFIX" (str (name p) \:) (*PREFIXES* p)])))

(defn ask-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (:ask q) ["ASK"] []))

(defn select-compile [q]
  {:pre [(map? q)]
   :post [(vector? %)]}
  (if (empty? (:select q))
    []
    (conj ["SELECT"] (vec (map encode (:select q))))))

(defn where-compile [q]
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
             (ask-compile q)
             (from-compile q)
             (select-compile q)
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
  (assoc q :ask true))

(defn from [q graph]
  {:pre [(map? q)]
   :post [(map? %)]}
  (assoc q :from graph))

(defn select [q & vars]
  {:pre [(map? q),
         (nil? (:ask q)),
         (empty? (->> vars
                      (remove keyword?)
                      (remove char?)))]
   :post [(map? %)]}
  (assoc q :select (vec vars)))

(defn where [q & vars]
  {:pre [(map? q) ]
   :post [(map? %)]}
  (update-in q [:where] into vars))

(defn with-prefixes [q & prefixes]
  (update-in q [:prefixes] into prefixes))

(defn optional [& args]
  (into ["OPTIONAL"] args))

; -----------------------------------------------------------------------------
; Fiddling
; -----------------------------------------------------------------------------

(comment


  (query (empty-query)
         (with-prefixes :dbpedia :foaf)
         (from "dbpedia.org/resource")
         (select \*)
         (where [:dbpedia "blab"] :p :o \;
                (optional :firstName "skap" \;)
                          (filter :lastName "somethin"))
                ))
