# matsu

A Clojure SPARQL query constructor

## Status

Everything is ALPHA and subject to change. Do not use yet!

## Installation

Soon available on clojars.

## Usage

Matsu is a DSL for constructing SPARQL queries:

```clojure
(query
  (select :person)
  (where :person \a [:foaf "Person"]
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

The prefixes are automatically infered provided that they exists in the global `PREFIXES` map. An exception will be thrown if the prefix cannot be resolved.

You can also supply prefixes with the query, which will override the global `prefixes`:

```clojure
(query
  (with-prefixes {:foaf "http://blblbl"}
    (select :person)
    (where :person \a [:foaf "Person"]
           \; [:foaf "mbox"] (URI. "mailto:me@me.com") \.)))
```

Matsu makes it easy to construct complex queries:

    EXAMPLE

You can bind queries to vars with `defquery` and use them as basis for other queries:

    EXAMPLE

While the aim of matsu is to cover the full SPARQL 1.1 specification, there will no doubt be cases where it falls short. In such cases you can always insert a raw string into your query with `raw`:

```clojure
(query
  (select :title :price)
  (where (group :x [:ns "price"] :p \.
                :x [:ns "discount"] :discount
                (bind [(raw "?p*(1-?discount)") :price]))
         (group :x [:dc "title"] :title \.)
         (filter :price \< 20)))
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

Remember that quotation marks must be escaped. Characters as well as quoted symbols will also render unchanged:

    EXAMPLE

See the tests for more examples on query syntax.

+ SPARQL 1.1 spec translation in [/doc/sparql_spec.md](https://github.com/boutros/matsu/blob/master/doc/sparql_spec.md)

+ See doc/example.clj for a omplete working example querying the remote dbedia SPARQL endpoint

## Limitations
* The `WHERE` keyword is not optional, like it is in SPARQL
* Single colon keyword prefix is not possible, use the equivalent `BASE`-form instead
* Dollar-prefixed variables not supported
* Subqueries not supported yet
* Property path syntax not supported yet
* Specifying variables with `VALUES` in data block not possible yet

There might be other limitations, especially with complex, nested queries. But most, if not all of the limitations can be circumvented by interpolating raw strings into the query with the `raw` function.

## Todos
* Sparql update

## Interals

The query is represented by a map

dsl functions, takes a map and returns a map

compiler-functions - compiles into []
-> into string


## Contribute

By all means!

## License

Copyright © 2012 Petter Goksøyr Åsen

Distributed under the Eclipse Public License, the same as Clojure.
