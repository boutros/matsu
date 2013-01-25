(ns boutros.matsu.compiler
  "Compiles the query-map into a valdid SPARQL 1.1 string"
  (:require [clojure.string :as string]
            [clojure.walk :refer [postwalk-replace]]
            [boutros.matsu.core :refer [prefixes ns-or-error]]))


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
    [[:keyword]]     => _:keyword (blank node)
    [[]]             => []

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
                    (vector? a) (if (seq a) (str '_ (first a)) "[]")
                    (not b) (str \< (name a) \>)
                    (string? a) (str \" a "\"@" (name b))
                    (and (map? a)
                         (keyword? b)) (into ["("]
                                             (conj (sub-compiler a) " AS " (encode b) ")" ))
                    (every? keyword? x) (str (name a) \: (name b))
                    :else "cannot encode!"))
    (map? x) (sub-compiler x)
    :else (throw (Exception.
                   (format "Don't know how to encode %s in an SPARQL context" x)))))


; -----------------------------------------------------------------------------
; Compiler functions
; -----------------------------------------------------------------------------

(def replacement-map
  "Used to replace clojure fn symbols with characters inside expressions"
  {= \= > \> < \< * \* + \+ - \- >= '>= <= '<=})

(defn- compiler [q part]
  (when-let [m (part q)]
    (sub-compiler m)))

(defn- sub-compiler [m]
  (conj []
        (:tag m)
        (first (:bounds m))
        (interpose
          (:sep m)
          (let [content (:content m)]
            (map encode (postwalk-replace replacement-map content))))
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
               (for [part [:query-form :from :from-named :delete :insert :where :order-by
                           :limit :offset :group-by :having]]
                (compiler q part)))
         (flatten)
         (string/join)
         (infer-prefixes local-prefixes)
         (add-base base)
         (string/trim))))
