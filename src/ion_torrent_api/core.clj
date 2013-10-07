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
  [file-path host creds]
  (:body (client/get (str "http://" host file-path)
                     {:basic-auth creds})))

(defn resource
  "Return a JSON resource from host."
  [resource host creds & [opts]]
  (:body (client/get (str "http://" host (ensure-starts-with "/rundb/api/v1/" resource))
                     {:as :json :basic-auth creds :query-params opts})))

(defn experiment
  "Experiments that have run."
  [host creds & [opts]]
  (resource "experiment/" host creds (assoc opts :status__exact "run")))

(defn results
  "Results that have completed."
  [host creds & [opts]]
  (resource "results/" host creds (assoc opts :status__startswith "Completed")))

(defn pluginresult
  "Pluginresult that have completed."
  [host creds & [opts]]
  (resource "pluginresult/" host creds (assoc opts :status__startswith "Completed")))

(defn pluginresult-id
  "Pluginresult that have completed."
  [host creds id]
  (resource (str "pluginresult/" id "/") host creds))

