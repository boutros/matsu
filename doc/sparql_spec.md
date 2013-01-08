#SPARQL 1.1 specification

An attempt to translate all the example queries from the [SPARQL 1.1 specification from W3C](http://www.w3.org/TR/sparql11-query/) into Matsu syntax. This document corresponds with the tests in `/test/boutros/matsu/sparql_spec.clj`.

The following namespaces are assumed to be registered:
```clojure
@PREFIXES
{:foaf    "<http://xmlns.com/foaf/0.1/>"
 :org     "<http://example.com/ns#>"
 :dc      "<http://purl.org/dc/elements/1.1/>"
 :ns      "<http://example.org/ns#>"
 :data    "<http://example.org/foaf/>"
 :vcard   "<http://www.w3.org/2001/vcard-rdf/3.0#>"
 :app     "<http://example.org/ns#>"
 :xsd     "<http://www.w3.org/2001/XMLSchema#>"
 :ent     "<http://org.example.com/employees#>"
 :a       "<http://www.w3.org/2000/10/annotation-ns#>"
 :t       "<http://example.org/types#>"}
```

## 2 Making Simple Queries (Informative)

### 2.1 Writing a Simple Query

```sparql
SELECT ?title
WHERE
{
  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .
}
```

```clojure
(query
  (select :title)
  (where (URI. "http://example.org/book/book1") (URI. "http://example.org/book/book1") :title) \.))
```

### 2.2 Multiple Matches

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE
  { ?x foaf:name ?name .
    ?x foaf:mbox ?mbox }
```

```clojure
(query
  (select :name :mbox)
  (where :x [:foaf "name"] :name \.
         :x [:foaf "mbox"] :mbox))
```

### 2.3 Matching RDF Literals


```sparql
SELECT ?v WHERE { ?v ?p "cat" }
```

```clojure
(query
  (select :v)
  (where :v :p "cat"))
```

```sparql
SELECT ?v WHERE { ?v ?p "cat"@en }
```

```clojure
(query
  (select :v)
  (where :v :p ["cat" :en]))
```

```sparql
SELECT ?v WHERE { ?v ?p 42 }
```

```clojure
(query
  (select :v)
  (where :v :p 42))
```

```sparql
SELECT ?v WHERE { ?v ?p "abc"^^<http://example.org/datatype#specialDatatype> }
```

```clojure
(query
  (select :v)
  (where :v :p (raw "\"abc\"^^<http://example.org/datatype#specialDatatype>")))
```

### 2.4 Blank Node Labels in Query Results

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ?x ?name
WHERE  { ?x foaf:name ?name }
```

```clojure
(query
  (select :x :name)
  (where :x [:foaf "name"] :name))
```

### 2.5 Creating Values with Expressions

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ( CONCAT(?G, " ", ?S) AS ?name )
WHERE  { ?P foaf:givenName ?G ; foaf:surname ?S }
```

```clojure
(query
  (select [(concat :G " " :S) :name])
  (where :P [:foaf "givenName"] :G
         \; [:foaf "surname"] :S))
```

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE  {
   ?P foaf:givenName ?G ;
      foaf:surname ?S
   BIND(CONCAT(?G, " ", ?S) AS ?name)
}
```

```clojure
(query
  (select :name)
  (where :P [:foaf "givenName"] :G
         \; [:foaf "surname"] :S
         (bind [(concat :G " " :S) :name])))
```

### 2.6 Building RDF Graphs

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
PREFIX org:    <http://example.com/ns#>

CONSTRUCT { ?x foaf:name ?name }
WHERE  { ?x org:employeeName ?name }
```

```clojure
(query
  (construct :x [:foaf "name"] :name)
  (where :x [:org "employeeName"] :name))
```

## 3 RDF Term Constraints (Informative)

### 3.1 Restricting the Value of Strings

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
SELECT  ?title
WHERE   { ?x dc:title ?title
          FILTER regex(?title, "^SPARQL")
        }
```

```clojure
(query
  (select :title)
  (where :x [:dc "title"] :title
         (filter-regex :title "^SPARQL")))
```

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
SELECT  ?title
WHERE   { ?x dc:title ?title
          FILTER regex(?title, "web", "i" )
        }
```


```clojure
(query
  (select :title)
  (where :x [:dc "title"] :title
         (filter-regex :title "web" "i")))
```

### 3.2 Restricting Numeric Values

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
PREFIX  ns:  <http://example.org/ns#>
SELECT  ?title ?price
WHERE   { ?x ns:price ?price .
          FILTER (?price < 30.5)
          ?x dc:title ?title . }
```

```clojure
(query
  (select :title :price)
    (where :x [:ns "price"] :price \.
           (filter :price \< 30.5)
           :x [:dc "title"] :title \.))
```

## SPARQL Syntax

### 4.2 Syntax for Triple Patterns

```sparql
PREFIX  dc: <http://purl.org/dc/elements/1.1/>
SELECT  ?title
WHERE   { <http://example.org/book/book1> dc:title ?title }
```

```clojure
(query
  (select :title)
  (where (URI. "http://example.org/book/book1") [:dc "title"] :title))
```

```sparql
PREFIX  dc: <http://purl.org/dc/elements/1.1/>
PREFIX  : <http://example.org/book/>

SELECT  $title
WHERE   { :book1  dc:title  $title }
```

```clojure
(query
  ...)
```
Currently not possible, since Clojure doesn't allow the keyword `::`. Use `base` instead, see below.

Dollar-prefixed variables are not supported either (`$title` is equal to `?title` anyway).

```
BASE    <http://example.org/book/>
PREFIX  dc: <http://purl.org/dc/elements/1.1/>

SELECT  $title
WHERE   { <book1>  dc:title  ?title }
```

```clojure
  (query
    (base (URI. "http://example.org/book"))
    (select :title)
    (where [book1] [:dc "title"] :title))
```

## 5 Graph Patterns

### 5.2 Group Graph Patterns

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE  {
          ?x foaf:name ?name .
          ?x foaf:mbox ?mbox .
       }
```

```clojure
(query
  (select :name :mbox)
  (where :x [:foaf "name"] :name \.
         :x [:foaf "mbox"] :mbox \.))
```

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE  { { ?x foaf:name ?name . }
         { ?x foaf:mbox ?mbox . }
       }
```

```clojure
(query
  (select :name :mbox)
  (where (group :x [:foaf "name"] :name \.)
         (group :x [:foaf "mbox"] :mbox \.)))
```


## 6 Including Optional Values

### 6.1 Optional Pattern Matching

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE  { ?x foaf:name  ?name .
         OPTIONAL { ?x  foaf:mbox  ?mbox }
       }
```

```clojure
(query
  (select :name :mbox)
  (where :x [:foaf "name"] :name \.
         (optional :x [:foaf "mbox"] :mbox)))
```

### 6.2 Constraints in Optional Pattern Matching

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
PREFIX  ns:  <http://example.org/ns#>
SELECT  ?title ?price
WHERE   { ?x dc:title ?title .
          OPTIONAL { ?x ns:price ?price . FILTER (?price < 30) }
        }
```

```clojure
(query
  (select :title :price)
  (where :x [:dc "title"] :title \.
         (optional :x [:ns "price"] :price \. (filter :price \< 30))))
```

### 6.3 Multiple Optional Graph Patterns

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox ?hpage
WHERE  { ?x foaf:name  ?name .
         OPTIONAL { ?x foaf:mbox ?mbox } .
         OPTIONAL { ?x foaf:homepage ?hpage }
       }
```

```clojure
(query
  (select :name :mbox :hpage)
  (where :x [:foaf "name"] :name \.
         (optional :x [:foaf "mbox"] :mbox) \.
         (optional :x [:foaf "homepage"] :hpage)))
```

## 7 Matching Alternatives

```sparql
PREFIX dc10:  <http://purl.org/dc/elements/1.0/>
PREFIX dc11:  <http://purl.org/dc/elements/1.1/>

SELECT ?title
WHERE  { { ?book dc10:title  ?title } UNION { ?book dc11:title  ?title } }
```

```clojure
(query
  (select :title)
  (where (union (group :book [:dc10 "title"] :title)
                (group :book [:dc11 "title"] :title))))
```

```sparql
PREFIX dc10:  <http://purl.org/dc/elements/1.0/>
PREFIX dc11:  <http://purl.org/dc/elements/1.1/>

SELECT ?x ?y
WHERE  { { ?book dc10:title ?x } UNION { ?book dc11:title  ?y } }
```

```clojure
(query
  (select :x :y)
  (where (union (group :book [:dc10 "title"] :x)
                (group :book [:dc11 "title"] :y))))
```

```sparql
PREFIX dc10:  <http://purl.org/dc/elements/1.0/>
PREFIX dc11:  <http://purl.org/dc/elements/1.1/>

SELECT ?title ?author
WHERE  { { ?book dc10:title ?title .  ?book dc10:creator ?author }
         UNION
         { ?book dc11:title ?title .  ?book dc11:creator ?author }
       }
```

```clojure
(query
  (select :title :author)
  (where (union (group :book [:dc10 "title"] :title \. :book [:dc10 "creator"] :author)
                (group :book [:dc11 "title"] :title \. :book [:dc11 "creator"] :author))))
```
## 8 Negation

### 8.1 Filtering Using Graph Patterns

```sparql
PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX  foaf:   <http://xmlns.com/foaf/0.1/>

SELECT ?person
WHERE
{
    ?person rdf:type  foaf:Person .
    FILTER NOT EXISTS { ?person foaf:name ?name }
}
```

```clojure
(query
  (select :person)
  (where :person [:rdf "type"] [:foaf "Person"] \.
         (filter-not-exists :person [:foaf "name"] :name)))
```

```sparql
PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX  foaf:   <http://xmlns.com/foaf/0.1/>

SELECT ?person
WHERE
{
    ?person rdf:type  foaf:Person .
    FILTER EXISTS { ?person foaf:name ?name }
}
```

```clojure
(query
  (select :person)
  (where :person [:rdf "type"] [:foaf "Person"] \.
         (filter-exists :person [:foaf "name"] :name)))
```

### 8.2 Removing Possible Solutions

```sparql
PREFIX :       <http://example/>
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>

SELECT DISTINCT ?s
WHERE {
   ?s ?p ?o .
   MINUS {
      ?s foaf:givenName "Bob" .
   }
}
```

```clojure
(query
  (select-distinct :s)
  (where :s :p :o \.
         (minus :s [:foaf "givenName"] "Bob" \.)))
```

### 8.3 Relationship and differences between NOT EXISTS and MINUS

```sparql
SELECT *
{
  ?s ?p ?o
  FILTER NOT EXISTS { ?x ?y ?z }
}
```
Currently not possible without the `WHERE` keyword (which is optional in SPARQL):
```clojure
(query
  (select \*)
  (where :s :p :o
         (filter-not-exists :x :y :z)))
```

```sparql
SELECT *
{
   ?s ?p ?o
   MINUS
     { ?x ?y ?z }
}
```

```clojure
(query
  (select \*)
  (where :s :p :o
         (minus :x :y :z)))
```

```sparql
PREFIX : <http://example/>
SELECT *
{
  ?s ?p ?o
  FILTER NOT EXISTS { :a :b :c }
}
```
Using `BASE` URI instead of `:`
```clojure
(query
  (base (URI. "http://example/"))
  (select \*)
  (where :s :p :o
         (filter-not-exists [:a] [:b] [:c])))
```

```sparql
PREFIX : <http://example/>
SELECT *
{
  ?s ?p ?o
  MINUS { :a :b :c }
}
```

```clojure
(query
  (base (URI. "http://example/"))
  (select \*)
  (where :s :p :o
         (minus [:a] [:b] [:c])))
```

```sparql
PREFIX : <http://example.com/>
SELECT * WHERE {
        ?x :p ?n
        FILTER NOT EXISTS {
                ?x :q ?m .
                FILTER(?n = ?m)
        }
}
```

```clojure
(query
  (base (URI. "http://example.com/"))
  (select \*)
  (where :x [:p] :n
         (filter-not-exists :x [:q] :m \.
                            (filter :n \= :m))))
```

```sparql
PREFIX : <http://example/>
SELECT * WHERE {
        ?x :p ?n
        MINUS {
                ?x :q ?m .
                FILTER(?n = ?m)
        }
}
```

```clojure
(query
  (base (URI. "http://example.com/"))
  (select \*)
  (where :x [:p] :n
         (minus :x [:q] :m \.
                (filter :n \= :m))))
```

## 9 Property Paths

I haven't decided how to implement the propert path syntax yet.

## 10 Assignment

### 10.1 BIND: Assigning to Variables

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
PREFIX  ns:  <http://example.org/ns#>

SELECT  ?title ?price
{  { ?x ns:price ?p .
     ?x ns:discount ?discount
     BIND(?p*(1-?discount) AS ?price)
   }
   {?x dc:title ?title . }
   FILTER(?price < 20)
}
```
Not quite possible yet without resorting to `raw`:
```clojure
(query
  (select :title :price)
  (where (group :x [:ns "price"] :p \.
                :x [:ns "discount"] :discount
                (bind [(raw "?p*(1-?discount)") :price]))
         (group :x [:dc "title"] :title \.)
         (filter :price \< 20)))
```

```sparql
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
PREFIX :     <http://example.org/book/>
PREFIX ns:   <http://example.org/ns#>

SELECT ?book ?title ?price
{
   VALUES ?book { :book1 :book3 }
   ?book dc:title ?title ;
         ns:price ?price .
}
```

```clojure
(query ...) ;currently not possible
```
### 10.2 VALUES: Providing inline data

```sparql
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
PREFIX :     <http://example.org/book/>
PREFIX ns:   <http://example.org/ns#>

SELECT ?book ?title ?price
{
   ?book dc:title ?title ;
         ns:price ?price .
   VALUES (?book ?title)
   { (UNDEF "SPARQL Tutorial")
     (:book2 UNDEF)
   }
}
```

```clojure
(query ...) ;currently not possible
```

```sparql
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
PREFIX :     <http://example.org/book/>
PREFIX ns:   <http://example.org/ns#>

SELECT ?book ?title ?price
{
   ?book dc:title ?title ;
         ns:price ?price .
}
VALUES (?book ?title)
{ (UNDEF "SPARQL Tutorial")
  (:book2 UNDEF)
}
```

```clojure
(query ...) ;currently not possible
```

## 11 Aggregates

### 11.1 Aggregate Example

```sparql
PREFIX : <http://books.example/>
SELECT (SUM(?lprice) AS ?totalPrice)
WHERE {
  ?org :affiliates ?auth .
  ?auth :writesBook ?book .
  ?book :price ?lprice .
}
GROUP BY ?org
HAVING (SUM(?lprice) > 10)
```

```clojure
(query
  (base (URI. "http://books.example/"))
  (select [(sum :lprice) :totalPrice])
  (where :org [:affiliates] :auth \.
         :auth [:writesBook] :book \.
         :book [:price] :lprice \.)
  (group-by :org)
  (having (sum :lprice) \> 10))
```

### 11.2 GROUP BY

```sparql
SELECT (AVG(?y) AS ?avg)
WHERE {
  ?a :x ?x ;
     :y ?y .
}
GROUP BY ?x
```

```clojure
(query
  (select [(avg :y) :avg])
  (where :a [:x] :x
         \; [:y] :y \.)
  (group-by :x))
```

### 11.3 HAVING

```sparql
PREFIX : <http://data.example/>
SELECT (AVG(?size) AS ?asize)
WHERE {
  ?x :size ?size
}
GROUP BY ?x
HAVING(AVG(?size) > 10)
```

```clojure
(query
  (base (URI. "http://data.example/"))
  (select [(avg :size) :asize])
  (where :x [:size] :size)
  (group-by :x)
  (having (avg :size) \> 10))
```

### 11.4 Aggregate Projection Restrictions

```sparql
PREFIX : <http://example.com/data/#>
SELECT ?x (MIN(?y) * 2 AS ?min)
WHERE {
  ?x :p ?y .
  ?x :q ?z .
} GROUP BY ?x (STR(?z))
```
Not quite there yet (must rethink the query data structure to better deal with expressions):
```clojure
(query
  (base (URI. "http://example.com/data/#"))
  (select :x [(raw "MIN(?y) * 2") :min])
  (where :x [:p] :y \. :x [:q] :z \.)
  (group-by :x (raw "(STR(?z))")))
```

### 11.5 Aggregate Example (with errors)

```sparql
PREFIX : <http://example.com/data/#>
SELECT ?g (AVG(?p) AS ?avg) ((MIN(?p) + MAX(?p)) / 2 AS ?c)
WHERE {
  ?g :p ?p .
}
GROUP BY ?g
```

```clojure
(query
  (base (URI. "http://example.com/data/#"))
  (select :g [(raw "AVG(?p) AS ?avg) ( (MIN(?p) + MAX(?p)) / 2") :c])
  (where :g [:p] :p \.)
  (group-by :g))
```

## 12 Subqueries

```sparql
PREFIX : <http://people.example/>
PREFIX : <http://people.example/>
SELECT ?y ?minName
WHERE {
  :alice :knows ?y .
  {
    SELECT ?y (MIN(?name) AS ?minName)
    WHERE {
      ?y :name ?name .
    } GROUP BY ?y
  }
}
```
Not supported yet, use `raw`.
```clojure
(query ...)
```

## 13 RDF Dataset

### 13.2 Specifying RDF Datasets

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT  ?name
FROM    <http://example.org/foaf/aliceFoaf>
WHERE   { ?x foaf:name ?name }
```

```clojure
(query
  (select :name)
  (from (URI. "http://example.org/foaf/aliceFoaf"))
  (where :x [:foaf "name"] :name))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

SELECT ?who ?g ?mbox
FROM <http://example.org/dft.ttl>
FROM NAMED <http://example.org/alice>
FROM NAMED <http://example.org/bob>
WHERE
{
   ?g dc:publisher ?who .
   GRAPH ?g { ?x foaf:mbox ?mbox }
}
```

```clojure
(query
  (select :who :g :mbox)
  (from (URI. "http://example.org/dft.ttl"))
  (from-named (URI. "http://example.org/bob")
              (URI. "http://example.org/alice"))
  (where :g [:dc "publisher"] :who \.
         (graph :g (group :x [:foaf "mbox"] :mbox))))
```

### 13.3 Querying the Dataset


```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

SELECT ?src ?bobNick
FROM NAMED <http://example.org/foaf/aliceFoaf>
FROM NAMED <http://example.org/foaf/bobFoaf>
WHERE
  {
    GRAPH ?src
    { ?x foaf:mbox <mailto:bob@work.example> .
      ?x foaf:nick ?bobNick
    }
  }

```

```clojure
(query
  (select :src :bobNick)
  (from-named (URI. "http://example.org/foaf/aliceFoaf")
              (URI. "http://example.org/foaf/bobFoaf"))
  (where (graph :src
                (group :x [:foaf "mbox"] (URI. "mailto:bob@work.example") \.
                       :x [:foaf "nick"] :bobNick))))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX data: <http://example.org/foaf/>

SELECT ?nick
FROM NAMED <http://example.org/foaf/aliceFoaf>
FROM NAMED <http://example.org/foaf/bobFoaf>
WHERE
  {
     GRAPH data:bobFoaf {
         ?x foaf:mbox <mailto:bob@work.example> .
         ?x foaf:nick ?nick }
  }
```

```clojure
(query
  (select :nick)
  (from-named (URI. "http://example.org/foaf/aliceFoaf")
              (URI. "http://example.org/foaf/bobFoaf"))
  (where (graph [:data "bobFoaf"]
                (group :x [:foaf "mbox"] (URI. "mailto:bob@work.example") \.
                       :x [:foaf "nick"] :nick))))
```

```sparql
PREFIX  data:  <http://example.org/foaf/>
PREFIX  foaf:  <http://xmlns.com/foaf/0.1/>
PREFIX  rdfs:  <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?mbox ?nick ?ppd
FROM NAMED <http://example.org/foaf/aliceFoaf>
FROM NAMED <http://example.org/foaf/bobFoaf>
WHERE
{
  GRAPH data:aliceFoaf
  {
    ?alice foaf:mbox <mailto:alice@work.example> ;
           foaf:knows ?whom .
    ?whom  foaf:mbox ?mbox ;
           rdfs:seeAlso ?ppd .
    ?ppd  a foaf:PersonalProfileDocument .
  } .
  GRAPH ?ppd
  {
      ?w foaf:mbox ?mbox ;
         foaf:nick ?nick
  }
}
```

```clojure
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
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc:   <http://purl.org/dc/elements/1.1/>

SELECT ?name ?mbox ?date
WHERE
  {  ?g dc:publisher ?name ;
        dc:date ?date .
    GRAPH ?g
      { ?person foaf:name ?name ; foaf:mbox ?mbox }
  }
```

```clojure
(query
  (select :name :mbox :date)
  (where
    :g [:dc "publisher"] :name
    \; [:dc "date"] :date \.
    (graph :g
           (group :person [:foaf "name"] :name
                  \; [:foaf "mbox"] :mbox))))
```

## 14 Basic Federated Query

Not supported yet.

## 15 Solution Sequences and Modifiers

### 15.1 ORDER BY

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>

SELECT ?name
WHERE { ?x foaf:name ?name }
ORDER BY ?name
```

```clojure
(query
  (select :name)
  (where :x [:foaf "name"] :name)
  (order-by :name))
```

```sparql
PREFIX     :    <http://example.org/ns#>
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>

SELECT ?name
WHERE { ?x foaf:name ?name ; :empId ?emp }
ORDER BY DESC(?emp)
```

```clojure
(query
  (base (URI. "http://example.org/ns#"))
  (select :name)
  (where :x [:foaf "name"] :name
         \; [:empId] :emp)
  (order-by (desc :emp))) ; or (order-by-desc :emp)
```

```sparql
PREFIX     :    <http://example.org/ns#>
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>

SELECT ?name
WHERE { ?x foaf:name ?name ; :empId ?emp }
ORDER BY ?name DESC(?emp)
```

```clojure
(query
  (base (URI. "http://example.org/ns#"))
  (select :name)
  (where :x [:foaf "name"] :name
         \; [:empId] :emp)
  (order-by :name (desc :emp)))
```

### 15.2 Projection

```sparql
PREFIX foaf:       <http://xmlns.com/foaf/0.1/>
SELECT ?name
WHERE
 { ?x foaf:name ?name }
```

```clojure
(query
  (select :name)
  (where :x [:foaf "name"] :name))
```

### 15.3 Duplicate Solutions

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
SELECT DISTINCT ?name WHERE { ?x foaf:name ?name }
```

```clojure
(query
  (select-distinct :name)
  (where :x [:foaf "name"] :name))
```

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
SELECT REDUCED ?name WHERE { ?x foaf:name ?name }
```

```clojure
(query
  (select-reduced :name)
  (where :x [:foaf "name"] :name))
```

### 15.4 OFFSET

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>

SELECT  ?name
WHERE   { ?x foaf:name ?name }
ORDER BY ?name
LIMIT   5
OFFSET  10
```

```clojure
(query
  (select :name)
  (where :x [:foaf "name"] :name)
  (order-by :name)
  (limit 5)
  (offset 10))
```

### 15.5 LIMIT

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>

SELECT ?name
WHERE { ?x foaf:name ?name }
LIMIT 20
```

```clojure
(query
  (select :name)
  (where :x [:foaf "name"] :name)
  (limit 20))
```

## 16 Query Forms

### 16.1 SELECT

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
SELECT ?nameX ?nameY ?nickY
WHERE
  { ?x foaf:knows ?y ;
       foaf:name ?nameX .
    ?y foaf:name ?nameY .
    OPTIONAL { ?y foaf:nick ?nickY }
  }
```

```clojure
(query
  (select :nameX :nameY :nickY)
  (where :x [:foaf "knows"] :y
         \; [:foaf "name"] :nameX \.
         :y [:foaf "name"] :nameY \.
         (optional :y [:foaf "nick"] :nickY)))
```

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
PREFIX  ns:  <http://example.org/ns#>
SELECT  ?title (?p*(1-?discount) AS ?price)
{ ?x ns:price ?p .
  ?x dc:title ?title .
  ?x ns:discount ?discount
}
```

```clojure
(query
  (select :title [(raw "?p*(1-?discount)") :price])
  (where :x [:ns "price"] :p \.
         :x [:dc "title"] :title \.
         :x [:ns "discount"] :discount))
```

```sparql
PREFIX  dc:  <http://purl.org/dc/elements/1.1/>
PREFIX  ns:  <http://example.org/ns#>
SELECT  ?title (?p AS ?fullPrice) (?fullPrice*(1-?discount) AS ?customerPrice)
{ ?x ns:price ?p .
   ?x dc:title ?title .
   ?x ns:discount ?discount
}
```

```clojure
(query
  (select :title [(raw "?p AS ?fullPrice) (?fullPrice*(1-?discount)") :customerPrice])
  (where :x [:ns "price"] :p \.
         :x [:dc "title"] :title \.
         :x [:ns "discount"] :discount))
```

### 16.2 CONSTRUCT

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>
CONSTRUCT   { <http://example.org/person#Alice> vcard:FN ?name }
WHERE       { ?x foaf:name ?name }
```

```clojure
(query
  (construct (URI. "http://example.org/person#Alice") [:vcard "FN"] :name)
  (where :x [:foaf "name"] :name))
```

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>

CONSTRUCT { ?x  vcard:N _:v .
            _:v vcard:givenName ?gname .
            _:v vcard:familyName ?fname }
WHERE
 {
    { ?x foaf:firstname ?gname } UNION  { ?x foaf:givenname   ?gname } .
    { ?x foaf:surname   ?fname } UNION  { ?x foaf:family_name ?fname } .
 }
```
TODO think about how to to deal with blank nodes, for now, just quote:
```clojure
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
```

```sparql
CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/aGraph> { ?s ?p ?o } . }
```

```clojure
(query
  (construct :s :p :o)
  (where (graph (URI. "http://example.org/aGraph") (group :s :p :o) \.)))
```

```sparql
PREFIX  dc: <http://purl.org/dc/elements/1.1/>
PREFIX app: <http://example.org/ns#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

CONSTRUCT { ?s ?p ?o } WHERE
 {
   GRAPH ?g { ?s ?p ?o } .
   ?g dc:publisher <http://www.w3.org/> .
   ?g dc:date ?date .
   FILTER ( app:customDate(?date) > "2005-02-28T00:00:00Z"^^xsd:dateTime ) .
 }
```

```clojure
(query
  (construct :s :p :o)
  (where
    (graph :g (group :s :p :o) \.)
    :g [:dc "publisher"] (URI. "http://www.w3.org/") \.
    :g [:dc "date"] :date \.
    (filter [:app "customDate(?date)"] \> (raw "\"2005-02-28T00:00:00Z\"^^xsd:dateTime")) \.))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX site: <http://example.org/stats#>

CONSTRUCT { [] foaf:name ?name }
WHERE
{ [] foaf:name ?name ;
     site:hits ?hits .
}
ORDER BY desc(?hits)
LIMIT 2
```
Hm, not possible except wit `raw`. (I've never seen this being used 'in the wild'.)
```clojure
(query
  (construct (raw "[]") [:foaf "name"] :name)
  (where (raw "[]") [:foaf "name"] :name
         \; [:site "hits"] :hits \.)
  (order-by-desc :hits)
  (limit 2))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
CONSTRUCT WHERE { ?x foaf:name ?name }
```

```clojure
(query ...) ; Not possible yet, use the form below
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>

CONSTRUCT { ?x foaf:name ?name }
WHERE
{ ?x foaf:name ?name }
```

```clojure
(query
  (construct :x [:foaf "name"] :name)
  (where :x [:foaf "name"] :name))
```

### 16.3 ASK

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
ASK  { ?x foaf:name  "Alice" }
```

```clojure
(query
  (ask :x [:foaf "name"] "Alice"))
```

```sparql
PREFIX foaf:    <http://xmlns.com/foaf/0.1/>
ASK  { ?x foaf:name  "Alice" ;
          foaf:mbox  <mailto:alice@work.example> }
```

```clojure
(query
  (ask :x [:foaf "name"] "Alice"
       \; [:foaf "mbox"] (URI. "mailto:alice@work.example")))
```

### 16.4 DESCRIBE (Informative)

```sparql
DESCRIBE <http://example.org/>
```

```clojure
(query
  (describe (URI. "http://example.org/")))
```

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
DESCRIBE ?x
WHERE    { ?x foaf:mbox <mailto:alice@org> }
```

```clojure
(query
  (describe :x)
  (where :x [:foaf "mbox"] (URI. "mailto:alice@org")))
```

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
DESCRIBE ?x
WHERE    { ?x foaf:name "Alice" }
```

```clojure
(query
  (describe :x)
  (where :x [:foaf "name"] "Alice"))
```

```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
DESCRIBE ?x ?y <http://example.org/>
WHERE    {?x foaf:knows ?y}
```

```clojure
(query
  (describe :x :y (URI. "http://example.org/"))
  (where :x [:foaf "knows"] :y))
```

```sparql
PREFIX ent:  <http://org.example.com/employees#>
DESCRIBE ?x WHERE { ?x ent:employeeId "1234" }
```

```clojure
(query
  (describe :x)
  (where :x [:ent "employeeId"] "1234"))
```

## 17 Expressions and Testing Values

```sparql
PREFIX a:      <http://www.w3.org/2000/10/annotation-ns#>
PREFIX dc:     <http://purl.org/dc/elements/1.1/>
PREFIX xsd:    <http://www.w3.org/2001/XMLSchema#>

SELECT ?annot
WHERE { ?annot  a:annotates  <http://www.w3.org/TR/rdf-sparql-query/> .
        ?annot  dc:date      ?date .
        FILTER ( ?date > "2005-01-01T00:00:00Z"^^xsd:dateTime ) }
```

```clojure
(query
  (select :annot)
  (where :annot [:a "annotates"] (URI. "http://www.w3.org/TR/rdf-sparql-query/") \.
         :annot [:dc "date"] :date \.
         (filter :date \> (raw "\"2005-01-01T00:00:00Z\"^^xsd:dateTime"))))
```

### 17.4 Function Definitions

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
SELECT ?givenName
 WHERE { ?x foaf:givenName  ?givenName .
         OPTIONAL { ?x dc:date ?date } .
         FILTER ( bound(?date) ) }
```

```clojure
(query
  (select :givenName)
  (where :x [:foaf "givenName"] :givenName \.
         (optional :x [:dc "date"] :date) \.
         (filter (bound :date))))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc:   <http://purl.org/dc/elements/1.1/>
SELECT ?name
 WHERE { ?x foaf:givenName  ?name .
         OPTIONAL { ?x dc:date ?date } .
         FILTER (!bound(?date)) }
```

```clojure
(query
  (select :name)
  (where :x [:foaf "givenName"] ::name \.
         (optional :x [:dc "date"] :date) \.
         (filter (!bound :date))))
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name1 ?name2
WHERE { ?x foaf:name  ?name1 ;
        foaf:mbox  ?mbox1 .
        ?y foaf:name  ?name2 ;
        foaf:mbox  ?mbox2 .
        FILTER (?mbox1 = ?mbox2 && ?name1 != ?name2)
      }
```
Not pretty but, possible: (might get rid of the quoting with more macro trixtery)
```clojure
(query
  (select :name1 :name2)
  (where :x [:foaf "name"] :name1
         \; [:foaf "mbox"] :mbox1 \.
         :y [:foaf "name"] :name2
         \; [:foaf "mbox"] :mbox2 \.
         (filter :mbox1 \= :mbox2 '&& :name1 '!= :name2)))
```

```sparql
PREFIX a:      <http://www.w3.org/2000/10/annotation-ns#>
PREFIX dc:     <http://purl.org/dc/elements/1.1/>
PREFIX xsd:    <http://www.w3.org/2001/XMLSchema#>

SELECT ?annotates
WHERE { ?annot  a:annotates  ?annotates .
        ?annot  dc:date      ?date .
        FILTER ( ?date = xsd:dateTime("2005-01-01T00:00:00Z") )
      }
```

```clojure
(query ...) ; TODO dateTime
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name1 ?name2
WHERE { ?x foaf:name  ?name1 ;
        foaf:mbox  ?mbox1 .
         ?y foaf:name  ?name2 ;
         foaf:mbox  ?mbox2 .
         FILTER (sameTerm(?mbox1, ?mbox2) && !sameTerm(?name1, ?name2))
      }
```

```clojure
  (query
  (select :name1 :name2)
  (where :x [:foaf "name"] :name1
         \; [:foaf "mbox"] :mbox1 \.
         :y [:foaf "name"] :name2
         \; [:foaf "mbox"] :mbox2 \.
         (filter (same-term :mbox1, :mbox2) '&& (!same-term :name1 :name2))))
```

```sparql
PREFIX  :      <http://example.org/WMterms#>

SELECT ?aLabel1 ?bLabel
WHERE { ?a  :label        ?aLabel .
        ?a  :weight       ?aWeight .
        ?a  :displacement ?aDisp .

        ?b  :label        ?bLabel .
        ?b  :weight       ?bWeight .
        ?b  :displacement ?bDisp .

        FILTER ( sameTerm(?aWeight, ?bWeight) && !sameTerm(?aDisp, ?bDisp)) }
```
TODO find a way to handle `&&`
```clojure
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
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
 WHERE { ?x foaf:name  ?name ;
            foaf:mbox  ?mbox .
         FILTER isIRI(?mbox) }
```

```clojure
(query ...)
```

```sparql
PREFIX a:      <http://www.w3.org/2000/10/annotation-ns#>
PREFIX dc:     <http://purl.org/dc/elements/1.1/>
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>

SELECT ?given ?family
WHERE { ?annot  a:annotates  <http://www.w3.org/TR/rdf-sparql-query/> .
  ?annot  dc:creator   ?c .
  OPTIONAL { ?c  foaf:given   ?given ; foaf:family  ?family } .
  FILTER isBlank(?c)
}
```

```clojure
(query ...)
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE { ?x foaf:name  ?name ;
        foaf:mbox  ?mbox .
        FILTER isLiteral(?mbox) }
```

```clojure
(query ...)
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
 WHERE { ?x foaf:name  ?name ;
            foaf:mbox  ?mbox .
         FILTER regex(str(?mbox), "@work\\.example$") }

```

```clojure
(query ...)
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
 WHERE { ?x foaf:name  ?name ;
            foaf:mbox  ?mbox .
         FILTER ( lang(?name) = "es" ) }
```

```clojure
(query ...)
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>
PREFIX eg:   <http://biometrics.example/ns#>
SELECT ?name ?shoeSize
 WHERE { ?x foaf:name  ?name ; eg:shoeSize  ?shoeSize .
         FILTER ( datatype(?shoeSize) = xsd:integer ) }
```

```clojure
(query ...)
```


```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
SELECT ?title
 WHERE { ?x dc:title  "That Seventies Show"@en ;
            dc:title  ?title .
         FILTER langMatches( lang(?title), "FR" ) }
```

```clojure
(query ...)
```

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
SELECT ?title
 WHERE { ?x dc:title  ?title .
         FILTER langMatches( lang(?title), "*" ) }
```

```clojure
(query ...)
```

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name
 WHERE { ?x foaf:name  ?name
         FILTER regex(?name, "^ali", "i") }
```

```clojure
(query ...)
```

### 17.6 Extensible Value Testing

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX func: <http://example.org/functions#>
SELECT ?name ?id
WHERE { ?x foaf:name  ?name ;
           func:empId   ?id .
        FILTER (func:even(?id)) }
```

```clojure
(query ...)
```

```sparql
PREFIX aGeo: <http://example.org/geo#>

SELECT ?neighbor
WHERE { ?a aGeo:placeName "Grenoble" .
        ?a aGeo:locationX ?axLoc .
        ?a aGeo:locationY ?ayLoc .

        ?b aGeo:placeName ?neighbor .
        ?b aGeo:locationX ?bxLoc .
        ?b aGeo:locationY ?byLoc .

        FILTER ( aGeo:distance(?axLoc, ?ayLoc, ?bxLoc, ?byLoc) < 10 ) .
      }
```

```clojure
(query ...)
```

## 18 Definition of SPARQL

```
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT (SUM(?val) AS ?sum) (COUNT(?a) AS ?count)
WHERE {
  ?a rdf:value ?val .
} GROUP BY ?a
```

```clojure
(query ...)
```

## Thats all, folks!


