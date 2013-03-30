(ns boutros.matsu.vendor.virtuoso
  "Non-standard SPARQL-functions and extensions introduced by Openlink's
  Virtuoso.

  Some info on the sql: and bif: namespaced functions here:
  http://docs.openlinksw.com/virtuoso/rdfsparql.html#rdfsqlfromsparql"
  (:require [boutros.matsu.compiler :refer [encode]]))

(defn modify
  "Virtuoso-specific equivalent of WITH."
  [q uri]
  (assoc q :with {:tag "MODIFY" :bounds [" "] :sep " " :content [uri]}))

(defn delete-from [q g & more]
  "Older SPARUL syntax: DELETE FROM <graph> { patterns... }"
  (assoc q :delete {:tag (str "DELETE FROM " (encode g)) :bounds [" { " " } "]
                    :sep " " :content (vec more)}))

(defn insert-into [q g & more]
  "Older SPARUL syntax: INSERT INTO <graph> { patterns... }"
  (assoc q :insert {:tag (str "INSERT INTO " (encode g)) :bounds [" { " " } "]
                    :sep " " :content (vec more)}))
