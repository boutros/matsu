(ns boutros.matsu.sparql-spec
  (:refer-clojure :exclude [filter concat group-by max min])
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
                      :data    "<http://example.org/foaf/>"
                      :vcard   "<http://www.w3.org/2001/vcard-rdf/3.0#>"
                      :app     "<http://example.org/ns#>"
                      :xsd     "<http://www.w3.org/2001/XMLSchema#>"
                      :site    "<http://example.org/stats#>"
                      :ent     "<http://org.example.com/employees#>"
                      :a       "<http://www.w3.org/2000/10/annotation-ns#>"
                      :t       "<http://example.org/types#>"})

; Tests

(deftest part-2
  "Making Simple Queries (Informative)"

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

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT (CONCAT(?G, \" \", ?S) AS ?name) WHERE { ?P foaf:givenName ?G ; foaf:surname ?S }"))

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
  "3 RDF Term Constraints (Informative)"

  (is (=
        (query
          (select :title)
          (where :x [:dc "title"] :title
                 (filter- (regex :title "^SPARQL"))))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { ?x dc:title ?title FILTER regex(?title, \"^SPARQL\") }"))

  (is (=
        (query
          (select :title)
          (where :x [:dc "title"] :title
                 (filter- (regex :title "web" "i"))))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT ?title WHERE { ?x dc:title ?title FILTER regex(?title, \"web\", \"i\") }"))

  (is (=
        (query
          (select :title :price)
          (where :x [:ns "price"] :price \.
                 (filter :price \< 30.5)
                 :x [:dc "title"] :title \.))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title ?price WHERE { ?x ns:price ?price . FILTER(?price < 30.5) ?x dc:title ?title . }")))

(deftest part-4
  "SPARQL Syntax"

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
  "Graph Patterns"

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

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name ?mbox WHERE { { ?x foaf:name ?name . } { ?x foaf:mbox ?mbox . } }")))

(deftest part-6
  "Including Optional Values"

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
  "Matching Alternatives"

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
  "Negation"

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
          (where- :s :p :o
                 (filter-not-exists :x :y :z)))

        "SELECT * { ?s ?p ?o FILTER NOT EXISTS { ?x ?y ?z } }"))

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
  "Assignment"

  (is (=
        (query
          (select :title :price)
          (where (group :x [:ns "price"] :p \.
                        :x [:ns "discount"] :discount
                        (bind [(raw "?p*(1-?discount)") :price]))
                 (group :x [:dc "title"] :title \.)
                 (filter :price \< 20)))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title ?price WHERE { { ?x ns:price ?p . ?x ns:discount ?discount BIND(?p*(1-?discount) AS ?price) } { ?x dc:title ?title . } FILTER(?price < 20) }")))

(deftest part-11
  "Aggregates"

  (is (=
        (query
          (base (URI. "http://books.example/"))
          (select [(sum :lprice) :totalPrice])
          (where :org [:affiliates] :auth \.
                 :auth [:writesBook] :book \.
                 :book [:price] :lprice \.)
          (group-by :org)
          (having (sum :lprice) \> 10))

        "BASE <http://books.example/> SELECT (SUM(?lprice) AS ?totalPrice) WHERE { ?org <affiliates> ?auth . ?auth <writesBook> ?book . ?book <price> ?lprice . } GROUP BY ?org HAVING(SUM(?lprice) > 10)"))

  (is (=
        (query
          (select [(avg :y) :avg])
          (where :a [:x] :x
                 \; [:y] :y \.)
                 (group-by :x))

          "SELECT (AVG(?y) AS ?avg) WHERE { ?a <x> ?x ; <y> ?y . } GROUP BY ?x"))

  (is (=
        (query
          (base (URI. "http://data.example/"))
          (select [(avg :size) :asize])
          (where :x [:size] :size)
          (group-by :x)
          (having (avg :size) \> 10))

        "BASE <http://data.example/> SELECT (AVG(?size) AS ?asize) WHERE { ?x <size> ?size } GROUP BY ?x HAVING(AVG(?size) > 10)"))

  (is (=
        (query
          (base (URI. "http://example.com/data/#"))
          (select :x [(raw "MIN(?y) * 2") :min])
          (where :x [:p] :y \. :x [:q] :z \.)
          (group-by :x (raw "(STR(?z))")))

        "BASE <http://example.com/data/#> SELECT ?x (MIN(?y) * 2 AS ?min) WHERE { ?x <p> ?y . ?x <q> ?z . } GROUP BY ?x (STR(?z))"))

  (is (=
        (query
          (base (URI. "http://example.com/data/#"))
          (select :g [(raw "AVG(?p) AS ?avg) ( (MIN(?p) + MAX(?p)) / 2") :c])
          (where :g [:p] :p \.)
          (group-by :g))

        "BASE <http://example.com/data/#> SELECT ?g (AVG(?p) AS ?avg) ( (MIN(?p) + MAX(?p)) / 2 AS ?c) WHERE { ?g <p> ?p . } GROUP BY ?g")))

(deftest part-13
  "RDF Datasets"

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
  "Solution Sequences and Modifiers"

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

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:name ?name } LIMIT 20")))


(deftest part-16
  "Query Forms"

  (is (=
        (query
          (select :nameX :nameY :nickY)
          (where :x [:foaf "knows"] :y
                 \; [:foaf "name"] :nameX \.
                 :y [:foaf "name"] :nameY \.
                 (optional :y [:foaf "nick"] :nickY)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?nameX ?nameY ?nickY WHERE { ?x foaf:knows ?y ; foaf:name ?nameX . ?y foaf:name ?nameY . OPTIONAL { ?y foaf:nick ?nickY } }"))

  (is (=
        (query
          (select :title [(raw "?p*(1-?discount)") :price])
          (where :x [:ns "price"] :p \.
                 :x [:dc "title"] :title \.
                 :x [:ns "discount"] :discount))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title (?p*(1-?discount) AS ?price) WHERE { ?x ns:price ?p . ?x dc:title ?title . ?x ns:discount ?discount }"))

  (is (=
        (query
          (select :title [(raw "?p AS ?fullPrice) (?fullPrice*(1-?discount)") :customerPrice])
          (where :x [:ns "price"] :p \.
                 :x [:dc "title"] :title \.
                 :x [:ns "discount"] :discount))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX ns: <http://example.org/ns#> SELECT ?title (?p AS ?fullPrice) (?fullPrice*(1-?discount) AS ?customerPrice) WHERE { ?x ns:price ?p . ?x dc:title ?title . ?x ns:discount ?discount }"))

  (is (=
        (query
          (construct (URI. "http://example.org/person#Alice") [:vcard "FN"] :name)
          (where :x [:foaf "name"] :name))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX vcard: <http://www.w3.org/2001/vcard-rdf/3.0#> CONSTRUCT { <http://example.org/person#Alice> vcard:FN ?name } WHERE { ?x foaf:name ?name }"))

  (is (=
        (query
          (construct :x [:vcard "N"] '_:v \.
                     '_:v [:vcard "givenName"] :gname \.
                     '_:v [:vcard "familyName"] :fname)
          (where (union
                   (group :x [:foaf "firstname"] :gname)
                   (group :x [:foaf "givenname"] :gname)) \.
                 (union
                   (group :x [:foaf "surname"] :fname)
                   (group :x [:foaf "family_name"] :fname)) \.))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX vcard: <http://www.w3.org/2001/vcard-rdf/3.0#> CONSTRUCT { ?x vcard:N _:v . _:v vcard:givenName ?gname . _:v vcard:familyName ?fname } WHERE { { ?x foaf:firstname ?gname } UNION { ?x foaf:givenname ?gname } . { ?x foaf:surname ?fname } UNION { ?x foaf:family_name ?fname } . }"))

  (is (=
        (query
          (construct :s :p :o)
          (where (graph (URI. "http://example.org/aGraph") (group :s :p :o) \.)))

        "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/aGraph> { ?s ?p ?o } . }"))

  (is (=
        (query
          (construct :s :p :o)
          (where
            (graph :g (group :s :p :o) \.)
            :g [:dc "publisher"] (URI. "http://www.w3.org/") \.
            :g [:dc "date"] :date \.
            (filter [:app "customDate(?date)"] \> (raw "\"2005-02-28T00:00:00Z\"^^xsd:dateTime")) \.))

        "PREFIX app: <http://example.org/ns#> PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } . ?g dc:publisher <http://www.w3.org/> . ?g dc:date ?date . FILTER(app:customDate(?date) > \"2005-02-28T00:00:00Z\"^^xsd:dateTime) . }"))

  (is (=
        (query
          (construct (raw "[]") [:foaf "name"] :name)
          (where (raw "[]") [:foaf "name"] :name
                 \; [:site "hits"] :hits \.)
          (order-by-desc :hits)
          (limit 2))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX site: <http://example.org/stats#> CONSTRUCT { [] foaf:name ?name } WHERE { [] foaf:name ?name ; site:hits ?hits . } ORDER BY DESC(?hits) LIMIT 2"))

  (is (=
        (query
          (construct :x [:foaf "name"] :name)
          (where :x [:foaf "name"] :name))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> CONSTRUCT { ?x foaf:name ?name } WHERE { ?x foaf:name ?name }"))

  (is (=
        (query
          (ask :x [:foaf "name"] "Alice"))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK { ?x foaf:name \"Alice\" }"))

  (is (=
        (query
          (ask :x [:foaf "name"] "Alice"
               \; [:foaf "mbox"] (URI. "mailto:alice@work.example")))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ASK { ?x foaf:name \"Alice\" ; foaf:mbox <mailto:alice@work.example> }"))

  (is (=
        (query
          (describe (URI. "http://example.org/")))

        "DESCRIBE <http://example.org/>"))

  (is (=
        (query
          (describe :x)
          (where :x [:foaf "mbox"] (URI. "mailto:alice@org")))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DESCRIBE ?x WHERE { ?x foaf:mbox <mailto:alice@org> }"))

  (is (=
        (query
          (describe :x)
          (where :x [:foaf "name"] "Alice"))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DESCRIBE ?x WHERE { ?x foaf:name \"Alice\" }"))

  (is (=
        (query
          (describe :x :y (URI. "http://example.org/"))
          (where :x [:foaf "knows"] :y))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> DESCRIBE ?x ?y <http://example.org/> WHERE { ?x foaf:knows ?y }"))

  (is (=
        (query
          (describe :x)
          (where :x [:ent "employeeId"] "1234"))

        "PREFIX ent: <http://org.example.com/employees#> DESCRIBE ?x WHERE { ?x ent:employeeId \"1234\" }")))

(deftest part-17
  "Expressions and Testing Values"

  (is (=
        (query
          (select :annot)
          (where :annot [:a "annotates"] (URI. "http://www.w3.org/TR/rdf-sparql-query/") \.
                 :annot [:dc "date"] :date \.
                 (filter :date \> (raw "\"2005-01-01T00:00:00Z\"^^xsd:dateTime"))))

        "PREFIX a: <http://www.w3.org/2000/10/annotation-ns#> PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?annot WHERE { ?annot a:annotates <http://www.w3.org/TR/rdf-sparql-query/> . ?annot dc:date ?date . FILTER(?date > \"2005-01-01T00:00:00Z\"^^xsd:dateTime) }"))

  (is (=
        (query
          (select :givenName)
          (where :x [:foaf "givenName"] :givenName \.
                 (optional :x [:dc "date"] :date) \.
                 (filter (bound :date))))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?givenName WHERE { ?x foaf:givenName ?givenName . OPTIONAL { ?x dc:date ?date } . FILTER(bound(?date)) }"))

  (is (=
        (query
          (select :name)
          (where :x [:foaf "givenName"] :name \.
                 (optional :x [:dc "date"] :date) \.
                 (filter (!bound :date))))

        "PREFIX dc: <http://purl.org/dc/elements/1.1/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name WHERE { ?x foaf:givenName ?name . OPTIONAL { ?x dc:date ?date } . FILTER(!bound(?date)) }"))

  (is (=
        (query
          (select :name1 :name2)
          (where :x [:foaf "name"] :name1
                 \; [:foaf "mbox"] :mbox1 \.
                 :y [:foaf "name"] :name2
                 \; [:foaf "mbox"] :mbox2 \.
                 (filter :mbox1 \= :mbox2 '&& :name1 '!= :name2)))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name1 ?name2 WHERE { ?x foaf:name ?name1 ; foaf:mbox ?mbox1 . ?y foaf:name ?name2 ; foaf:mbox ?mbox2 . FILTER(?mbox1 = ?mbox2 && ?name1 != ?name2) }"))

  (is (=
        (query
          (select :name1 :name2)
          (where :x [:foaf "name"] :name1
                 \; [:foaf "mbox"] :mbox1 \.
                 :y [:foaf "name"] :name2
                 \; [:foaf "mbox"] :mbox2 \.
                 (filter (same-term :mbox1, :mbox2) '&& (!same-term :name1 :name2))))

        "PREFIX foaf: <http://xmlns.com/foaf/0.1/> SELECT ?name1 ?name2 WHERE { ?x foaf:name ?name1 ; foaf:mbox ?mbox1 . ?y foaf:name ?name2 ; foaf:mbox ?mbox2 . FILTER(sameTerm(?mbox1, ?mbox2) && !sameTerm(?name1, ?name2)) }"))

  (is (=
        (query
          (base (URI. "http://example.org/WMterms#"))
          (select :aLabel1, :bLabel)
          (where :a [:label] :aLabel \.
                 :a [:weight] :aWeight \.
                 :a [:displacement] :aDisp \.
                 :b [:label] :bLabel \.
                 :b [:weight] :bWeight \.
                 :b [:displacement] :bDisp \.
                 (filter (same-term :aWeight :bWeight) '&&
                         (!same-term :aDisp :bDisp))))

        "BASE <http://example.org/WMterms#> SELECT ?aLabel1 ?bLabel WHERE { ?a <label> ?aLabel . ?a <weight> ?aWeight . ?a <displacement> ?aDisp . ?b <label> ?bLabel . ?b <weight> ?bWeight . ?b <displacement> ?bDisp . FILTER(sameTerm(?aWeight, ?bWeight) && !sameTerm(?aDisp, ?bDisp)) }"))
    )