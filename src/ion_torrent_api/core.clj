(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [ion-torrent-api
             [experiment :as e]
             [result :as r]
             [plugin-result :as pr]]))

;;; general
(def ^:const ^:private BUFFER-SIZE (* 16 1024))

(defn sort-by-id-desc
  "Sort list of items by elements with 'id' key in descending numeric order."
  [items]
  (sort-by #(get % "id") > items))

(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [^String prefix ^String s]
  (if (.startsWith s prefix) s (str prefix s)))

;;;
(defn newest-result
  "Get the newest completed result from a collection of results."
  [r-coll]
  (first (sort-by-id-desc r-coll)))

(defn newest-variant-caller-plugin-result
  "Get the newest completed variantCaller if any from a collection of plugin-results."
  [pr-coll]
  (some pr/plugin-result-variant-caller? pr-coll))

(defn newest-coverage-plugin-result
  "Get the newest completed variantCaller if any from a collection of plugin-results."
  [pr-coll]
  (some pr/plugin-result-coverage? pr-coll))

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

(defn get-resource-file-to-file
  "Get a file from host to local file. Deletes the local file if an exception occurs."
  [creds host file-path dest-file & [opts]]
  (try
    (with-open [out (io/output-stream dest-file)]
      (get-resource-file-to-stream creds host file-path out opts)
      dest-file)
    (catch Exception e
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
  (get-completed-resource creds host (str "results/" id "/")))

(defn get-plugin-result-uri
  [creds host uri]
  (get-completed-resource creds host uri))

(defn get-plugin-result-id
  [creds host id]
  (get-completed-resource creds host (str "pluginresult/" id "/")))

(defn get-experiment-results
  "All results for an experiment (completed, not thumbnails), in most-recent-first order."
  [creds host e]
  (->> (e/experiment-result-uri e)
       (map #(get-result-uri creds host %))
       (remove r/result-metadata-thumb) ; HACK how to exclude thumbs in the query API?
       sort-by-id-desc))

(defn get-result-plugin-results
  "All plugin-results for a result (completed), in most-recent-first order."
  [creds host r]
  (->> (r/result-plugin-results r)
       (map #(get-plugin-result-uri creds host %))
       sort-by-id-desc))

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
