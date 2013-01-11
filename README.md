# matsu

A Clojure SPARQL query constructor

## Status

Everything is ALPHA and subject to change. That said, I'm begining to become reasonably satisfied with the DSL syntax as it is now. What bugs me is that dots and semicolns have to be written as characters or quoted.

## Installation

Soon available on clojars.

## Usage

Matsu is a DSL for constructing SPARQL queries:

```clojure
(query
  (select :person)
  (where :person a [:foaf "Person"]
         \; [:foaf "mbox"] (URI. "mailto:me@me.com") \.))
```

Which would yield the following string:

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?person
WHERE
  {
    ?person a foaf:Person
    ; foaf:mbox <mailto:me@me.com> .
  }
```

Althught without newlines or indentation. A pretty-printer might be added in the future.

The prefixes are automatically infered provided that they exists in the global `prefixes` map. An exception will be thrown if the prefix cannot be resolved.

You can also supply query-local prefixes which will override the global `prefixes`:

```clojure
(query-with-prefixes {:foaf "<mylocalfoaf>"}
  (select :person)
  (where :person [:foaf "name"] "Petter"))
```
```sparql
PREFIX foaf: <mylocalfoaf>
SELECT ?person WHERE { ?s foaf:name "Petter" }
```

Matsu makes it possible to create complex, nested queries:

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
                  :ppd a [:foaf "PersonalProfileDocument"] \.)
           \.)
    (graph :ppd
           (group :w [:foaf "mbox"] :mbox
                  \; [:foaf "nick"] :nick))))
```

Yielding the following SPARQL string:

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

You can bind queries to vars with `defquery` and use them as basis for other queries:

```clojure
(defquery q1
  (select *)
  (where :s :p :o))

(query q1
   (limit 5))
```

```sparql
SELECT * WHERE { ?s ?p ?o } LIMIT 5
```

While the aim of matsu is to cover the full SPARQL 1.1 specification, there will no doubt be cases where it falls short. In such cases you can always insert a raw string into your query with `raw`:

```clojure
(query
  (select :title :price)
  (where (group :x [:ns "price"] :p \.
                :x [:ns "discount"] :discount
                (bind [(raw "?p*(1-?discount)") :price]))
         (group :x [:dc "title"] :title \.)
         (filter :price < 20)))
```

Yielding the following SPARQL string:

```sparql
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX ns: <http://example.org/ns#>

SELECT ?title ?price
WHERE {
        { ?x ns:price ?p .
           ?x ns:discount ?discount
           BIND(?p*(1-?discount) AS ?price)
        }
        { ?x dc:title ?title . }
        FILTER(?price < 20)
      }
```

See the tests for more examples on query syntax.

+ SPARQL 1.1 spec translation in [/doc/sparql_spec.md](https://github.com/boutros/matsu/blob/master/doc/sparql_spec.md)

+ See doc/example.clj for a omplete working example querying the remote dbedia SPARQL endpoint

## Limitations
* Single colon keyword prefix is not possible, use the equivalent `BASE`-form instead
* Dollar-prefixed variables not supported

There might be other limitations, especially when dealing with SPARQL expressions. But most, if not all of the limitations can be circumvented by interpolating raw strings into the query with the `raw` function.

## Todos
* Syntacic sugar (macros)
* SPARQL update
* Datetime encoding, java to ^^xsd:dateTime
* Subqueries
* Property path syntax
* Specifying variables with `VALUES` in data block
* Federated queries (SERVICE)
* CONSTRUCT short form (CONSTRUCT WHERE { ... })

## Contribute

By all means! I'm open for discussing any ideas you might have.

## License

Copyright © 2012 Petter Goksøyr Åsen

Distributed under the Eclipse Public License, the same as Clojure.
