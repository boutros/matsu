(ns boutros.matsu.w3c-update-test
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [boutros.matsu.core :refer [register-namespaces]])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

(register-namespaces {:dc      "<http://purl.org/dc/elements/1.1/>"
                      :ns      "<http://example.org/ns#>"
                      :xsd     "<http://www.w3.org/2001/XMLSchema#>"
                      :foaf    "<http://xmlns.com/foaf/0.1/>"})

(deftest example-1
  (is (=
        (query
          (insert-data (URI. "http://example/book1") [:dc :title] "A new book"
                       \; [:dc :creator] "A.N.Other" \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { <http://example/book1> dc:title \"A new book\" ; dc:creator \"A.N.Other\" . }")))

(deftest example-2
  (is (=
        (query
          (insert-data (graph (URI. "http://example/bookStore")
                               (URI. "http://example/book1") [:ns :price] 42)))

        "PREFIX ns: <http://example.org/ns#> INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1> ns:price 42 } }")))

(deftest example-3
  (is (=
        (query
          (delete-data (URI. "http://example/book2") [:dc :title] "David Copperfield"
                       \; [:dc :creator] "Edmund Wells" \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE DATA { <http://example/book2> dc:title \"David Copperfield\" ; dc:creator \"Edmund Wells\" . }")))

(deftest example-4)

(deftest example-5
  (is (=
        (query
          (with (URI. "http://example/addresses"))
          (delete :person [:foaf :givenName] "Bill")
          (insert :person [:foaf :givenName] "William")
          (where :person [:foaf :givenName] "Bill"))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> WITH <http://example/addresses> DELETE { ?person foaf:givenName \"Bill\" } INSERT { ?person foaf:givenName \"William\" } WHERE { ?person foaf:givenName \"Bill\" }")))

(deftest example-6
  (is (=
        (query
          (delete :book :p :v)
          (where :book [:dc :date] :date \.
                 (filter :date > ["1970-01-01T00:00:00-02:00" "xsd:dateTime"])
                 :book :p :v))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> DELETE { ?book ?p ?v } WHERE { ?book dc:date ?date . FILTER(?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime) ?book ?p ?v }")))

(deftest example-7
  (is (=
        (query
          (with (URI. "http://example/addresses"))
          (delete :person :property :value)
          (where :person :property :value
                 \; [:foaf :givenName] "Fred"))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> WITH <http://example/addresses> DELETE { ?person ?property ?value } WHERE { ?person ?property ?value ; foaf:givenName \"Fred\" }")))

(deftest example-8
  (is (=
        (query
          (insert
            (graph (URI. "http://example/bookStore2")
                  :book :p :v))
          (where
            (graph (URI. "http://example/bookStore")
                   :book [:dc :date] :date \.
                   (filter :date > ["1970-01-01T00:00:00-02:00" "xsd:dateTime"])
                   :book :p :v)))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> INSERT { GRAPH <http://example/bookStore2> { ?book ?p ?v } } WHERE { GRAPH <http://example/bookStore> { ?book dc:date ?date . FILTER(?date > \"1970-01-01T00:00:00-02:00\"^^xsd:dateTime) ?book ?p ?v } }")))

(deftest example-9
  (is (=
        (query
          (insert
            (graph (URI. "http://example/addresses")
                   :person [:foaf :name] :name \.
                   :person [:foaf :mbox] :email))
          (where
            (graph (URI. "http://example/people")
                   :person [:foaf :name] :name \.
                   (optional :person [:foaf :mbox] :email))))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> INSERT { GRAPH <http://example/addresses> { ?person foaf:name ?name . ?person foaf:mbox ?email } } WHERE { GRAPH <http://example/people> { ?person foaf:name ?name . OPTIONAL { ?person foaf:mbox ?email } } }")))

;(deftest example-10)

(deftest example-11
  (is (=
        (query
          (delete)
          (where :person [:foaf :givenName] "Fred"
                 \; :property :value))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DELETE WHERE { ?person foaf:givenName \"Fred\" ; ?property ?value }")))

(deftest example-12
  (is (=
        (query
          (delete)
          (where
            (graph (URI. "http://example.com/names")
                   :person [:foaf :givenName] "Fred"
                   \; :property1 :value1)
            (graph (URI. "http://example.com/addresses")
                   :person :property2 :value2)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DELETE WHERE { GRAPH <http://example.com/names> { ?person foaf:givenName \"Fred\" ; ?property1 ?value1 } GRAPH <http://example.com/addresses> { ?person ?property2 ?value2 } }")))