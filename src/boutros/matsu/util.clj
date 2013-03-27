(ns boutros.matsu.util
  (:require [clojure.string :as string]))

;; ----------------------------------------------------------------------------
;; Pretty-printer
;; ----------------------------------------------------------------------------
;; A rather crude regex-based implementation. This is far from perfect, but
;; still usefull when debugging long and complex SPARQl queries.
;;
;; Ideally, I'll implement a proper SPARQL lexer/parser based on the complete
;; grammar described here:
;; http://www.w3.org/TR/sparql11-query/#sparqlGrammar

(def before2 #{"SELECT" "INSERT" "DELETE" "WHERE"})
(def before #{"BASE" "PREFIX" "FROM" "OPTIONAL" "WITH" "MODIFY"})
(def after #{ ";" "." })
(def both #{ "{" "}" })

(defn insert-newlines
  "Insert newline before and after certain SPARQL keywords and syntatic signifiers."
  [s]
  (string/join " "
    (for [elem (re-seq #"[^\s\"']+|\"([^\"]*)\"|'([^']*)'" s) ;")
                        :let [e (first elem)]]
       (cond
         (contains? before2 e) (str "\n \n" e)
         (contains? before e) (str "\n" e)
         (contains? after e) (str e "\n")
         (contains? both e) (str "\n" e "\n")
         :else e))))

(defn insert-indentation
  "Indents lines following brackets and lines following semicolon (same subject)"
  [s]
  (string/join "\n"
    (loop [lines (string/split-lines s) result [] indent "" same-subject ""]
      (let [line (first lines)
            add-indent (if (re-matches #"\{" (str line))
                         (str indent "   ")
                         false)
            remove-indent (if (re-matches #"\}" (str line))
                            (apply str (drop 3 indent))
                            false)
            semicolon (if (re-find #";" (str line))
                        (apply str
                               (repeat
                                 (inc
                                   (clojure.core/count
                                     (re-find #"[^\s]*" (str line))))
                                 " "))
                        "")
            indentstring (if remove-indent remove-indent indent)]
        (if (empty? lines)
          result
        (recur (rest lines)
               (conj result (str same-subject indentstring line))
               (cond
                 add-indent add-indent
                 remove-indent remove-indent
                 :else indent)
               (if semicolon semicolon "")))))))

(defn pprint
  "A very crude regex-based SPARQL pretty-printer."
  [s]
  (-> s
      insert-newlines
      (string/replace #"\n\s" "\n")
      (string/replace #"\s\n" "\n")
      insert-indentation
      string/trim))