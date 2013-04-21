### 0.1.3-SNAPSHOT
* fix: doubled quotes wasn't escaped if string followed by language tag
* fix: don't infer namespace prefixes from inside quoted strings in the query

### 0.1.2
* feature: queries made with defquery can take arguments
* feature: compiler encodes from java.util.Date and org.joda.time.DateTime
           formats to xsd:dateTime
* feature: pretty-printer
* feature: compiler encodes lazy-seqs
* fix: ensure proper escaping of double quotes in strings

### 0.1.0 Initial release