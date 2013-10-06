(ns ion-torrent-api.core
  (:require [clj-http.client :as client]))


(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [prefix s]
  (if (.startsWith s prefix) s (str prefix s)))

(defn resource
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

