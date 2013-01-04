(ns boutros.matsu.sparql-spec
  (:refer-clojure :exclude [filter])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

(deftest part-2
  "2 Making Simple Queries (Informative)"
  (is (=
        (query
          (select :title)
          (where (URI. "http://example.org/book/book1")
                 (URI. "http://purl.org/dc/elements/1.1/title")
                 :title \.))

        "SELECT ?title WHERE { <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . }"))

  (is (=
        (query
          (prefix :foaf)
          (select :name :mbox)
          (where :x [:foaf "name"] :name \.
                 :x [:foaf "mbox"] :mbox))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox }"))

  (is (=
        (query
          (select :v)
          (where :v :p "cat"))

        "SELECT ?v WHERE { ?v ?p \"cat\" }"))

  (is (=
        (query
          (select :v)
          (where :v :p ["cat" :en]))

        "SELECT ?v WHERE { ?v ?p \"cat\"@en }"))
  )

