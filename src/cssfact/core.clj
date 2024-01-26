(ns cssfact.core
  (:require [clojure.tools.cli :as cli]
            [cssfact.css :as css]
            [cssfact.matrix :as matrix]))

(defn compress [{:keys [input output num-rules added removed]}]
  (println "Reading input...")
  (let [input-text (slurp input)
        mcss (css/css->matrix (css/parse input-text))]
    (printf "%s selectors, %s declarations, %s style rules, %s non-style rules, %s elementary rules.\n"
            (count (:selectors mcss))
            (count (:declarations mcss))
            (:original-rules-count mcss)
            (count (:other-rules mcss))
            (count (get-in mcss [:matrix :ones])))
    (println "Decomposing matrix...")
    (let [decomp (matrix/decompose (:matrix mcss) num-rules)
          {:keys [residue num-rules num-added num-removed]} (matrix/residue mcss decomp)]
      (printf "%s elementary rules in output, %s removed, %s added\n" num-rules num-removed num-added)
      (println "Saving residual files...")
      (spit added (css/residual-css mcss residue pos?))
      (spit removed (css/residual-css mcss residue neg?))
      (println "Saving output...")
      (let [out-css (css/reconstruct-css mcss decomp)]
        (spit output out-css)
        (printf "%s saved, %s -> %s characters." output (count input-text) (count out-css))))))

(def cli-options
  [["-i" "--input CSSFILE" "Input file"]
   ["-o" "--output CSSFILE" "Output file" :default "out.css"]
   ["-n" "--num-rules N" "Number of rules" :parse-fn #(Long/parseLong %) :validate [pos?] :default 10]
   ["-a" "--added CSSFILE" "Residual file of added declarations" :default "added.css"]
   ["-r" "--removed CSSFILE" "Residual file of removed declarations" :default "removed.css"]])

(defn -main [& args]
  (let [{:keys [options summary]} (cli/parse-opts args cli-options)]
    (if (:input options)
      (compress options)
      (printf "Usage: clojure -M -m cssfact.core [OPTIONS]\n\n%s\n" summary))
    (shutdown-agents)))
