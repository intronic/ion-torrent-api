(ns ion-torrent-api.data
  (:require [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]))


;;; ;;;;;;;;;;;;;;;;;
;;; Experiment

(defn experiment-name
  "Experiment name."
  [exp]
  (get exp "expName"))

(defn experiment-id
  [exp]
  (get exp "id"))

(defn experiment-uri
  [exp]
  (get exp "resource_uri"))

(defn experiment-keys
  [exp]
  (into #{} (keys exp)))

(defn experiment-run-type
  [exp]
  (get exp "runtype"))

(defn experiment-chip-type
  [exp]
  (get exp "chipType"))

(defn experiment-pgm-name
  [exp]
  (exp "pgmName"))

(defn experiment-result-uri
  [exp]
  (get exp "results"))

(defn experiment-result-date
  [exp]
  (inst/read-instant-timestamp (get exp "resultDate")))

(defn experiment-dir
  [exp]
  (get exp "expDir"))

(defn experiment-status-run?
  [exp]
  (= "run" (get exp "status")))

(defn experiment-sample-maps
  "Return a list of samples for the experiment. Each sample is a map of the following:
   {\"externalId\" \"\",
    \"name\" \"C66_2169-74\",
    \"displayedName\" \"C66 2169-74\",
    \"date\" \"2013-10-19T11:44:45.000360+00:00\",
    \"status\" \"run\",
    \"experiments\" [\"/rundb/api/v1/experiment/65/\"],
    \"id\" 189,
    \"sampleSets\" [],
    \"resource_uri\" \"/rundb/api/v1/sample/189/\",
    \"description\" nil}"
[exp]
  (get exp "samples"))

(defn experiment-sample-names
  "Return a sorted list of sample names for the experiment."
  [exp]
  (sort (map #(% "displayedName") (experiment-sample-maps exp))))

(defn experiment-sample-barcode-map
  "Return a map of samples and vector of barcodes for the experiment."
  [exp]
  (let [samp-bc-map (into {}
                          (for [[s {barcodes "barcodes"}] (mapcat #(% "barcodedSamples")
                                                                  (exp "eas_set"))]
                            [s barcodes]))
        samps (sort (keys samp-bc-map))
        names (experiment-sample-names exp)]
    (assert (= samps names)
            (pr-str "Sample name mismatch (samples=" samps ", names=" names ")"))
    samp-bc-map))

(defn experiment-barcode-sample-map
  "Return a map of barcodes to sample for the experiment.
Fails if there is more than one barcode for a single sample."
  [exp]
  (into {} (for [[s barcodes] (experiment-sample-barcode-map exp)
                 bc barcodes]
             [bc s])))

(defn experiment-barcode-sample-map-with-dups
  "Return a map of barcodes and vector of samples for the experiment.
Handles the case where a sample has 2 barcodes."
  [exp]
  (reduce (fn [m [k v]]
            (update-in m [k] (fnil conj []) v))
          {}
          (for [[s barcodes] (experiment-sample-barcode-map exp)
                bc barcodes]
            [bc s])))

(defn experiment-samples
  "Return a sorted list of samples for the experiment."
  [exp]
  (sort (keys (experiment-sample-barcode-map exp))))

(defn experiment-barcodes
  "Return a sorted list of barcodes for the experiment."
  [exp]
  (sort (keys (experiment-barcode-sample-map exp))))

;;; ;;;;;;;;;;;;;;;;;
;;; Result

(defn result-pluginresults
  "Plugin results for an experiment result."
  [res]
  (get res "pluginresults"))

(defn result-metadata-thumb
  "Result metadata thumbnails."
  [res]
  (get-in res ["metaData" "thumb"]))

;;; ;;;;;;;;;;;;;;;;;
;;; Plugin

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


