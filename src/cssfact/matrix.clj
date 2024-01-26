(ns cssfact.matrix
  (:require [cheshire.core :as json]
            [clojure.core.matrix :as matrix]
            [clojure.core.matrix.operators :as matrix.ops]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]))

;; Edit this to point at the directory where you've downloaded
;; and compiled binary-matrix-factorization
(def bmf-dir "/Users/nathell/projects/vendor/binary-matrix-factorization/bin")

(matrix/set-current-implementation :vectorz)

(defn bmmul [x y]
  (matrix/emap #(if (pos? %) 1 0) (matrix/mmul x y)))

(defn ->dense [{:keys [shape ones]}]
  (let [[n m] shape]
    (matrix/set-indices! (matrix/zero-matrix n m)
                         ones
                         (repeat (count ones) 1))))

(defn parse-txt [filename]
  (with-open [rdr (io/reader filename)]
    (let [lines (line-seq rdr)]
      (matrix/matrix (mapv #(read-string (str "[" % "]")) lines)))))

(defn save-csv [outfile {:keys [shape ones]}]
  (with-open [w (io/writer outfile)]
    (binding [*out* w]
      (println "n,m")
      (let [[n m] shape]
        (printf "%s,%s\n" n m)
        (println "i,j,x_ij")
        (doseq [[i j] ones]
          (printf "%s,%s,%s\n" i j 1))))))

(defn decompose
  ([matrix rank] (decompose matrix rank 1))
  ([matrix rank init-alg]
   (let [temp-file (java.io.File/createTempFile "matrix" ".csv")]
     (try
       (save-csv temp-file matrix)
       (sh/sh (str bmf-dir "/wrapper.sh") (str temp-file) (str rank) (str init-alg))
       [(parse-txt (str bmf-dir "/Sout.txt"))
        (parse-txt (str bmf-dir "/Bout.txt"))]
       (finally
         (.delete temp-file))))))

(defn residue [{:keys [matrix]} [sm bm]]
  (let [m (->dense matrix)
        m' (bmmul sm bm)
        residue (matrix/emap - m' m)]
    {:residue residue
     :num-rules (matrix/non-zero-count m')
     :num-added (count (filter pos? (matrix/eseq residue)))
     :num-removed (count (filter neg? (matrix/eseq residue)))}))
