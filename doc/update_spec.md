#SPARQL 1.1 Update

An attempt to translate all the example queries from the [SPARQL 1.1  UPDATEspecification from W3C](http://www.w3.org/TR/sparql11-update/) into Matsu syntax. This document corresponds with the tests in `/test/boutros/matsu/w3c_update_test.clj`.

### Example 1

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT DATA
{
  <http://example/book1> dc:title "A new book" ;
                         dc:creator "A.N.Other" .
}
```

```clojure
(query
  (insert-data
    (URI. "http://example/book1") [:dc :title] "A new book"
    \; [:dc :creator] "A.N.Other" \.))
```

### Example 2

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX ns: <http://example.org/ns#>
INSERT DATA
{ GRAPH <http://example/bookStore> { <http://example/book1>  ns:price  42 } }
```

```clojure
(query
  (insert-data
    (graph (URI. "http://example/bookStore")
           (group (URI. "http://example/book1") [:ns :price] 42))))
```

### Example 3

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE DATA
{
  <http://example/book2> dc:title "David Copperfield" ;
                         dc:creator "Edmund Wells" .
}
```

```clojure
(query
  (delete-data
    (URI. "http://example/book2") [:dc :title] "David Copperfield"
          \; [:dc :creator] "Edmund Wells" \.))
```

### Example 4

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
DELETE DATA
{ GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  "Fundamentals of Compiler Desing" } } ;

PREFIX dc: <http://purl.org/dc/elements/1.1/>
INSERT DATA
{ GRAPH <http://example/bookStore> { <http://example/book1>  dc:title  "Fundamentals of Compiler Design" } }
```

```clojure
(query) ; TBD
```

### Example 5

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>

WITH <http://example/addresses>
DELETE { ?person foaf:givenName 'Bill' }
INSERT { ?person foaf:givenName 'William' }
WHERE
  { ?person foaf:givenName 'Bill'
  }
```

```clojure
(query
  (with (URI. "http://example/addresses"))
  (delete :person [:foaf :givenName] "Bill")
  (insert :person [:foaf :givenName] "William")
  (where :person [:foaf :givenName] "Bill"))
```
### Example 6

```sparql
PREFIX dc:  <http://purl.org/dc/elements/1.1/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

DELETE
 { ?book ?p ?v }
WHERE
 { ?book dc:date ?date .
   FILTER ( ?date > "1970-01-01T00:00:00-02:00"^^xsd:dateTime )
   ?book ?p ?v
 }
```

```clojure
(query
  (delete :book :p :v)
  (where :book [:dc :date] :date \.
         (filter :date > ["1970-01-01T00:00:00-02:00" "xsd:dateTime"])
         :book :p :v))
```

### Example 7

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>

WITH <http://example/addresses>
DELETE { ?person ?property ?value }
WHERE { ?person ?property ?value ; foaf:givenName 'Fred' }

```

```clojure
(query
  (with (URI. "http://example/addresses"))
  (delete :person :property :value)
  (where :person :property :value
         \; [:foaf :givenName] "Fred"))
```

### Example 8

```sparql
PREFIX dc:  <http://purl.org/dc/elements/1.1/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

INSERT
  { GRAPH <http://example/bookStore2> { ?book ?p ?v } }
WHERE
  { GRAPH  <http://example/bookStore>
       { ?book dc:date ?date .
         FILTER ( ?date > "1970-01-01T00:00:00-02:00"^^xsd:dateTime )
         ?book ?p ?v
  } }
```

```clojure
(query)
```
### Example 9

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

INSERT
  { GRAPH <http://example/addresses>
    {
      ?person  foaf:name  ?name .
      ?person  foaf:mbox  ?email
    } }
WHERE
  { GRAPH  <http://example/people>
    {
      ?person  foaf:name  ?name .
      OPTIONAL { ?person  foaf:mbox  ?email }
    } }
```

```clojure
(query)
```
### Example 10

```sparql
PREFIX dc:  <http://purl.org/dc/elements/1.1/>
PREFIX dcmitype: <http://purl.org/dc/dcmitype/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

INSERT
  { GRAPH <http://example/bookStore2> { ?book ?p ?v } }
WHERE
  { GRAPH  <http://example/bookStore>
     { ?book dc:date ?date .
       FILTER ( ?date < "2000-01-01T00:00:00-02:00"^^xsd:dateTime )
       ?book ?p ?v
     }
  } ;

WITH <http://example/bookStore>
DELETE
 { ?book ?p ?v }
WHERE
 { ?book dc:date ?date ;
         dc:type dcmitype:PhysicalObject .
   FILTER ( ?date < "2000-01-01T00:00:00-02:00"^^xsd:dateTime )
   ?book ?p ?v
 }
```

```clojure
(query)
```
### Example 11

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>

DELETE WHERE { ?person foaf:givenName 'Fred';
                       ?property      ?value }
```

```clojure
(query)
```

### Example 12

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>

DELETE WHERE {
  GRAPH <http://example.com/names> {
    ?person foaf:givenName 'Fred' ;
            ?property1 ?value1
  }
  GRAPH <http://example.com/addresses> {
    ?person ?property2 ?value2
  }
}
```
