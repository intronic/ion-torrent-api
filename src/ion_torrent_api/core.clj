(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]))

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

;;; general

(defn- plugin-name?
  "returns a function that tests if plugin is named 'name'."
  [name]
  #(= name (get-in % ["plugin" "name"])))

(defn- sort-by-id-desc
  "Sort list of items by elements with 'id' key in descending numeric order."
  [items]
  (sort-by #(get % "id") > items))

(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [^String prefix ^String s]
  (if (.startsWith s prefix) s (str prefix s)))

;;; access utilities

(defn experiment-samples
  "Return a sorted list of samples for the experiment."
  [exp]
  (sort (into #{}
              (apply concat (for [eas (exp "eas_set")]
                              (keys (get eas "barcodedSamples")))))))

(defn experiment-barcodes
  "Return a sorted list of barcodes for the experiment."
  [exp]
  (sort (into #{}
              (apply concat (for [eas (exp "eas_set")
                                  [_ {barcodes "barcodes"}] (get eas "barcodedSamples")]
                              barcodes)))))

(defn plugin-barcodes
  "Return a sorted list of barcodes for the plugin result."
  [plugin-result]
  (sort (keys (get-in plugin-result ["store" "barcodes"]))))

;;; paths
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
  [host creds file-path dest-file]
  (try
   (with-open [o (io/output-stream dest-file)]
     (io/copy (:body (client/get (str "http://" host file-path)
                                 {:as :stream :basic-auth creds}))
              o
              :buffer-size (* 16 1024))
     dest-file)
   (catch Exception e
     (io/delete-file dest-file)
     (throw e))))

(defn resource
  "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable."
  [host creds resource & [opts]]
  (:body (client/get (str "http://" host (ensure-starts-with "/rundb/api/v1/" resource))
                     {:as :json-string-keys :basic-auth creds :query-params opts})))

;;; get resources

(defn experiment
  "Experiments that have run."
  [host creds & [opts]]
  (resource host creds "experiment/" (assoc opts "status__exact" "run")))

(defn experiment-name
  "Experiment by name."
  [host creds name & [opts]]
  (let [{{tot "total_count"} "meta"
         exp "objects"} (resource host creds "experiment/" (merge opts {"expName__exact" name "status__exact" "run"}))]
    (if (= 1 tot) (first exp))))

(defn results
  "Results that have completed."
  [host creds & [opts]]
  (resource host creds "results/" (assoc opts "status__startswith" "Completed")))

(defn experiment-results
  "Results that have completed for an experiment and are not thumbnails, returned in most-recent-first order."
  [host creds exp]
  (sort-by-id-desc
   (remove #(get-in % ["metaData" "thumb"])
           (map #(resource host creds % {"status__startswith" "Completed"}) (get exp "results")))))

(defn experiment-pluginresults
  "Plugin results that have completed for an experiment, returned in most-recent-first order."
  [host creds exp]
  (map #(resource host creds % {"status__exact" "Completed"})
       (mapcat #(get % "pluginresults") (experiment-results host creds exp))))

(defn experiment-coverage
  "coverageAnalysis plugin results that have completed, for an experiment, returned in most-recent-first order."
  [host creds exp]
  (filter (plugin-name? "coverageAnalysis")
          (experiment-pluginresults host creds exp)))

(defn experiment-variants
  "variantCaller plugin results that have completed, for an experiment, returned in most-recent-first order."
  [host creds exp]
  (sort-by-id-desc
   (filter (plugin-name? "variantCaller")
           (experiment-pluginresults host creds exp))))

(defn pluginresult
  "Pluginresult that have completed."
  [host creds & [opts]]
  (resource host creds "pluginresult/"  (assoc opts "status__startswith" "Completed")))

(defn pluginresult-id
  "Pluginresult that have completed."
  [host creds id]
  (resource host creds (str "pluginresult/" id "/")))

(defn coverage
  "coverageAnalysis for id."
  [host creds id]
  (let [{{name "name"} "plugin" :as res} (pluginresult-id host creds id)]
    (if (= "coverageAnalysis" name) res)))

(defn variant-call
  "variantCall for id."
  [host creds id]
  (let [{{name "name"} "plugin" :as res} (pluginresult-id host creds id)]
    (if (= "variantCaller" name) res)))






#_(defn coverage-amplicon-files
  [cov]
  (map #(coverage-amplicon-file-path cov (name %)) (-> cov (get-in ["store" "barcodes"]) keys)))

