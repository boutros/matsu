(ns boutros.matsu.sparql-test
  (:refer-clojure :exclude [filter])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

; Macros

(defquery q1
  (select :s))

(defquery q2
  (from "http://dbpedia.org/resource"))

(deftest saved-queries
  (testing "query based on saved queries"
    (is (=
          (query q1
            (where :s :p :o \.))

          "SELECT ?s WHERE { ?s ?p ?o . }"))

    (is (=
          (query q2
            (select \*)
            (where :s :p :o))

          "SELECT * FROM <http://dbpedia.org/resource> WHERE { ?s ?p ?o }")))
  )

; Utils

(deftest utils
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
         [:foaf "mbox"]              "foaf:mbox")))

; Query DSL

(deftest query-functions
  (testing "ask"
    (is (=
          (query
            (ask :s :p :o \.))

          "ASK { ?s ?p ?o . }")))

  (testing "select"
    (is (=
          (query
            (select :s)
            (where :s :p :o))

          "SELECT ?s WHERE { ?s ?p ?o }")))

  (testing "select-distinct"
    (is (=
          (query
            (select-distinct :type)
            (where :s \a :type))

          "SELECT DISTINCT ?type WHERE { ?s a ?type }")))

  (testing "prefixed names"
    (is (=
          (query
            (prefix :foaf)
            (select :name :mbox)
            (where :x [:foaf "name"] :name \.
                   :x [:foaf "mbox"] :mbox))

          "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox }"))

    (is (=
          (query
            (prefix :foaf)
            (ask :person \a [:foaf "Person"]
                  \; [:foaf "mbox"] (URI. "mailto:petter@petter.com") \.))

          "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK { ?person a foaf:Person ; foaf:mbox <mailto:petter@petter.com> . }")))

    (testing "limit"
      (is (=
            (query
              (prefix :rdfs)
              (select :subject :label)
              (where :subject [:rdfs "label"] :label)
              (limit 5))

            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?subject ?label WHERE { ?subject rdfs:label ?label } LIMIT 5")))

  (testing "filter"
    (is (=
          (query
            (prefix :dbpedia :prop)
            (ask [:dbpedia "Amazon_River"] [:prop "length"] :amazon \.
                 [:dbpedia "Nile"] [:prop "length"] :nile \.
                 (filter :amazon \> :nile ) \.))

          "PREFIX dbpedia: <http://dbpedia.org/resource/> PREFIX prop: <http://dbpedia.org/property/> ASK { dbpedia:Amazon_River prop:length ?amazon . dbpedia:Nile prop:length ?nile . FILTER(?amazon > ?nile) . }"
          )))


    ; (testing "group"
    ;   (is (=
    ;         (query
    ;           (ask)
    ;           (group :s :p :o \.))

    ;         "ASK { ?s ?p ?o . }")))

  ; (testing "filter"
  ;   (is (=
  ;         (query
  ;           (prefix :prop)
  ;           (select :subject :label)
  ;           (where :subject [:rdfs "label"] :label)
  ;           (limit 5))

  ;         "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?subject ?label WHERE { ?subject rdfs:label ?label } LIMIT 5"))))
  )
