(ns boutros.matsu.util-test
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [boutros.matsu.compiler :refer [encode]]
            [boutros.matsu.core :refer [register-namespaces]]
            [boutros.matsu.util :refer [pprint]])
  (:use clojure.test
        boutros.matsu.sparql)
  (:import (java.net URI)))

(register-namespaces {:dc      "<http://purl.org/dc/elements/1.1/>"
                      :ns      "<http://example.org/ns#>"
                      :xsd     "<http://www.w3.org/2001/XMLSchema#>"
                      :foaf    "<http://xmlns.com/foaf/0.1/>"})

(deftest pretty-printer
  (is
    (= (pprint
         (query
           (select :s)
           (where :s :p :o \.)))
"SELECT ?s

WHERE
{
   ?s ?p ?o .
}"))

  (is
    (= (pprint
         (query
           (select :name :mbox :date :known)
           (where
             :g [:dc :publisher] :name \;
                [:dc :date] :date \.
             (graph :g
                    :person [:foaf :name] :name \;
                      [:foaf :mbox] :mbox \;
                      [:foaf :knows] :known))))
"PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

SELECT ?name ?mbox ?date ?known

WHERE
{
   ?g dc:publisher ?name ;
      dc:date ?date .
   GRAPH ?g
   {
      ?person foaf:name ?name ;
              foaf:mbox ?mbox ;
              foaf:knows ?known
   }
}"))

  (is
    (= (pprint
         (query
           (insert
             (graph (URI. "http://example/addresses")
                    :person [:foaf :name] :name \.
                    :person [:foaf :mbox] :email))
           (where
             (graph (URI. "http://example/people")
                    :person [:foaf :name] :name \.
                    (optional :person [:foaf :mbox] :email)))))
"PREFIX foaf: <http://xmlns.com/foaf/0.1/>

INSERT
{
   GRAPH <http://example/addresses>
   {
      ?person foaf:name ?name .
      ?person foaf:mbox ?email
   }
}

WHERE
{
   GRAPH <http://example/people>
   {
      ?person foaf:name ?name .
      OPTIONAL
      {
         ?person foaf:mbox ?email
      }
   }
}")))