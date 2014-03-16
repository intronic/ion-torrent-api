(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [ion-torrent-api.data :as data]))

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

;;; ;;;;;;;;;;;;;;;;;;;;;;;;
;;; Query Torrent Server API

;;; generic calls for resources and resource files
(defn- get-resource-file
  "Return a file from host."
  [creds host file-path]
  (:body (io! (client/get (str host file-path) {:basic-auth creds}))))

(defn- get-resource-file-to-stream
  "Get a file from host and copy to stream."
  [creds host file-path out-stream & [opts]]
  (let [i (:body (io! (client/get (str host file-path) {:as :stream :basic-auth creds :query-params opts})))]
    (io/copy i out-stream :buffer-size BUFFER-SIZE)))

(defn- get-resource-file-to-file
  "Get a file from host to local file. Deletes the local file if an exception occurs."
  [creds host file-path dest-file & [opts]]
  (try
    (with-open [out (io/output-stream dest-file)]
      (get-resource-file-to-stream creds host file-path out opts)
      dest-file)
    (catch Exception e
      #_(println "error: " res " -> " dest-file)
      (io/delete-file dest-file)
      (throw e))))

(defn- get-resource
  "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable.
host should "
  [creds host resource & [opts]]
  (:body (io! (client/get (str host (ensure-starts-with "/rundb/api/v1/" resource))
                          {:as :json-string-keys :basic-auth creds :query-params opts}))))

(defn- get-completed-resource
  "Get resources with Completed status."
  [creds host resource & [opts]]
  (get-resource creds host resource (assoc opts "status__startswith" "Completed")))

;;; ;;;;;;;;;;;;;;;;;;;;;;;
;;; Get Experiment

(defn get-experiment
  "Experiments that have run. Returns a map of metadata and objects:
'meta' key lists total_count, limit, offset and next/previos URIs.
'objects' key containl list of experiment resources."
  [creds host & [opts]]
  (get-resource creds host "experiment/" (assoc opts "status__exact" "run")))

(defn get-experiment-name
  "Get experiment by name."
  [creds host name & [opts]]
  (let [res (get-experiment creds host (assoc opts "expName__exact" name))]
    (first (get res "objects"))))

;;; ;;;;;;;;;;;;;;;;;;;;;;;
;;; Get Result

(defn get-result-uri
  [creds host uri]
  (get-completed-resource creds host uri))

(defn get-result-id
  [creds host id]
  (get-completed-resource creds host (str "results/" id)))





;;; get-all-results : get-completed-resource "results/"

(defn get-experiment-results
  "List of results that have completed for an experiment and are not thumbnails, returned in most-recent-first order."
  [creds host exp]
  (->> exp
       (get exp "results")              ; HACK replace with experiment-results accessor
       (map #(get-completed-resource creds host %))
       (remove #(get-in % ["metaData" "thumb"])) ; HACK how to exclude thumbs in the query API?
       sort-by-id-desc))

;;; get-pluginresult-uri : get-completed-resource
;;; get-all-pluginresult : get-completed-resource "pluginresult/"

(defn get-pluginresult-id
  "Pluginresult for id."
  [creds host id]
  (get-completed-resource creds host (str "pluginresult/" id "/")))

(defn get-coverage-id
  "coverageAnalysis for id."
  [creds host id]
  (let [{{name "name"} "plugin" :as res}
        (get-pluginresult-id creds host id)]
    (if (= "coverageAnalysis" name) res)))

(defn get-variant-call-id
  "variantCall for id."
  [creds host id]
  (let [{{name "name"} "plugin" :as res}
        (get-pluginresult-id creds host id)]
    (if (= "variantCaller" name) res)))

(defn get-experiment-pluginresults
  "List of plugin results that have completed for an experiment, returned in most-recent-first order."
  [host creds exp]
  (sort-by-id-desc
   (map #(get-completed-resource host creds %)
        (mapcat #(get % "pluginresults") ; HACK replace with accessor
                (get-experiment-results host creds exp)))))

(defn get-experiment-coverage
  "List of coverageAnalysis plugin results that have completed, for an experiment, returned in most-recent-first order."
  [creds host exp]
  (sort-by-id-desc
   (filter (plugin-name? "coverageAnalysis")
           (get-experiment-pluginresults creds host exp))))

(defn get-experiment-variants
  "List of variantCaller plugin results that have completed, for an experiment, returned in most-recent-first order."
  [creds host exp]
  (sort-by-id-desc
   (filter (plugin-name? "variantCaller")
           (get-experiment-pluginresults creds host exp))))

(defn- get-result-metrics
  "Sorted list of metrics for a result."
  [metric-name creds host res]
  (sort-by-id-desc
   (map #(get-resource creds host %)
        (get res metric-name))))

(def get-result-libmetrics
  (partial get-result-metrics "libmetrics"))

(def get-result-qualitymetrics
  (partial get-result-metrics "qualitymetrics"))

(def get-result-analysismetrics
  (partial get-result-metrics "analysismetrics"))

(def get-result-tfmetrics
  (partial get-result-metrics "tfmetrics"))
