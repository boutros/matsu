(ns boutros.matsu.sparql-test
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [boutros.matsu.compiler :refer [encode]]
            [boutros.matsu.core :refer [register-namespaces]])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

;; Setup

(register-namespaces {:dbpedia "<http://dbpedia.org/resource/>"
                      :foaf    "<http://xmlns.com/foaf/0.1/>"
                      :sql     "sql"})

;; Main Macros

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
          "SELECT * FROM <http://dbpedia.org/resource> WHERE { ?s ?p ?o }"))))

(deftest local-prefixes
  (is (=
        (query-with-prefixes {:foaf "<mylocalfoaf>"}
          (select :person)
          (where :person [:foaf :name] "Petter"))
        "PREFIX foaf: <mylocalfoaf> SELECT ?person WHERE { ?person foaf:name \"Petter\" }")))

;; SPARQL Query DSL functions

(deftest query-forms
  (testing "select"
    (is (=
          (query
            (select :s)
            (where :s :p :o))
          "SELECT ?s WHERE { ?s ?p ?o }")))

  (testing "construct"
    (is (=
          (query
            (construct :s :p :o)
            (where :s :p2 :o))
          "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p2 ?o }")))

  (testing "ask"
    (is (=
          (query
            (ask :s :p :o \.))
          "ASK { ?s ?p ?o . }")))

  (testing "describe"
    (is (=
          (query
            (describe (URI. "http://hi.com/hello")))
          "DESCRIBE <http://hi.com/hello>"))
    (is (=
          (query
            (describe :x)
            (where :x :p :o))
          "DESCRIBE ?x WHERE { ?x ?p ?o }"))))

(deftest graph-patterns
  (testing "where"
    (is (=
          (query
            (select *)
            (where :s :p :o))
          "SELECT * WHERE { ?s ?p ?o }"))
    (is (=
          (query
            (select *)
            (where- :s :p :o))
          "SELECT * { ?s ?p ?o }")))

  (testing "optional"
    (is (=
          (query
            (select :o1 :o2)
            (where :s1 :p1 :o1 \.
                   (optional :s2 :p2 :o2) \.))
          "SELECT ?o1 ?o2 WHERE { ?s1 ?p1 ?o1 . OPTIONAL { ?s2 ?p2 ?o2 } . }")))

  (testing "union"
    (is (=
          (query
            (select :o)
            (where (union
                     (group :s :p :o)
                     (group :s :p2 :o))))
          "SELECT ?o WHERE { { ?s ?p ?o } UNION { ?s ?p2 ?o } }")))

  (testing "graph"
    (is (=
          (query
            (select *)
            (where (graph (URI. "someuri"))
                   (group :s :p :o) \.))
          "SELECT * WHERE { GRAPH <someuri> { ?s ?p ?o } . }"))))

(deftest negation
  (testing "filter"
    (is (=
          (query
            (ask :s1 :p1 :o1 \.
                 :s2 :p2 :o2 \.
                 (filter :o2 > :o1 ) \.))
          "ASK { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 . FILTER(?o2 > ?o1) . }")))

  (testing "filter inside optional"
    (is (=
          (query
            (select :s :price)
            (where :s :p :o \.
                   (optional :s :p2 :price \.
                             (filter :price < 30))))
          "SELECT ?s ?price WHERE { ?s ?p ?o . OPTIONAL { ?s ?p2 ?price . FILTER(?price < 30) } }")))

  (testing "filter-not-exists"
    (is (=
          (query
            (select :s)
            (where :s :p :o \.
                   (filter-not-exists :s :p2 :o2)))
          "SELECT ?s WHERE { ?s ?p ?o . FILTER NOT EXISTS { ?s ?p2 ?o2 } }")))

  (testing "filter-exists"
    (is (=
          (query
            (select :s)
            (where :s :p :o \.
                   (filter-exists :s :p2 :o2)))
          "SELECT ?s WHERE { ?s ?p ?o . FILTER EXISTS { ?s ?p2 ?o2 } }")))

  (testing "minus"
    (is (=
          (query
            (select :s)
            (where :s :p :o \.
                   (minus :s :p2 "cake")))
          "SELECT ?s WHERE { ?s ?p ?o . MINUS { ?s ?p2 \"cake\" } }"))))

(deftest datasets
  (testing "from"
    (is (=
          (query
            (select :s)
            (from (URI. "somegraph"))
            (where :s :p :o))
          "SELECT ?s FROM <somegraph> WHERE { ?s ?p ?o }")))

  (testing "from-named"
    (is (=
          (query
            (select *)
            (from-named (URI. "example/1")
                        (URI. "example/2"))
            (where :s :p :o))
          "SELECT * FROM NAMED <example/1> FROM NAMED <example/2> WHERE { ?s ?p ?o }"))))

(deftest modifiers
  (testing "select-distinct"
    (is (=
          (query
            (select-distinct :type)
            (where :s a :type))
          "SELECT DISTINCT ?type WHERE { ?s a ?type }")))

  (testing "select-reduced"
    (is (=
          (query
            (select-reduced :s)
            (where :s :p :o))
          "SELECT REDUCED ?s WHERE { ?s ?p ?o }")))

  (testing "limit and offset"
    (is (=
          (query
            (select :s :o)
            (where :s :p :o)
            (limit 5)
            (offset 3))
          "SELECT ?s ?o WHERE { ?s ?p ?o } LIMIT 5 OFFSET 3")))

  (testing "order-by"
    (is (=
          (query
            (select *)
            (where :s :p :o)
            (order-by (asc :o)))
          "SELECT * WHERE { ?s ?p ?o } ORDER BY ASC(?o)"))
    (is (=
          (query
            (select *)
            (where :s :p :o)
            (order-by (desc :o)))
          "SELECT * WHERE { ?s ?p ?o } ORDER BY DESC(?o)"))
    (is (=
          (query
            (select *)
            (where :s :p :o)
            (order-by-desc :o))
          "SELECT * WHERE { ?s ?p ?o } ORDER BY DESC(?o)"))
    (is (=
          (query
            (select *)
            (where :s :p :o)
            (order-by-asc :o))
          "SELECT * WHERE { ?s ?p ?o } ORDER BY ASC(?o)"))))

(deftest aggregation
  (testing "group-by"
    (is (=
          (query
            (select :s)
            (where :s :p :o)
            (group-by :s))
          "SELECT ?s WHERE { ?s ?p ?o } GROUP BY ?s")))

  (testing "having + sum"
    (is (=
          (query
            (select :s)
            (where :s :p :o)
            (having (sum :o) > 4))
          "SELECT ?s WHERE { ?s ?p ?o } HAVING(SUM(?o) > 4)")))

  (testing "avg"
    (is (=
          (query
            (select [(avg :o) :avg])
            (where :s :p :o))
          "SELECT (AVG(?o) AS ?avg) WHERE { ?s ?p ?o }")))

  (testing "group-concat"
    (is (=
          (query
            (select [(group-concat :s "|") :s])
            (where :s :p :o))
          "PREFIX sql: sql SELECT (sql:GROUP_CONCAT(?s, \"|\") AS ?s) WHERE { ?s ?p ?o }")))

  (testing "sample"
    (is (=
          (query
            (select [(sample :s) :s])
            (where :s :p :o))
          "PREFIX sql: sql SELECT (sql:SAMPLE(?s) AS ?s) WHERE { ?s ?p ?o }")))

  ;; todo min, max, count
  )

(deftest assignment
  (testing "bind"
    (is (= 1 1)))

  ;(testing "values")

  )

(deftest functional-forms
  (testing "bound"
    (is (= 1 1)))

  (testing "!bound"
    (is (= 1 1))))

(deftest string-functions
  (testing "concat"
    (is (= 1 1)))
  ;; +++
  )