(defproject matsu "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[midje "1.4.0"]
                                  [com.stuartsierra/lazytest "1.2.3"]]}}
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :main matsu.sparql)
