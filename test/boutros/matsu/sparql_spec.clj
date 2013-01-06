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
                      :ns      "<http://example.org/ns#>"
                      :org     "<http://example.com/ns#>"})

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
                 (bind [(concat :G " " :S) :name])))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?P foaf:givenName ?G ; foaf:surname ?S BIND(CONCAT(?G, \" \", ?S) AS ?name) }"))
  (is (=
        (query
          (construct :x [:foaf "name"] :name)
          (where :x [:org "employeeName"] :name))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX org: <http://example.com/ns#> CONSTRUCT { ?x foaf:name ?name } WHERE { ?x org:employeeName ?name }")))

(deftest part-3
  (is (=
        (query
          (select :title)
          (where :x [:dc "title"] :title
                 (filter-regex :title "^SPARQL")))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { ?x dc:title ?title FILTER regex(?title, \"^SPARQL\") }"))

  (is (=
        (query
          (select :title)
          (where :x [:dc "title"] :title
                 (filter-regex :title "web" "i")))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { ?x dc:title ?title FILTER regex(?title, \"web\", \"i\") }"))

  (is (=
        (query
          (select :title :price)
          (where :x [:ns "price"] :price \.
                 (filter :price \< 30.5)
                 :x [:dc "title"] :title \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title ?price WHERE { ?x ns:price ?price . FILTER(?price < 30.5) ?x dc:title ?title . }")))

(deftest part-4
  (is (=
        (query
          (select :title)
          (where (URI. "http://example.org/book/book1") [:dc "title"] :title))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { <http://example.org/book/book1> dc:title ?title }"))

    (is (=
        (query
          (base (URI. "http://example.org/book/"))
          (select :title)
          (where [:book1] [:dc "title"] :title))

        "BASE <http://example.org/book/> PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { <book1> dc:title ?title }")))

(deftest part-5
  (is (=
        (query
          (select :name :mbox)
          (where :x [:foaf "name"] :name \.
                 :x [:foaf "mbox"] :mbox \.))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox . }"))

    ; (is (=
    ;     (query
    ;       )

    ;     "PREFIX foaf:    <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE  { { ?x foaf:name ?name . } { ?x foaf:mbox ?mbox . } }"))
    )

(deftest part-6
  (is (=
        (query
          (select :name :mbox)
          (where :x [:foaf "name"] :name \.
                 (optional :x [:foaf "mbox"] :mbox)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . OPTIONAL { ?x foaf:mbox ?mbox } }"))

    (is (=
          (query
            (select :title :price)
            (where :x [:dc "title"] :title \.
                   (optional :x [:ns "price"] :price \. (filter :price \< 30))))

          "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title ?price WHERE { ?x dc:title ?title . OPTIONAL { ?x ns:price ?price . FILTER(?price < 30) } }" ))

    (is (=
          (query
            (select :name :mbox :hpage)
            (where :x [:foaf "name"] :name \.
                   (optional :x [:foaf "mbox"] :mbox) \.
                   (optional :x [:foaf "homepage"] :hpage)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox ?hpage WHERE { ?x foaf:name ?name . OPTIONAL { ?x foaf:mbox ?mbox } . OPTIONAL { ?x foaf:homepage ?hpage } }"))
    )
