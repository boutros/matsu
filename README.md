# matsu

A Clojure SPARQL query constructor

## Status

Everything is ALPHA and subject to change. Do not use yet!

## Installation

Not yet available on clojars.

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

The aim of Matsu is to cover the full SPARQL 1.1 specification. But no doubt there will be edge cases where Matsu falls short. In such cases you can always insert a raw string into your query with `raw`:

    EXAMPLE

Remember that quotation marks must be escaped. Characters as well as quoted symbols will also render unchanged:

    EXAMPLE

See the tests for more examples on query syntax.

+ SPARQL 1.1 spec translation in /doc/sparql_spec.md

+ Complete working example querying dbpedia in docs/example.clj

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
