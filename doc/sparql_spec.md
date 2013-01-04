#SPARQL 1.1 spesification

An attempt to translate all the example queries from the [SPARQL 1.1 W3C Recommendation](http://www.w3.org/TR/sparql11-query/) to Matsu syntax. This document corresponds to the tests in `/test/boutros/matsu/sparql_spec.clj`.

The following namespaces are assumed to be present in *PREFIXES*:
```
{:foaf "<>" etc}
```

## 2 Making Simple Queries (Informative)

### 2.1 Writing a Simple Query

####SPARQL
```
SELECT ?title
WHERE
{
  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .
}
```

####CLOJURE
```
(query
  (select :title)
  (where (URI. "http://example.org/book/book1") (URI. "http://example.org/book/book1") :title) \.))
```

### 2.2 Multiple Matches

####SPARQL
```
PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
SELECT ?name ?mbox
WHERE
  { ?x foaf:name ?name .
    ?x foaf:mbox ?mbox }
```

####CLOJURE
```
(query
  (prefix :foaf)
  (select :name :mbox)
  (where :x [:foaf "name"] :name \.
         :x [:foaf "mbox"] :mbox))
```
