(ns ion-torrent-api.experiment
  (:require [clojure.java.io :as io]
            [clojure.instant :as inst]))
(comment
  (defprotocol UniqueID
    "Unique Identifier"
    (unique-id [this]))




  (defrecord Experiment id name pgm-name display-name uri date run-type chip-type
             sample-map latest-result-date result-uri-set dir status ftp-status raw-map
             url)

  (defn experiment
    ([torrent-server name]
       (experiment (get-experiment torrent-server
                                   )))

    ([api-map]
       (let [main-keys ["id" "expName" "pgmName" "displayName" "resource_uri"
                        "date" "runtype" "chipType" "samples" "resultDate"
                        "results" "expDir" "status" "ftpStatus"]]
         (apply ->Experiment (concat (map (partial get api-map) main-keys)
                                     [(apply dissoc api-map "log" main-keys)])))))

  (defn experiment-get
    [])
  (defn experiment-run?
    [e]
    (= "run" (experiment-status e)))

  (defn experiment-ftp-complete?
    [e]
    (= "Complete" (experiment-ftp-status e)))

  (defn experiment-complete?
    [e]
    (and (experiment-run? e) (experiment-ftp-complete? e)))

  (comment "Sample-maps"
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
    \"description\" nil}")

  (defn experiment-sample-names
    "Return a sorted list of sample names for the experiment."
    [e]
    (sort (map #(% "displayedName") (experiment-sample-maps e))))

  (defn experiment-sample-barcode-map
    "Return a map of samples and vector of barcodes for the experiment."
    [e]
    (let [samp-bc-map (into {}
                            (for [[s {barcodes "barcodes"}] (mapcat #(% "barcodedSamples")
                                                                    (get e "eas_set"))]
                              [s barcodes]))
          samps (sort (keys samp-bc-map))
          names (experiment-sample-names e)]
      (assert (= samps names)
              (pr-str "Sample name mismatch (samples=" samps ", names=" names ")"))
      samp-bc-map))

  (defn experiment-barcode-sample-map
    "Return a map of barcodes to sample for the experiment.
Fails if there is more than one barcode for a single sample."
    [e]
    (into {} (for [[s barcodes] (experiment-sample-barcode-map e)
                   bc barcodes]
               [bc s])))

  (defn experiment-barcode-sample-map-with-dups
    "Return a map of barcodes and vector of samples for the experiment.
Handles the case where a sample has 2 barcodes."
    [e]
    (reduce (fn [m [k v]]
              (update-in m [k] (fnil conj []) v))
            {}
            (for [[s barcodes] (experiment-sample-barcode-map e)
                  bc barcodes]
              [bc s])))

  (defn experiment-samples
    "Return a sorted list of samples for the experiment."
    [e]
    (sort (keys (experiment-sample-barcode-map e))))

  (defn experiment-barcodes
    "Return a sorted list of barcodes for the experiment."
    [e]
    (sort (keys (experiment-barcode-sample-map e))))
  )
