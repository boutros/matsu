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
       ["une pipe" :fr]            "\"une pipe\"@fr"
       true                        true
       false                       false
       (URI. "http://dbpedia.org") "<http://dbpedia.org>"
       [:foaf "mbox"]              "foaf:mbox"
       [:keyword]                  "<keyword>"
       [[]]                        "[]"))