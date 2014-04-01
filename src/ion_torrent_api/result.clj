(ns ion-torrent-api.result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))
(comment
  (defn result-keys
    [r]
    (into #{} (keys r)))

  (defn result-id
    [r]
    (get r "id"))

  (defn result-name
    [r]
    (get r "resultsName"))

  (defn result-uri
    [r]
    (get r "resource_uri"))

  (defn result-report-link
    [r]
    (get r "reportLink"))

  (defn result-bam-link
    [r]
    (get r "bamLink"))

  (defn result-fastq-link
    [r]
    (get r "fastqLink"))

  (defn result-file-path
    [r]
    (get r "filesystempath"))

  (defn result-status
    [r]
    (get r "status"))

  (defn result-complete?
    [r]
    (= "Completed" (result-status r)))

  (defn result-timestamp
    [r]
    (inst/read-instant-timestamp (get r "timeStamp")))

  (defn result-version
    [r]
    (get r "analysisVersion"))

  (defn result-experiment
    [r]
    (get r "experiment"))

  (defn result-plugin-status
    [r]
    (get r "pluginState"))

  (defn result-plugin-results
    [r]
    (get r "pluginresults"))

  (defn result-tf-metrics
    [r]
    (get r "tfmetrics"))

  (defn result-lib-metrics
    [r]
    (get r "libmetrics"))

  (defn result-quality-metrics
    [r]
    (get r "qualitymetrics"))

  (defn result-analysis-metrics
    [r]
    (get r "analysismetrics"))

  (defn result-run-id
    [r]
    (get r "runid"))

  (defn result-reference
    [r]
    (get r "reference"))

  (defn result-projects
    [r]
    (get r "projects"))

  (defn result-report-storage
    [r]
    (get r "reportstorage"))

  (defn result-plugin-store
    [r]
    (get r "pluginStore"))

  (defn result-metadata-thumb
    [r]
    (get-in r ["metaData" "thumb"]))


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
