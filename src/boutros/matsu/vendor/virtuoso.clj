(ns boutros.matsu.vendor.virtuoso
  "Non-standard SPARQL-functions and extensions introduced by Openlink's
  Virtuoso.

  Some info on the sql: and bif: namespaced functions here:
  http://docs.openlinksw.com/virtuoso/rdfsparql.html#rdfsqlfromsparql")

(defn modify
  "Virtuoso-specific equivalent of WITH."
  [q uri]
  (assoc q :with {:tag "MODIFY" :bounds [" "] :sep " " :content [uri]}))