(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]))

;;; general
(def ^:const ^:private BUFFER-SIZE (* 16 1024))

(defn- plugin-name?
  "returns a function that tests if plugin is named 'name'."
  [name]
  #(= name (get-in % ["plugin" "name"])))

(defn sort-by-id-desc
  "Sort list of items by elements with 'id' key in descending numeric order."
  [items]
  (sort-by #(get % "id") > items))

(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [^String prefix ^String s]
  (if (.startsWith s prefix) s (str prefix s)))

;;; access utilities

(defn experiment-sample-maps
  "Return a list of samples for the experiment."
  [exp]
  ;; two ways to get samples, check that result is the same
  ;; example of eliment under exp->samples key:
  ;;  {"externalId" "",
  ;;   "name" "C66_2169-74",
  ;;   "displayedName" "C66 2169-74",
  ;;   "date" "2013-10-19T11:44:45.000360+00:00",
  ;;   "status" "run",
  ;;   "experiments" ["/rundb/api/v1/experiment/65/"],
  ;;   "id" 189,
  ;;   "sampleSets" [],
  ;;   "resource_uri" "/rundb/api/v1/sample/189/",
  ;;   "description" nil}
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
  "Return a map of barcodes and vector of samples for the experiment.
Normally there should only be one sample per barcode."
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

(defn experiment-pgm-name
  [exp]
  (exp "pgmName"))

(defn experiment-result-date
  [exp]
  (inst/read-instant-timestamp (exp "resultDate")))

(defn experiment-chip-type
  [exp]
  (exp "chipType"))

(defn plugin-barcodes
  "Return a sorted list of barcodes for the plugin result."
  [plugin-result]
  (sort (keys (get-in plugin-result ["store" "barcodes"]))))

;;; paths

(defn- pluginresult-api-path
  "API path to pluginresult files."
  [res]
  (let [{^String path "path"
         ^String report-link "reportLink"} res]
    ;; sample path:
    ;;   "/results/analysis/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    ;; sample reportLink:
    ;;   "/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/"
    ;; required API path to report files:
    ;;   "/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    (.substring path (.indexOf path report-link))))

(defn bam-path
  "Return the bam path for a particular barcode based on the result 'bamLink'"
  [result barcode]
  (str (result "reportLink") (name barcode) "_rawlib.bam")
  ;; eg: /output/Home/Auto_user_AIB-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/IonXpress_009_rawlib.bam
  
  ;; alternatively, more complicated but possibly less assumptions and
  ;; safer?:-
  ;; eg: /output/Home/Auto_user_AIB-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/download_links/IonXpress_009_R_2013_03_11_23_41_27_user_AIB-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_Auto_user_AIB-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7.bam
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

;;; get data from API

(defn resource-file
  "Return a file from host."
  [host creds file-path]
  (:body (client/get (str "http://" host file-path)
                     {:basic-auth creds})))

(defn write-resource-file
  "Write a file from host. Deletes the file if an exception occurs."
  [host creds file-path dest-file & [opts]]
  (let [res (str "http://" host file-path)]
    (try
      (with-open [o (io/output-stream dest-file)]
        (let [i (:body (client/get res (merge {:as :stream :basic-auth creds} opts)))]
          (io/copy i o :buffer-size BUFFER-SIZE))
        dest-file)
      (catch Exception e
        #_(println "error: " res " -> " dest-file)
        (io/delete-file dest-file)
        (throw e)))))

(defn resource
  "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable."
  [host creds resource & [opts]]
  (:body (client/get (str "http://" host (ensure-starts-with "/rundb/api/v1/" resource))
                     {:as :json-string-keys :basic-auth creds :query-params opts})))

;;; get resources through API returning map of metadata and data

(defn experiment
  "Experiments that have run. Returns a map of metadata and objects:
'meta' key lists total_count, limit, offset and next/previos URIs.
'objects' key containl list of experiment resources."
  [host creds & [opts]]
  (resource host creds "experiment/" (assoc opts "status__exact" "run")))

(defn results
  "Results that have completed. Returns a map of metadata and objects:
'meta' key lists total_count, limit, offset and next/previos URIs.
'objects' key containl list of experiment resources."
  [host creds & [opts]]
  (resource host creds "results/" (assoc opts "status__startswith" "Completed")))

(defn pluginresult
  "Pluginresult that have completed. Returns a map of metadata and objects:
'meta' key lists total_count, limit, offset and next/previos URIs.
'objects' key containl list of experiment resources."
  [host creds & [opts]]
  (resource host creds "pluginresult/"  (assoc opts "status__startswith" "Completed")))

;;; Query individual resources by ID

(defn pluginresult-id
  "Pluginresult for id."
  [host creds id]
  (resource host creds (str "pluginresult/" id "/")))

(defn coverage-id
  "coverageAnalysis for id."
  [host creds id]
  (let [{{name "name"} "plugin" :as res} (pluginresult-id host creds id)]
    (if (= "coverageAnalysis" name) res)))

(defn variant-call-id
  "variantCall for id."
  [host creds id]
  (let [{{name "name"} "plugin" :as res} (pluginresult-id host creds id)]
    (if (= "variantCaller" name) res)))

;;; query objects by experiment 

(defn experiment-name
  "Experiment by name."
  [host creds name & [opts]]
  (let [{{tot "total_count"} "meta"
         exp "objects"} (resource host creds "experiment/" (merge opts {"expName__exact" name "status__exact" "run"}))]
    (if (= 1 tot) (first exp))))

(defn experiment-results
  "List of results that have completed for an experiment and are not thumbnails, returned in most-recent-first order."
  [host creds exp & [opts]]
  (sort-by-id-desc
   (remove #(get-in % ["metaData" "thumb"])
           (map #(resource host creds % (merge {"status__startswith" "Completed"} opts)) (get exp "results")))))

(defn experiment-pluginresults
  "List of plugin results that have completed for an experiment, returned in most-recent-first order."
  [host creds exp & [opts]]
  (sort-by-id-desc
   (map #(resource host creds % (merge {"status__exact" "Completed"} opts))
        (mapcat #(get % "pluginresults") (experiment-results host creds exp)))))

(defn experiment-coverage
  "List of coverageAnalysis plugin results that have completed, for an experiment, returned in most-recent-first order."
  [host creds exp & [opts]]
  (sort-by-id-desc
   (filter (plugin-name? "coverageAnalysis")
           (experiment-pluginresults host creds exp opts))))

(defn experiment-variants
  "List of variantCaller plugin results that have completed, for an experiment, returned in most-recent-first order."
  [host creds exp & [opts]]
  (sort-by-id-desc
   (filter (plugin-name? "variantCaller")
           (experiment-pluginresults host creds exp opts))))

(defn- result-metrics
  "Sorted list of metrics for a result."
  [metric-name host creds res & [opts]]
  (sort-by-id-desc
   (map #(resource host creds % opts) (get res metric-name))))

(def result-libmetrics
  (partial result-metrics "libmetrics"))

(def result-qualitymetrics
  (partial result-metrics "qualitymetrics"))

(def result-analysismetrics
  (partial result-metrics "analysismetrics"))

(def result-tfmetrics
  (partial result-metrics "tfmetrics"))

