(ns boutros.matsu.sparql-test
  (:use midje.sweet
        boutros.matsu.sparql)
  (:import (java.net URI)))

(facts
  "fn encode: correctily encodes RDF variables and literals"
  (encode \*) => \*
  (encode :keyword) => "?keyword"
  (encode 23) => "\"23\"^^xsd:integer"
  (encode 9.9) => "\"9.9\"^^xsd:decimal"
  (encode "string") => "\"string\""
  (encode true) => "\"true\"^^xsd:boolean"
  (encode false) => "\"false\"^^xsd:boolean"
  ;(encode (java.util.Date.)) => "not implemented!"
  )

; Testing the macros

; (fact
;   "query macro can be used with or without a saved query-map"
;   (defquery my-query (select :s)) => truthy
;   (query my-query
;          (where : :p :o \.)) =>
;   (query (select :s)
;          (where :s :p :o \.))))

(fact
  "fn query: ASK query form is supported"
  (query
    (ask)
    (where :s :p :o \.)) => "ASK WHERE { ?s ?p ?o . }"
  )

(fact
  "example query from Sparql spec"
  (query
    (select :title)
    (where (URI. "http://example.org/book/book1") (URI. "http://purl.org/dc/elements/1.1/title") :title \.))
  => "SELECT ?title WHERE { <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title . }"
  )

(fact
  "example2 query from sparql spec"
  (query
    (prefix :foaf)
    (select :name :mbox)
    (where :x [:foaf "name"] :name \.
           :x [:foaf "mbox"] :mbox))
  => "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { ?x foaf:name ?name . ?x foaf:mbox ?mbox }"
  )

(fact
  (query
    (prefix :foaf)
    (ask)
    (where :person \a [:foaf "Person"]
           \; [:foaf "mbox"] (URI. "mailto:petter@petter.com") \.))
  =>
  "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK WHERE { ?person a foaf:Person ; foaf:mbox <mailto:petter@petter.com> . }"
  )
