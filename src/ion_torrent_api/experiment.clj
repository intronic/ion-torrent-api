(ns ion-torrent-api.experiment
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))

(defn experiment-name
  [exp]
  (get exp "expName"))

(defn experiment-display-name
  [exp]
  (get exp "displayName"))

(defn experiment-id
  [exp]
  (get exp "id"))

(defn experiment-uri
  [exp]
  (get exp "resource_uri"))

(defn experiment-date
  [exp]
  (inst/read-instant-timestamp (get exp "date")))

(defn experiment-keys
  [exp]
  (into #{} (keys exp)))

(defn experiment-run-type
  [exp]
  (get exp "runtype"))

(defn experiment-chip-type
  [exp]
  (get exp "chipType"))

(defn experiment-pgm-name
  [exp]
  (exp "pgmName"))

(defn experiment-result-uri
  [exp]
  (get exp "results"))

(defn experiment-result-date
  [exp]
  (inst/read-instant-timestamp (get exp "resultDate")))

(defn experiment-dir
  [exp]
  (get exp "expDir"))

(defn experiment-run?
  [exp]
  (= "run" (get exp "status")))

(defn experiment-complete?
  [exp]
  (and (experiment-run? exp)
       (= "Complete" (get exp "ftpStatus"))))

(defn experiment-sample-maps
  "Return a list of samples for the experiment. Each sample is a map of the following:
   {\"externalId\" \"\",
    \"name\" \"C66_2169-74\",
    \"displayedName\" \"C66 2169-74\",
    \"date\" \"2013-10-19T11:44:45.000360+00:00\",
    \"status\" \"run\",
    \"experiments\" [\"/rundb/api/v1/experiment/65/\"],
    \"id\" 189,
    \"sampleSets\" [],
    \"resource_uri\" \"/rundb/api/v1/sample/189/\",
    \"description\" nil}"
[exp]
  (get exp "samples"))

(defn experiment-sample-names
  "Return a sorted list of sample names for the experiment."
  [exp]
  (sort (map #(% "displayedName") (experiment-sample-maps exp))))

(defn experiment-sample-barcode-map
  "Return a map of samples and vector of barcodes for the experiment."
  [exp]
  (let [samp-bc-map (into {}
                          (for [[s {barcodes "barcodes"}] (mapcat #(% "barcodedSamples")
                                                                  (exp "eas_set"))]
                            [s barcodes]))
        samps (sort (keys samp-bc-map))
        names (experiment-sample-names exp)]
    (assert (= samps names)
            (pr-str "Sample name mismatch (samples=" samps ", names=" names ")"))
    samp-bc-map))

(defn experiment-barcode-sample-map
  "Return a map of barcodes to sample for the experiment.
Fails if there is more than one barcode for a single sample."
  [exp]
  (into {} (for [[s barcodes] (experiment-sample-barcode-map exp)
                 bc barcodes]
             [bc s])))

(defn experiment-barcode-sample-map-with-dups
  "Return a map of barcodes and vector of samples for the experiment.
Handles the case where a sample has 2 barcodes."
  [exp]
  (reduce (fn [m [k v]]
            (update-in m [k] (fnil conj []) v))
          {}
          (for [[s barcodes] (experiment-sample-barcode-map exp)
                bc barcodes]
            [bc s])))

(defn experiment-samples
  "Return a sorted list of samples for the experiment."
  [exp]
  (sort (keys (experiment-sample-barcode-map exp))))

(defn experiment-barcodes
  "Return a sorted list of barcodes for the experiment."
  [exp]
  (sort (keys (experiment-barcode-sample-map exp))))
