(ns ion-torrent-api.result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))
(comment
  (defn result-complete?
    [r]
    (= "Completed" (result-status r)))

  ;; HACK alternatively, more complicated but possibly less assumptions and safer?:-
  ;; eg: /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/download_links/IonXpress_009_R_2013_03_11_23_41_27_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7.bam
  ;; (let [bam (io/as-file (result "bamLink"))]
  ;;     (str (io/file (.getParent bam) "download_links" (str (name barcode) "_" (.getName bam)))))
  ;; eg:
  ;; /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/IonXpress_009_rawlib.bam

  (defn result-api-path-bam
    "Return the bam path for a particular barcode based on the result 'bamLink'"
    [r bc]
    (str (result-report-link r) (name bc) "_rawlib.bam"))

  (defn result-api-path-bai
    "Return the bam bai path for a particular barcode"
    [r bc]
    (str (result-api-path-bam r bc) ".bai"))

  (defn result-api-path-pdf
    "Return the path for a result summary PDF"
    [r]
    (format "/report/latex/%d.pdf" (result-id r))))
