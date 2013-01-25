(ns boutros.matsu.core-test
  (:require [boutros.matsu.core :refer [register-namespaces]]
            [boutros.matsu.sparql :refer [query select where]]
            [clojure.test :refer :all]))

(deftest missing-namespace
  (is (thrown? IllegalArgumentException
               (query
                 (select *)
                 (where [:missing :dada] :p :o)))))