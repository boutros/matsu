(ns boutros.matsu.sparql-test
  (:refer-clojure :exclude [filter concat group-by max min])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

; Namespaces

(register-namespaces {:dbpedia "<http://dbpedia.org/resource/>"
                      :foaf    "<http://xmlns.com/foaf/0.1/>"
                      :rdfs    "<http://www.w3.org/2000/01/rdf-schema#>"
                      :prop    "<http://dbpedia.org/property/>"
                      :dc      "<http://purl.org/dc/elements/1.1/>"
                      :ns      "<http://example.org/ns#>"
                      :org     "<http://example.com/ns#>"})

(deftest missing-namespace
  (is (thrown? IllegalArgumentException
               (query
                 (select \*)
                 (where [:missing "dada"] :p :o)))))

(deftest local-prefixes
  (is (=
        (query
          (with-prefixes {:foaf "<mylocalfoaf>"})
          (select :person)
          (where :person [:foaf "name"] "Petter"))

        "PREFIX foaf: <mylocalfoaf> SELECT ?person WHERE { ?person foaf:name \"Petter\" }")))

; Macros

(defquery q1
  (select :s))

(defquery q2
  (from (URI. "http://dbpedia.org/resource")))

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
         23                          23 ;"\"23\"^^xsd:integer"
         9.9                         9.9 ;"\"9.9\"^^xsd:decimal"
         "string"                    "\"string\""
         true                        true ;"\"true\"^^xsd:boolean"
         false                       false ;"\"false\"^^xsd:boolean"
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
            (select :name :mbox)
            (where :x [:foaf "name"] :name \.
                   :x [:foaf "mbox"] :mbox))

          "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox }"))

    (is (=
          (query
            (ask :person \a [:foaf "Person"]
                  \; [:foaf "mbox"] (URI. "mailto:petter@petter.com") \.))

          "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK { ?person a foaf:Person ; foaf:mbox <mailto:petter@petter.com> . }")))

  (testing "limit"
    (is (=
          (query
            (select :s :o)
            (where :s :p :o)
            (limit 5))

          "SELECT ?s ?o WHERE { ?s ?p ?o } LIMIT 5")))

  (testing "filter"
    (is (=
          (query
            (ask :s1 :p1 :o1 \.
                 :s2 :p2 :o2 \.
                 (filter :o2 \> :o1 ) \.))

          "ASK { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . FILTER(?o2 > ?o1) . }")))

  (testing "optional"
    (is (=
          (query
            (select :o1 :o2)
            (where :s1 :p1 :o1 \.
                   (optional :s2 :p2 :o2) \.))

          "SELECT ?o1 ?o2 WHERE { ?s1 ?p1 ?o1 . OPTIONAL { ?s2 ?p2 ?o2 } . }")))

  (testing "filter inside optional"
    (is (=
          (query
            (select :s :price)
            (where :s :p :o \.
                   (optional :s :p2 :price \.
                             (filter :price \< 30))))

          "SELECT ?s ?price WHERE { ?s ?p ?o . OPTIONAL { ?s ?p2 ?price . FILTER(?price < 30) } }")))

  (testing "language tags"
    (is (=
          (query
            (select :s)
            (where :s :p ["une pipe" :fr]))

          "SELECT ?s WHERE { ?s ?p \"une pipe\"@fr }")))

  (testing "raw"
    (is (=
          (query
            (select :title)
            (where :s :p :title \.
                   (raw "FILTER langMatches( lang(?title), \"FR\" )")))

          "SELECT ?title WHERE { ?s ?p ?title . FILTER langMatches( lang(?title), \"FR\" ) }")))

  (testing "contstruct"
    (is (=
          (query
            (construct :s :p :o)
            (where :s :p2 :o))

          "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p2 ?o }")))

  )

(deftest sparql-functions
  (testing "concat"
    (is (= 1) 1)))