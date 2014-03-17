(ns ion-torrent-api.plugin-result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))

(defn plugin-result-keys
  [pr]
  (into #{} (keys pr)))

(defn plugin-result-id
  [pr]
  (get pr "id"))

(defn plugin-result-uri
  [pr]
  (get pr "resource_uri"))

(defn plugin-result-result-uri
  [pr]
  (get pr "result"))

(defn plugin-result-path
  [pr]
  (get pr "path"))

(defn plugin-result-status
  [pr]
  (get pr "state"))

(defn plugin-result-start-time
  [pr]
  (inst/read-instant-timestamp (get pr "starttime")))

(defn plugin-result-end-time
  [pr]
  (inst/read-instant-timestamp (get pr "endtime")))

(defn plugin-result-duration
  [pr]
  (get pr "duration"))

(defn plugin-result-size
  [pr]
  (get pr "size"))

(defn plugin-result-name
  [pr]
  (get pr "resultName"))

(defn plugin-result-report-link
  [pr]
  (get pr "reportLink"))

(defn plugin-result-plugin
  [pr]
  (get pr "plugin"))

(defn plugin-result-store
  [pr]
  (get pr "store"))


(defn plugin-barcodes
  "Return a sorted list of barcodes for the plugin result."
  [plugin-result]
  (sort (keys (get-in plugin-result ["store" "barcodes"]))))

(defn plugin-versioned-name
  [plugin-result]
  (get-in plugin-result ["plugin" "versionedName"]))

(defn plugin-configuration
  [plugin-result]
  (get-in plugin-result ["store" "Configuration"]))


;;; ;;;;;;;;;;;;;;;;;
;;; paths

(defn- pluginresult-api-path
  "API path to pluginresult files."
  [res]
  (let [{^String path "path"
         ^String report-link "reportLink"} res]
    ;; sample path:
    ;;   "/results/analysis/output/Home/Auto_user_XXX-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    ;; sample reportLink:
    ;;   "/output/Home/Auto_user_XXX-24-AmpliSeq_CCP_24_50_061/"
    ;; required API path to report files:
    ;;   "/output/Home/Auto_user_XXX-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    (.substring path (.indexOf path report-link))))

(defn bam-path
  "Return the bam path for a particular barcode based on the result 'bamLink'"
  [result barcode]
  (str (result "reportLink") (name barcode) "_rawlib.bam")
  ;; eg: /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/IonXpress_009_rawlib.bam
  
  ;; alternatively, more complicated but possibly less assumptions and
  ;; safer?:-
  ;; eg: /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/download_links/IonXpress_009_R_2013_03_11_23_41_27_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7.bam
  #_(let [bam (io/as-file (result "bamLink"))]
      (str (io/file (.getParent bam) "download_links" (str (name barcode) "_" (.getName bam))))))

(defn bam-bai-path
  "Return the bam bai path for a particular barcode"
  [result barcode]
  (str (bam-path result barcode) ".bai"))

(defn result-pdf-path
  "Return the path for a result summary PDF"
  [{id "id"}]
  (format "/report/latex/%d.pdf" id))

(defn coverage-amplicon-file-path
  "Coverage by amplicon file path. Barcode is a keyword or string."
  [cov barcode]
  (let [prefix (get-in cov ["store" "barcodes" (name barcode) "Alignments"])]
    (str (pluginresult-api-path cov) "/" (name barcode) "/" prefix ".amplicon.cov.xls")))

(defn tsvc-variant-path-prefix
  "TSVC variant path prefix."
  [res]
  (str (pluginresult-api-path res) "/"))

(defn tsvc-variant-target-region-path
  "Target region bed file path."
  [res]
  (if-let [reg (get-in res ["store" "Target Regions"])]
    (str (tsvc-variant-path-prefix res) reg ".bed")))

(defn tsvc-variant-file-path
  "TSVC variant vcf.gz file path. Barcode is a keyword or string."
  [res barcode]
  (str (tsvc-variant-path-prefix res) (name barcode) "/TSVC_variants.vcf.gz" ))

(defn tsvc-variant-tbi-file-path
  "TSVC variant vcf.gz.tbi file path. Barcode is a keyword or string."
  [res barcode]
  (str (tsvc-variant-file-path res barcode) ".tbi"))


