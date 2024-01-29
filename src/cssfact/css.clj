(ns cssfact.css
  (:require [clojure.core.matrix :as matrix]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io StringReader]
           [org.w3c.dom.css CSSStyleRule]
           [org.w3c.css.sac InputSource]
           [com.steadystate.css.dom Property]
           [com.steadystate.css.parser CSSOMParser SACParserCSS3]))

(defn parse [css-string]
  (let [parser (CSSOMParser. (SACParserCSS3.))
        input-source (InputSource. (StringReader. css-string))]
    (.parseStyleSheet parser input-source nil nil)))

(defn style-rule->edn [rule]
  {:selectors (str/split (.getSelectorText rule) #",\s*")
   :declarations (mapv str (.getProperties (.getStyle rule)))})

(defn style-rule? [rule]
  (instance? CSSStyleRule rule))

(defn css->matrix [css]
  (let [rules (-> css .getCssRules .getRules)
        style-rules (map style-rule->edn (filter style-rule? rules))
        other-rules (remove style-rule? rules)
        all-selectors (vec (sort (distinct (mapcat :selectors style-rules))))
        all-declarations (vec (sort (distinct (mapcat :declarations style-rules))))]
    {:original-rules-count (count style-rules)
     :other-rules other-rules
     :selectors all-selectors
     :declarations all-declarations
     :matrix {:shape [(count all-selectors) (count all-declarations)]
              :ones (vec (for [{:keys [selectors declarations]} style-rules
                               selector selectors
                               declaration declarations]
                           [(.indexOf all-selectors selector)
                            (.indexOf all-declarations declaration)]))}}))

(defn reconstruct-rule [{:keys [selectors declarations]} [sm bm] i]
  (let [sels (matrix/non-zero-indices (matrix/get-column sm i))
        decls (matrix/non-zero-indices (matrix/get-row bm i))]
    (when (seq decls)
      (str
       (str/join ", " (map selectors sels))
       " {\n"
       (str/join "\n" (map #(str "  " (declarations %) ";") decls))
       "\n}\n"))))

(defn reconstruct-css [mcss decomp]
  (let [[_ n] (matrix/shape (first decomp))]
    (apply str
           (str/join "\n" (:other-rules mcss))
           "\n"
           (map (partial reconstruct-rule mcss decomp) (range n)))))

(defn residual-css [{:keys [selectors declarations]} residue filter-fn]
  (apply str
         (for [[x slice] (map-indexed vector (matrix/non-zero-indices residue)) y slice
               :when (filter-fn (matrix/mget residue x y))]
           (format "%s { %s; }\n" (selectors x) (declarations y)))))
