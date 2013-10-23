(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]))

(defn- pluginresult-api-path
  "API path to pluginresult files."
  [res]
  (let [{path "path"
         report-link "reportLink"} res]
    ;; sample path:
    ;;   "/results/analysis/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    ;; sample reportLink:
    ;;   "/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/"
    ;; required API path to report files:
    ;;   "/output/Home/Auto_user_AIB-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
    (.substring path (.indexOf path report-link))))

(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [prefix s]
  (if (.startsWith s prefix) s (str prefix s)))

(defn resource-file
  "Return a file from host."
  [host creds file-path]
  (:body (client/get (str "http://" host file-path)
                     {:basic-auth creds})))

(defn write-resource-file
  "write a file from host."
  [host creds file-path dest-file]
  (with-open [o (io/output-stream dest-file)]
    (io/copy (:body (client/get (str "http://" host file-path)
                                {:as :stream :basic-auth creds}))
             o
             :buffer-size (* 16 1024))
    dest-file))

(defn resource
  "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable."
  [host creds resource & [opts]]
  (:body (client/get (str "http://" host (ensure-starts-with "/rundb/api/v1/" resource))
                     {:as :json-string-keys :basic-auth creds :query-params opts})))

(defn experiment
  "Experiments that have run."
  [host creds & [opts]]
  (resource "experiment/" host creds (assoc opts "status__exact" "run")))

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
  "Results that have completed for an experiment and are not thumbnails."
  [host creds exp]
  (remove #(get-in % ["metaData" "thumb"])
          (map #(resource host creds % {"status__startswith" "Completed"}) (get exp "results"))))

(defn experiment-pluginresults
  "Plugin results that have completed for an experiment."
  [host creds exp]
  (map #(resource host creds % {"status__exact" "Completed"})
       (mapcat #(get % "pluginresults") (experiment-results host creds exp))))

(defn experiment-coverage
  "coverageAnalysis plugin results that have completed, for an experiment."
  [host creds exp]
  (filter #(-> % (get-in ["plugin" "name"]) (= "coverageAnalysis"))
          (experiment-pluginresults host creds exp)))

(defn experiment-variants
  "variantCaller plugin results that have completed, for an experiment."
  [host creds exp]
  (filter #(-> % (get-in ["plugin" "name"]) (= "variantCaller"))
          (experiment-pluginresults host creds exp)))

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

(defn coverage-amplicon-file-path
  "Coverage by amplicon file path. Barcode is a keyword or string."
  [cov barcode]
  (let [prefix (get-in cov ["store" "barcodes" (name barcode) "Alignments"])]
    (str (pluginresult-api-path cov) "/" (name barcode) "/" prefix ".amplicon.cov.xls")))

(defn tsvc-variant-file-path
  "TSVC variant vcf.gz file path. Barcode is a keyword or string."
  [res barcode]
  (let [path (str (pluginresult-api-path res) "/" (name barcode) "/TSVC_variants.vcf.gz" )]
    path))

(defn tsvc-variant-tbi-file-path
  "TSVC variant vcf.gz.tbi file path. Barcode is a keyword or string."
  [res barcode]
  (let [path (str (pluginresult-api-path res) "/" (name barcode) "/TSVC_variants.vcf.gz.tbi" )]
    path))



#_(defn coverage-amplicon-files
  [cov]
  (map #(coverage-amplicon-file-path cov (name %)) (-> cov (get-in ["store" "barcodes"]) keys)))

