(ns boutros.matsu.compiler-test
  (:require [boutros.matsu.compiler :refer [encode]]
            [clojure.test :refer :all]
            [clj-time.core :refer [date-time]])
  (:import (java.net URI)))

(deftest fn-encode
  (are [a b] (= (encode a) b)
       \*                          \*
       :keyword                    "?keyword"
       23                          23
       9.9                         9.9
       "string"                    "\"string\""
       "string with \"quotes\""    "\"string with \\\"quotes\\\"\""
       ["une pipe" :fr]            "\"une pipe\"@fr"
       ["quotes \"and\" lang" :en] "\"quotes \\\"and\\\" lang\"@en"
       true                        true
       false                       false
       (URI. "http://dbpedia.org") "<http://dbpedia.org>"
       [:foaf :mbox]               "foaf:mbox"
       ["42" "somedatatype"]        "\"42\"^^somedatatype"
       [:keyword]                  "<keyword>"
       [[]]                        "[]"
       (range 2)                   '(0 \space 1)
       (date-time 1981 9 8)        "\"1981-09-08T00:00:00.000Z\"^^xsd:dateTime"
       (java.util.Date. 1)         "\"1970-01-01T00:00:00.001Z\"^^xsd:dateTime"))