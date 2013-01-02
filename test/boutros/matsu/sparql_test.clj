(ns boutros.matsu.sparql-test
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

; Utils

(testing "encode"
  (are [a b] (= (encode a) b)
       \*                          \*
       :keyword                    "?keyword"
       23                          "\"23\"^^xsd:integer"
       9.9                         "\"9.9\"^^xsd:decimal"
       "string"                    "\"string\""
       true                        "\"true\"^^xsd:boolean"
       false                       "\"false\"^^xsd:boolean"
       (URI. "http://dbpedia.org") "<http://dbpedia.org>"
       [:foaf "mbox"]              "foaf:mbox"))

; Macros



; Query DSL

(testing "ask"
  (is
    (query
      (ask)
      (where :s :p :o \.))

    "ASK WHERE { ?s ?p ?o . }"))

(testing "select"
  (is
    (query
      (select :s)
      (where :s :p :o))

    "SELECT ?s WHERE { :s :p :o }"))

(testing "prefix and namespaced qualifiers"
  (is
    (query
    (prefix :foaf)
    (select :name :mbox)
    (where :x [:foaf "name"] :name \.
           :x [:foaf "mbox"] :mbox))

    "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox }")

  (is
    (query
    (prefix :foaf)
    (ask)
    (where :person \a [:foaf "Person"]
           \; [:foaf "mbox"] (URI. "mailto:petter@petter.com") \.))

    "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK WHERE { ?person a foaf:Person ; foaf:mbox <mailto:petter@petter.com> . }"))
