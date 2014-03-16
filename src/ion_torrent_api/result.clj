(ns ion-torrent-api.result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))

(defn result-keys
  [res]
  (into #{} (keys res)))

(defn result-id
  [res]
  (get res "id"))

(defn result-name
  [res]
  (get res "resultsName"))

(defn result-uri
  [res]
  (get res "resource_uri"))

(defn result-report-link
  [res]
  (get res "reportLink"))

(defn result-bam-link
  [res]
  (get res "bamLink"))

(defn result-fastq-link
  [res]
  (get res "fastqLink"))

(defn result-file-path
  [res]
  (get res "filesystempath"))

(defn result-status
  [res]
  (get res "status"))

(defn result-timestamp
  [res]
  (inst/read-instant-timestamp (get res "timeStamp")))

(defn result-version
  [res]
  (get res "analysisVersion"))

(defn result-experiment
  [res]
  (get res "experiment"))

(defn result-plugin-state
  [res]
  (get res "pluginState"))

(defn result-plugin-results
  [res]
  (get res "pluginresults"))

(defn result-tf-metrics
  [res]
  (get res "tfmetrics"))

(defn result-lib-metrics
  [res]
  (get res "libmetrics"))

(defn result-quality-metrics
  [res]
  (get res "qualitymetrics"))

(defn result-analysis-metrics
  [res]
  (get res "analysismetrics"))

(defn result-run-id
  [res]
  (get res "runid"))

(defn result-reference
  [res]
  (get res "reference"))

(defn result-projects
  [res]
  (get res "projects"))

(defn result-report-storage
  [res]
  (get res "reportstorage"))

(defn result-plugin-store
  [res]
  (get res "pluginStore"))

(defn result-metadata-thumb
  [res]
  (get-in res ["metaData" "thumb"]))
