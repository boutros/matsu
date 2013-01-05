#SPARQL 1.1 specification

An attempt to translate all the example queries from the [SPARQL 1.1 specification from W3C](http://www.w3.org/TR/sparql11-query/) into Matsu syntax. This document corresponds with the tests in `/test/boutros/matsu/sparql_spec.clj`.

The following namespaces are assumed to be registered:
```clojure
@PREFIXES
{:foaf    "<http://xmlns.com/foaf/0.1/>"
 :org     "<http://example.com/ns#>"
 :dc      "<http://purl.org/dc/elements/1.1/>"
 :ns      "<http://example.org/ns#>"}
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
  (select \( (concat :G " " :S) 'AS :name \) )
  (where :P [:foaf "givenName"] :G
         \; [:foaf "surname"] :S))
```
*NOT* happy about this syntax, must think about how to handle SPARQL expressions.

Maybe `(select [(concat :G " " :S) :name])` if vector is `[(list?) keyword?]`


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
    (where :s [:ns "price"] :price \.
           (filter :price \< 30.5)
           :x [:dc "title"] :title \.))
```

## SPARQL Syntax

### 4.2 Syntax for Triple Patterns