(ns ion-torrent-api.core
  (:require [clj-http.client :as client]))

(defn- pluginresult-api-path
  "API path to pluginresult files."
  [res]
  (let [{path :path
         report-link :reportLink} res]
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

(defn resource
  "Return a JSON resource from host."
  [host creds resource & [opts]]
  (:body (client/get (str "http://" host (ensure-starts-with "/rundb/api/v1/" resource))
                     {:as :json :basic-auth creds :query-params opts})))

(defn experiment
  "Experiments that have run."
  [host creds & [opts]]
  (resource "experiment/" host creds (assoc opts :status__exact "run")))

(defn experiment-name
  "Experiment by name."
  [host creds name & [opts]]
  (let [{{tot :total_count} :meta
         exp :objects} (resource host creds "experiment/" (merge opts {:expName__exact name :status__exact "run"}))]
    (if (= 1 tot) (first exp))))

(defn results
  "Results that have completed."
  [host creds & [opts]]
  (resource host creds "results/" (assoc opts :status__startswith "Completed")))

(defn experiment-results
  "Results that have completed."
  [host creds exp & [opts]]
  (map (partial resource host creds) (:results exp)))

(defn experiment-coverage
  "given an experiment, get all coverageAnalysis plugin results"
  [host creds exp & [opts]]
  (filter #(-> % :plugin :name (= "coverageAnalysis"))
          (map (partial resource host creds)
               (mapcat :pluginresults (experiment-results host creds exp)))))

(defn pluginresult
  "Pluginresult that have completed."
  [host creds & [opts]]
  (resource host creds "pluginresult/"  (assoc opts :status__startswith "Completed")))

(defn pluginresult-id
  "Pluginresult that have completed."
  [host creds id]
  (resource host creds (str "pluginresult/" id "/")))

(defn coverage
  "coverageAnalysis for id."
  [host creds id]
  (let [{{name :name} :plugin :as res} (pluginresult-id host creds id)]
    (if (= "coverageAnalysis" name) res)))

(defn variant-call
  "variantCall for id."
  [host creds id]
  (let [{{name :name} :plugin :as res} (pluginresult-id host creds id)]
    (if (= "variantCaller" name) res)))

(defn amplicon-coverage-file-path
  "Coverage by amplicon file path."
  [res barcode]
  (let [{{{{prefix :Alignments} (keyword barcode)} :barcodes} :store} res
        path (str (pluginresult-api-path res) "/" barcode "/" prefix ".amplicon.cov.xls")]
    path))

(defn tsvc-variant-file-path
  "TSVC variant vcf file path."
  [res barcode]
  (let [path (str (pluginresult-api-path res) "/" barcode "/TSVC_variants.vcf.gz" )]
    path))
