(ns ion-torrent-api.plugin-result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))

(defn plugin-result-keys
  [pr]
  (into #{} (keys pr)))

(defn plugin-result-id
  [pr]
  (get pr "id"))

(defn plugin-result-uri
  [pr]
  (get pr "resource_uri"))

(defn plugin-result-result-uri
  [pr]
  (get pr "result"))

(defn plugin-result-path
  [pr]
  (get pr "path"))

(defn plugin-result-status
  [pr]
  (get pr "state"))

(defn plugin-result-complete?
  [pr]
  (= "Completed" (plugin-result-status pr)))

(defn plugin-result-start-time
  [pr]
  (inst/read-instant-timestamp (get pr "starttime")))

(defn plugin-result-end-time
  [pr]
  (inst/read-instant-timestamp (get pr "endtime")))

(defn plugin-result-duration
  [pr]
  (get pr "duration"))

(defn plugin-result-size
  [pr]
  (get pr "size"))

(defn plugin-result-result-name
  [pr]
  (get pr "resultName"))

(defn plugin-result-report-link
  [pr]
  (get pr "reportLink"))

(defn plugin-result-plugin
  [pr]
  (get pr "plugin"))

(defn plugin-result-store
  [pr]
  (get pr "store"))

(defn plugin-result-barcodes
  "Return a sorted list of barcodes for the plugin result."
  [pr]
  (sort (keys (get-in pr ["store" "barcodes"]))))

(defn plugin-result-barcode-counts
  [pr]
  (get-in pr ["store" "barcodes"]))

(defn plugin-result-versioned-name
  [pr]
  (get-in pr ["plugin" "versionedName"]))

(defn plugin-result-configuration
  [pr]
  (get-in pr ["store" "Configuration"]))

(defn plugin-result-target-bed-file
  [pr]
  (get-in pr ["store" "targets_bed"]))

(defn plugin-result-target-bed-file-name
  [pr]
  (if-let [bed (plugin-result-target-bed-file pr)]
    (.. (io/as-file bed) getName)))

;; path:        "/results/analysis/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"
;; reportLink:                   "/output/Home/XXX-24-YYY/"
;; API path:                     "/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"
(defn plugin-result-api-path-prefix
  "Direct path to pluginresult files through API."
  [pr]
  (let [^String path (plugin-result-path pr)
        ^String link (plugin-result-report-link pr)]
    (subs path (.indexOf path link))))

(defn plugin-result-api-path-coverage-amplicon-file
  "Coverage by amplicon file path. Barcode is a keyword or string."
  [pr-cov bc]
  (if-let [prefix (get-in pr-cov ["store" "barcodes" (name bc) "Alignments"])]
    (str (plugin-result-api-path-prefix pr-cov) "/" (name bc) "/" prefix ".amplicon.cov.xls")))

(defn plugin-result-api-path-tsvc-variant-prefix
  "TSVC variant path prefix."
  [pr]
  (str (plugin-result-api-path-prefix pr) "/"))

(defn plugin-result-api-path-tsvc-variant-target-region
  "Target region bed file path."
  [pr]
  (if-let [bed (plugin-result-target-bed-file-name pr)]
    (str (plugin-result-api-path-tsvc-variant-prefix pr) bed)))

(defn plugin-result-api-path-tsvc-variant-file
  "TSVC variant vcf.gz file path. Barcode is a keyword or string."
  [pr bc]
  (str (plugin-result-api-path-tsvc-variant-prefix pr) (name bc) "/TSVC_variants.vcf.gz" ))

(defn plugin-result-api-path-tsvc-variant-tbi-file
  "TSVC variant vcf.gz.tbi file path. Barcode is a keyword or string."
  [pr bc]
  (str (plugin-result-api-path-tsvc-variant-file pr bc) ".tbi"))
