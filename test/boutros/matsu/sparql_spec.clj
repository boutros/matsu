(ns boutros.matsu.sparql-spec
  (:refer-clojure :exclude [filter concat group-by])
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
                      :org     "<http://example.com/ns#>"
                      :dc10    "<http://purl.org/dc/elements/1.0/>"
                      :dc11    "<http://purl.org/dc/elements/1.1/>"
                      :rdf     "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                      :data    "<http://example.org/foaf/>"})

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
          (select [(concat :G " " :S) :name])
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

    (is (=
        (query
          (select :name :mbox)
          (where (group :x [:foaf "name"] :name \.)
                 (group :x [:foaf "mbox"] :mbox \.)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { { ?x foaf:name ?name . } { ?x foaf:mbox ?mbox . } }"))
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

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox ?hpage WHERE { ?x foaf:name ?name . OPTIONAL { ?x foaf:mbox ?mbox } . OPTIONAL { ?x foaf:homepage ?hpage } }")))

(deftest part-7
  (is (=
        (query
          (select :title)
          (where (union (group :book [:dc10 "title"] :title)
                        (group :book [:dc11 "title"] :title))))

        "PREFIX dc10: <http://purl.org/dc/elements/1.0/> PREFIX dc11: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { { ?book dc10:title ?title } UNION { ?book dc11:title ?title } }"))

  (is (=
        (query
          (select :x :y)
          (where (union (group :book [:dc10 "title"] :x)
                        (group :book [:dc11 "title"] :y))))

        "PREFIX dc10: <http://purl.org/dc/elements/1.0/> PREFIX dc11: <http://purl.org/dc/elements/1.1/> SELECT ?x ?y WHERE { { ?book dc10:title ?x } UNION { ?book dc11:title ?y } }"))
  (is (=
        (query
          (select :title :author)
          (where (union (group :book [:dc10 "title"] :title \. :book [:dc10 "creator"] :author)
                        (group :book [:dc11 "title"] :title \. :book [:dc11 "creator"] :author))))

        "PREFIX dc10: <http://purl.org/dc/elements/1.0/> PREFIX dc11: <http://purl.org/dc/elements/1.1/> SELECT ?title ?author WHERE { { ?book dc10:title ?title . ?book dc10:creator ?author } UNION { ?book dc11:title ?title . ?book dc11:creator ?author } }")))

(deftest part-8
  (is (=
        (query
          (select :person)
          (where :person [:rdf "type"] [:foaf "Person"] \.
                 (filter-not-exists :person [:foaf "name"] :name)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT ?person WHERE { ?person rdf:type foaf:Person . FILTER NOT EXISTS { ?person foaf:name ?name } }"))
  (is (=
        (query
          (select :person)
          (where :person [:rdf "type"] [:foaf "Person"] \.
                 (filter-exists :person [:foaf "name"] :name)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT ?person WHERE { ?person rdf:type foaf:Person . FILTER EXISTS { ?person foaf:name ?name } }"))

  (is (=
        (query
          (select-distinct :s)
          (where :s :p :o \.
                 (minus :s [:foaf "givenName"] "Bob" \.)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT DISTINCT ?s WHERE { ?s ?p ?o . MINUS { ?s foaf:givenName \"Bob\" . } }"))

  (is (=
        (query
          (select \*)
          (where :s :p :o
                 (filter-not-exists :x :y :z)))

        "SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { ?x ?y ?z } }"))

  (is (=
        (query
          (select \*)
          (where :s :p :o
                 (minus :x :y :z)))

        "SELECT * WHERE { ?s ?p ?o MINUS { ?x ?y ?z } }"))

  (is (=
        (query
          (base (URI. "http://example/"))
          (select \*)
          (where :s :p :o
                 (filter-not-exists [:a] [:b] [:c])))

        "BASE <http://example/> SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { <a> <b> <c> } }"))

  (is (=
        (query
          (base (URI. "http://example/"))
          (select \*)
          (where :s :p :o
                 (minus [:a] [:b] [:c])))

        "BASE <http://example/> SELECT * WHERE { ?s ?p ?o MINUS { <a> <b> <c> } }"))

  (is (=
        (query
          (base (URI. "http://example.com/"))
          (select \*)
          (where :x [:p] :n
                 (filter-not-exists :x [:q] :m \.
                                    (filter :n \= :m))))

        "BASE <http://example.com/> SELECT * WHERE { ?x <p> ?n FILTER NOT EXISTS { ?x <q> ?m . FILTER(?n = ?m) } }"))

  (is (=
        (query
          (base (URI. "http://example.com/"))
          (select \*)
          (where :x [:p] :n
                 (minus :x [:q] :m \.
                        (filter :n \= :m))))

        "BASE <http://example.com/> SELECT * WHERE { ?x <p> ?n MINUS { ?x <q> ?m . FILTER(?n = ?m) } }")))

(deftest part-10
  (is (=
        (query
          (select :title :price)
          (where (group :x [:ns "price"] :p \.
                        :x [:ns "discount"] :discount
                        (bind [(raw "?p*(1-?discount)") :price]))
                 (group :x [:dc "title"] :title \.)
                 (filter :price \< 20)))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title ?price WHERE { { ?x ns:price ?p . ?x ns:discount ?discount BIND(?p*(1-?discount) AS ?price) } { ?x dc:title ?title . } FILTER(?price < 20) }"))
  )

(deftest part-11
  (is (=
        (query
          (base (URI. "http://books.example/"))
          (select [(sum :lprice) :totalPrice])
          (where :org [:affiliates] :auth \.
                 :auth [:writesBook] :book \.
                 :book [:price] :lprice \.)
          (group-by :org)
          (having (sum :lprice) \> 10))

        "BASE <http://books.example/> SELECT ( SUM(?lprice) AS ?totalPrice ) WHERE { ?org <affiliates> ?auth . ?auth <writesBook> ?book . ?book <price> ?lprice . } GROUP BY ?org HAVING( SUM(?lprice) > 10 )"))

    (is (=
          (query
            (select [(avg :y) :avg])
            (where :a [:x] :x
                   \; [:y] :y \.)
            (group-by :x))

          "SELECT ( AVG(?y) AS ?avg ) WHERE { ?a <x> ?x ; <y> ?y . } GROUP BY ?x"))

    (is (=
          (query
            (base (URI. "http://data.example/"))
            (select [(avg :size) :asize])
            (where :x [:size] :size)
            (group-by :x)
            (having (avg :size) \> 10))

          "BASE <http://data.example/> SELECT ( AVG(?size) AS ?asize ) WHERE { ?x <size> ?size } GROUP BY ?x HAVING( AVG(?size) > 10 )"))

    (is (=
          (query
            (base (URI. "http://example.com/data/#"))
            (select :x [(raw "MIN(?y) * 2") :min])
            (where :x [:p] :y \. :x [:q] :z \.)
            (group-by :x (raw "(STR(?z))")))

          "BASE <http://example.com/data/#> SELECT ?x ( MIN(?y) * 2 AS ?min ) WHERE { ?x <p> ?y . ?x <q> ?z . } GROUP BY ?x (STR(?z))"))

    (is (=
          (query
            (base (URI. "http://example.com/data/#"))
            (select :g [(raw "AVG(?p) AS ?avg) ( (MIN(?p) + MAX(?p)) / 2") :c])
            (where :g [:p] :p \.)
            (group-by :g))

          "BASE <http://example.com/data/#> SELECT ?g ( AVG(?p) AS ?avg) ( (MIN(?p) + MAX(?p)) / 2 AS ?c ) WHERE { ?g <p> ?p . } GROUP BY ?g")))

    (deftest part-13
      (is (=
            (query
              (select :name)
              (from (URI. "http://example.org/foaf/aliceFoaf"))
              (where :x [:foaf "name"] :name))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name FROM <http://example.org/foaf/aliceFoaf> WHERE { ?x foaf:name ?name }"))

      (is (=
            (query
              (select :who :g :mbox)
              (from (URI. "http://example.org/dft.ttl"))
              (from-named (URI. "http://example.org/bob")
                          (URI. "http://example.org/alice"))
              (where :g [:dc "publisher"] :who \.
                     (graph :g (group :x [:foaf "mbox"] :mbox))))

            "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?who ?g ?mbox FROM <http://example.org/dft.ttl> FROM NAMED <http://example.org/bob> FROM NAMED <http://example.org/alice> WHERE { ?g dc:publisher ?who . GRAPH ?g { ?x foaf:mbox ?mbox } }"))

      (is (=
            (query
              (select :src :bobNick)
              (from-named (URI. "http://example.org/foaf/aliceFoaf")
                          (URI. "http://example.org/foaf/bobFoaf"))
              (where (graph :src
                            (group :x [:foaf "mbox"] (URI. "mailto:bob@work.example") \.
                                   :x [:foaf "nick"] :bobNick))))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?src ?bobNick FROM NAMED <http://example.org/foaf/aliceFoaf> FROM NAMED <http://example.org/foaf/bobFoaf> WHERE { GRAPH ?src { ?x foaf:mbox <mailto:bob@work.example> . ?x foaf:nick ?bobNick } }"))

      (is (=
            (query
              (select :nick)
              (from-named (URI. "http://example.org/foaf/aliceFoaf")
                          (URI. "http://example.org/foaf/bobFoaf"))
              (where (graph [:data "bobFoaf"]
                            (group :x [:foaf "mbox"] (URI. "mailto:bob@work.example") \.
                                   :x [:foaf "nick"] :nick))))

            "PREFIX data: <http://example.org/foaf/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?nick FROM NAMED <http://example.org/foaf/aliceFoaf> FROM NAMED <http://example.org/foaf/bobFoaf> WHERE { GRAPH data:bobFoaf { ?x foaf:mbox <mailto:bob@work.example> . ?x foaf:nick ?nick } }"))

      (is (=
            (query
              (select :mbox :nick :ppd)
              (from-named (URI. "http://example.org/foaf/aliceFoaf")
                          (URI. "http://example.org/foaf/bobFoaf"))
              (where
                (graph [:data "aliceFoaf"]
                       (group :alice [:foaf "mbox"] (URI. "mailto:alice@work.example")
                              \; [:foaf "knows"] :whom \.
                              :whom [:foaf "mbox"] :mbox
                              \; [:rdfs "seeAlso"] :ppd \.
                              :ppd \a [:foaf "PersonalProfileDocument"] \.)
                       \.)
                (graph :ppd
                       (group :w [:foaf "mbox"] :mbox
                              \; [:foaf "nick"] :nick))))

            "PREFIX data: <http://example.org/foaf/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?mbox ?nick ?ppd FROM NAMED <http://example.org/foaf/aliceFoaf> FROM NAMED <http://example.org/foaf/bobFoaf> WHERE { GRAPH data:aliceFoaf { ?alice foaf:mbox <mailto:alice@work.example> ; foaf:knows ?whom . ?whom foaf:mbox ?mbox ; rdfs:seeAlso ?ppd . ?ppd a foaf:PersonalProfileDocument . } . GRAPH ?ppd { ?w foaf:mbox ?mbox ; foaf:nick ?nick } }"))
      (is (=
            (query
              (select :name :mbox :date)
              (where
                :g [:dc "publisher"] :name
                \; [:dc "date"] :date \.
                (graph :g
                       (group :person [:foaf "name"] :name
                              \; [:foaf "mbox"] :mbox))))

            "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox ?date WHERE { ?g dc:publisher ?name ; dc:date ?date . GRAPH ?g { ?person foaf:name ?name ; foaf:mbox ?mbox } }")))

    (deftest part-15
      (is (=
            (query
              (select :name)
              (where :x [:foaf "name"] :name)
              (order-by :name))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name } ORDER BY ?name"))

      (is (=
            (query
              (base (URI. "http://example.org/ns#"))
              (select :name)
              (where :x [:foaf "name"] :name
                     \; [:empId] :emp)
              (order-by (desc :emp))) ; or (order-by-desc :emp)

            "BASE <http://example.org/ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name ; <empId> ?emp } ORDER BY DESC(?emp)"))

      (is (=
            (query
              (base (URI. "http://example.org/ns#"))
              (select :name)
              (where :x [:foaf "name"] :name
                     \; [:empId] :emp)
              (order-by :name (desc :emp)))

            "BASE <http://example.org/ns#> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name ; <empId> ?emp } ORDER BY ?name DESC(?emp)"))

      (is (=
            (query
              (select :name)
              (where :x [:foaf "name"] :name))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name }"))

      (is (=
            (query
              (select-distinct :name)
              (where :x [:foaf "name"] :name))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT DISTINCT ?name WHERE { ?x foaf:name ?name }"))

      (is (=
            (query
              (select-reduced :name)
              (where :x [:foaf "name"] :name))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT REDUCED ?name WHERE { ?x foaf:name ?name }"))

      (is (=
            (query
              (select :name)
              (where :x [:foaf "name"] :name)
              (order-by :name)
              (limit 5)
              (offset 10))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name } ORDER BY ?name LIMIT 5 OFFSET 10"))

      (is (=
            (query
              (select :name)
              (where :x [:foaf "name"] :name)
              (limit 20))

            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name } LIMIT 20"))
      )