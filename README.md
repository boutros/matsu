# matsu

A Clojure SPARQL query constructor

## Status

I'm using matsu in my own applications, but the DSL syntax might still change somewhat. Any feedback appreciated.

## Installation
Add the following to your project.clj:
```clojure
[matsu "0.1.0"]          ; stable
[matsu "0.1.1-SNAPSHOT"] ; dev
```
## Usage

Matsu is a DSL for constructing SPARQL queries:

```clojure
(query
  (select :person)
  (where :person a [:foaf :Person] \;
         [:foaf :mbox] (URI. "mailto:me@me.com") \.))
```

Which would yield the following string:

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?person
WHERE
  {
     ?person a foaf:Person ;
             foaf:mbox <mailto:me@me.com> .
  }
```

`query` by default outputs a string without newlines or indentation, but there is a pretty-printer provided in the `util` namespace which aims to produce a formatted query like the one seen above. While far from perfect, I find it very helpfull when debugging long and complex queries.

The prefixes are automatically infered provided that they exists in the global `prefixes` map. An exception will be thrown if the prefix cannot be resolved. You add prefixes using `register-namespaces`:
```clojure
(register-namespaces {:foaf    "<http://xmlns.com/foaf/0.1/>"
                      :rdfs    "<http://www.w3.org/2000/01/rdf-schema#>"})
```

You can also supply query-local prefixes which will override the global `prefixes`:

```clojure
(query-with-prefixes {:foaf "<mylocalfoaf>"}
  (select :person)
  (where :person [:foaf :name] "Petter"))
```
```sparql
PREFIX foaf: <mylocalfoaf>
SELECT ?person WHERE { ?person foaf:name "Petter" }
```

Matsu makes it possible to create complex, nested queries:

```clojure
(query
  (select :mbox :nick :ppd)
  (from-named (URI. "http://example.org/foaf/aliceFoaf")
              (URI. "http://example.org/foaf/bobFoaf"))
  (where
    (graph [:data :aliceFoaf]
           :alice [:foaf :mbox] (URI. "mailto:alice@work.example")
           \; [:foaf :knows] :whom \.
           :whom [:foaf :mbox] :mbox
           \; [:rdfs :seeAlso] :ppd \.
           :ppd a [:foaf :PersonalProfileDocument] \.) \.
    (graph :ppd
           :w [:foaf :mbox] :mbox
           \; [:foaf :nick] :nick)))
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

### Working with saved queries
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

### SPARQL Update
`INSERT` and `DELETE` supported:

```clojure
(query
  (with (URI. "http://example/addresses"))
  (delete :person [:foaf :givenName] "Bill")
  (insert :person [:foaf :givenName] "William")
  (where :person [:foaf :givenName] "Bill"))
```

```sparql
PREFIX foaf:  <http://xmlns.com/foaf/0.1/>

WITH <http://example/addresses>
DELETE { ?person foaf:givenName 'Bill' }
INSERT { ?person foaf:givenName 'William' }
WHERE
  { ?person foaf:givenName 'Bill'
  }
```

### Interpolating raw strings in the query
While the aim of matsu is to cover the full SPARQL 1.1 specification, there will no doubt be cases where it falls short. In such cases you can always insert a raw string into your query with `raw`:

```clojure
(query
  (select :title :price)
  (where (group :x [:ns :price] :p \.
                :x [:ns :discount] :discount
                (bind [(raw "?p*(1-?discount)") :price]))
         (group :x [:dc :title] :title \.)
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

## More examples
See the [doc](https://github.com/boutros/matsu/blob/master/doc) directory for more examples:
+ [/doc/example.clj](https://github.com/boutros/matsu/blob/master/doc/example.clj) - an example REPL session using matsu to query the DBpedia SPARQL endpoint.
+ [/doc/sparql_spec.md](https://github.com/boutros/matsu/blob/master/doc/sparql_spec.md) - The example queries from the WC3 SPARQL 1.1 specification
+ [/doc/update_spec.md](https://github.com/boutros/matsu/blob/master/doc/update_spec.md) - The example queries from the WC3 SPARQL 1.1 Update specification

## Limitations
* Single colon keyword prefix is not possible, use the equivalent `BASE`-form instead
* Dollar-prefixed variables not supported

There might be other limitations, especially when dealing with SPARQL expressions. But most, if not all of the limitations can be circumvented by interpolating raw strings into the query with the `raw` function.

## Todos
* Better BIND syntax
* Subqueries
* Property path syntax
* Specifying variables with `VALUES` in data block
* Federated queries (SERVICE)
* Syntacic sugar (macros)
* Queries made with `defquery` should be able to take parameters

## Contribute

By all means! I'm open for discussing any ideas you might have.

## License

Copyright © 2012 Petter Goksøyr Åsen

Distributed under the Eclipse Public License, the same as Clojure.
