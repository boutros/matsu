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
          (insert-data (URI. "http://example/book1") [:dc "title"] "A new book"
                       \; [:dc "creator"] "A.N.Other" \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> INSERT DATA { <http://example/book1> dc:title \"A new book\" ; dc:creator \"A.N.Other\" . }")))

(deftest example-2
  (is (=
        (query
          (insert-data (graph (URI. "http://example/bookStore")
                              (group (URI. "http://example/book1") [:ns "price"] 42))))

        "PREFIX ns: <http://example.org/ns#> INSERT DATA { GRAPH <http://example/bookStore> { <http://example/book1> ns:price 42 } }")))

(deftest example-3
  (is (=
        (query
          (delete-data (URI. "http://example/book2") [:dc "title"] "David Copperfield"
                       \; [:dc "creator"] "Edmund Wells" \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> DELETE DATA { <http://example/book2> dc:title \"David Copperfield\" ; dc:creator \"Edmund Wells\" . }")))

(deftest example-4)