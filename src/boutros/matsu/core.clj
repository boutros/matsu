(ns boutros.matsu.core
  "Matsu core datastructures")


;; ----------------------------------------------------------------------------
;; Datastructures and vars
;: ----------------------------------------------------------------------------

(defn empty-query []
  "Query-map constructor

  Each key (except :local-prefixes) can be populated with a map specifying the
  tag, bounds, content and separator of the query part.

    ex: {:tag nil :content [:x :y] :bounds ["" ""] :sep " "}

  The :content field can contain other such maps, so the datastructure
  can be arbitrarily deeply nested."
  {:local-prefixes {}
   :with nil
   :delete nil
   :insert nil
   :base nil
   :from nil
   :from-named nil
   :query-form nil
   :where nil
   :order-by nil
   :group-by nil
   :having nil
   :offset nil
   :limit nil})

;; a global map of the current registered prefixes
(def prefixes (atom {:xsd "<http://www.w3.org/2001/XMLSchema#>"}))


;; ----------------------------------------------------------------------------
;; Namespace functions
;; ----------------------------------------------------------------------------

(defn register-namespaces [m]
  (swap! prefixes merge m))

(defn ns-or-error
  "Resolves prefixes. Throws an error if the namespace cannot be resolved."
  [k m]
  (if-let [v (k m)] ; check for query-local prefixes first
    v
    (if-let [v (k @prefixes)]
      v
     (throw (IllegalArgumentException. (str "Cannot resolve namespace: " k))))))