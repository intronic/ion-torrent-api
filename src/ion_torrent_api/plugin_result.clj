(ns ion-torrent-api.plugin-result
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))
(comment
  (defn plugin-result-complete?
    [pr]
    (= "Completed" (plugin-result-status pr)))

  (defn plugin-result-complete?
    [pr]
    (= "Completed" (plugin-result-status pr)))

  (defn plugin-result-coverage?
    [pr]
    (= "coverageAnalysis" (plugin-result-plugin-name pr)))

  (defn plugin-result-variant-caller?
    [pr]
    (= "variantCaller" (plugin-result-plugin-name pr)))

  (defn plugin-result-barcodes
    "Return a sorted list of barcodes for the plugin result."
    [pr]
    (sort (keys (get-in pr ["store" "barcodes"]))))

  (defn plugin-result-barcode-counts
    [pr]
    (get-in pr ["store" "barcodes"]))

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
    [pr bc]
    (if-let [prefix (get-in pr ["store" "barcodes" (name bc) "Alignments"])]
      (str (plugin-result-api-path-prefix pr) "/" (name bc) "/" prefix ".amplicon.cov.xls")))

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
    (str (plugin-result-api-path-tsvc-variant-file pr bc) ".tbi")))
