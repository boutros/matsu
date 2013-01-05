(ns boutros.matsu.sparql-spec
  (:refer-clojure :exclude [filter concat])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

; Setup

(register-namespaces {:dbpedia "<http://dbpedia.org/resource/>"
                      :foaf    "<http://xmlns.com/foaf/0.1/>"
                      :rdfs    "<http://www.w3.org/2000/01/rdf-schema#>"
                      :prop    "<http://dbpedia.org/property/>"
                      :dc      "<http://purl.org/dc/elements/1.1/>"
                      :ns      "<http://example.org/ns#>"})

; Tests

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

  (is (=
        (query
          (select :v)
          (where :v :p 42))

        "SELECT ?v WHERE { ?v ?p 42 }"))

  (is (=
        (query
          (select :v)
          (where :v :p (raw "\"abc\"^^<http://example.org/datatype#specialDatatype>")))

        "SELECT ?v WHERE { ?v ?p \"abc\"^^<http://example.org/datatype#specialDatatype> }"))

  (is (=
        (query
          (select :x :name)
          (where :x [:foaf "name"] :name))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?x ?name WHERE { ?x foaf:name ?name }"))

  (is (=
        (query
          (select \( (concat :G " " :S) 'AS :name \) )
          (where :P [:foaf "givenName"] :G
                 \; [:foaf "surname"] :S))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ( CONCAT(?G, \" \", ?S) AS ?name ) WHERE { ?P foaf:givenName ?G ; foaf:surname ?S }"))

  (is (=
        (query
          (select :name)
          (where :P [:foaf "givenName"] :G
                 \; [:foaf "surname"] :S
                 (bind (concat :G " " :S) 'AS :name)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?P foaf:givenName ?G ; foaf:surname ?S BIND(CONCAT(?G, \" \", ?S) AS ?name) }"))
  )

