(ns boutros.matsu.compiler-test
  (:require [boutros.matsu.compiler :refer [encode]]
            [clojure.test :refer :all])
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
       true                        true
       false                       false
       (URI. "http://dbpedia.org") "<http://dbpedia.org>"
       [:foaf :mbox]               "foaf:mbox"
       ["42" "somedatatype"]        "\"42\"^^somedatatype"
       [:keyword]                  "<keyword>"
       [[]]                        "[]"
       (range 2)                   '(0 \space 1)))