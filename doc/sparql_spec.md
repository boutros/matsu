#SPARQL 1.1 specification

An attempt to translate all the example queries from the [SPARQL 1.1 specification from W3C](http://www.w3.org/TR/sparql11-query/) into Matsu syntax. This document corresponds with the tests in `/test/boutros/matsu/sparql_spec.clj`.

The following namespaces are assumed to be present in the `*PREFIXES*` map:
```
{:foaf "<>" etc}
```

## 2 Making Simple Queries (Informative)

### 2.1 Writing a Simple Query

####SPARQL
```sparql
SELECT ?title
WHERE
{
  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .
}
```

####CLOJURE
```clojure
(query
  (select :title)
  (where (URI. "http://example.org/book/book1") (URI. "http://example.org/book/book1") :title) \.))
```

### 2.2 Multiple Matches

####SPARQL
```sparql
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE
  { ?x foaf:name ?name .
    ?x foaf:mbox ?mbox }
```

####CLOJURE
```clojure
(query
  (prefix :foaf)
  (select :name :mbox)
  (where :x [:foaf "name"] :name \.
         :x [:foaf "mbox"] :mbox))
```

### 2.3 Matching RDF Literals


####SPARQL
```sparql
SELECT ?v WHERE { ?v ?p "cat" }
```

####CLOJURE
```clojure
(query
  (select :v)
  (where :v :p "cat"))
```

####SPARQL
```sparql
SELECT ?v WHERE { ?v ?p "cat"@en }

```

####CLOJURE
```clojure
(query
  (select :v)
  (where :v :p ["cat" :en]))
```

####SPARQL
```sparql
SELECT ?v WHERE { ?v ?p 42 }

```

####CLOJURE
```
(query ...)
```

####SPARQL
```sparql
SELECT ?v WHERE { ?v ?p "abc"^^<http://example.org/datatype#specialDatatype> }

```

####CLOJURE
```
(query ...)
```

### 2.4 Blank Node Labels in Query Results


### 2.5 Creating Values with Expressions


### 2.6 Building RDF Graphs

## 3 RDF Term Constraints (Informative)