(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [ion-torrent-api
             [experiment :as e]
             [result :as r]
             [plugin-result :as pr]]))

(declare get-json get-completed-resource ensure-starts-with barcode-eas-map BUFFER-SIZE)

(defprotocol TorrentServerAPI
  "Torrent Server API calls."

  (get-experiments [torrent-server] [torrent-server opts] [torrent-server limit offset]
    "Get experiments (with options 'opts' or by limit and offset).")

  (get-experiment-name [torrent-server name] [torrent-server name opts]
    "Get experiment by name (with options 'opts').")

  (get-experiment [torrent-server id-or-uri] [torrent-server id-or-uri opts]
    "Get experiment by id or uri (with options 'opts').")

  (get-result [torrent-server id-or-uri] [torrent-server id-or-uri opts]
    "Get result by id or uri (with options 'opts').")

  (get-plugin-result [torrent-server id-or-uri] [torrent-server id-or-uri opts]
    "Get plugin-result by id or uri (with options 'opts').")

  (bam-uri [this bc]
  "BAM uri for barcode.")

  (bai-uri [this bc]
  "BAM BAI uri for barcode.")

  (bam-header-uri [this bc]
  "BAM header uri for barcode."))


(defprotocol UniqueID
    "Unique Identifier"
    (unique-id [this]))


(defrecord TorrentServer [server-url creds api-path]

  TorrentServerAPI

  (get-experiments [torrent-server]
    (get-experiments torrent-server {}))

  (get-experiments [torrent-server limit offset]
    (get-experiments torrent-server {"limit" limit "offset" offset }))

  (get-experiments [torrent-server opts]
    (get-completed-resource torrent-server "experiment/" (merge {"status__exact" "run"} opts)))

  (get-experiment-name [torrent-server name]
    (get-experiment-name torrent-server name {}))

  (get-experiment-name [torrent-server name opts]
    ;; query by options returns map with "meta" and "objects" keys
    (let [{objects "objects" {total-count "total_count" :as meta} "meta"}
          (get-completed-resource torrent-server "experiment/"
                                  (merge opts {"expName__exact" name "status__exact" "run" "limit" 2}))]
      ;;      (assert (and meta total-count) "Invalid experiment name query response.")
      ;;      (assert (<= 0 total-count 1) (str "More than one experiment (" total-count ") for name [" name "]."))
      (first objects)))

  (get-experiment [torrent-server id-or-uri]
    (get-experiment torrent-server id-or-uri {}))

  (get-experiment [torrent-server id-or-uri opts]
    (get-completed-resource torrent-server (ensure-starts-with (str (:api-path torrent-server) "experiment/")
                                                               (str id-or-uri))))

  (get-result [torrent-server id-or-uri]
    (get-result torrent-server id-or-uri {}))

  (get-result [torrent-server id-or-uri opts]
    (get-completed-resource torrent-server (ensure-starts-with (str (:api-path torrent-server) "results/")
                                                               (str id-or-uri))))
  (get-plugin-result [torrent-server id-or-uri]
    (get-plugin-result torrent-server id-or-uri {}))

  (get-plugin-result [torrent-server id-or-uri opts]
    (get-completed-resource torrent-server (ensure-starts-with (str (:api-path torrent-server) "pluginresult/")
                                                               (str id-or-uri)))))

(defn torrent-server [server-url creds & [api-path]]
  (map->TorrentServer {:server-url server-url :creds creds :api-path (or api-path "/rundb/api/v1/")}))

;;; Experiment record

(defrecord Experiment [id name pgm-name display-name uri run-type chip-type sample-map
                       result-uri-set dir status ftp-status barcode-sample-map date latest-result-date raw-map]
  Object
  (toString [this] (pr-str this)))

(defn experiment [json]
  (let [main-keys ["id" "expName" "pgmName" "displayName" "resource_uri"
                   "runtype" "chipType" "samples"
                   "results" "expDir" "status" "ftpStatus"]]
    (assert (<= 0 (count (get json "eas_set")) 1) "Zero or one EAS set expected.")
    (apply ->Experiment (concat (map (partial get json) main-keys)
                                ;; for now, work with one label per barcode and one eas_set per experiment
                                [(barcode-eas-map (get (first (get json "eas_set")) "barcodedSamples"))
                                 (inst/read-instant-date (get json "date"))
                                 (inst/read-instant-date (get json "resultDate"))
                                 (apply dissoc json "log" main-keys)]))))

;;;  Result record

(defrecord Result [id name uri experiment-uri status
                   plugin-result-uri-set plugin-state-map analysis-version report-status plugin-store-map
                   bam-link fastq-link report-link filesystem-path reference
                   lib-metrics-uri-set tf-metrics-uri-set analysis-metrics-uri-set quality-metrics-uri-set
                   timestamp thumbnail? raw-map]
  Object

  (toString [this] (pr-str this))

  TorrentServerAPI

  (bam-uri [_ bc]
    (str report-link bc "_rawlib.bam"))

  (bai-uri [_ bc]
    (str report-link bc "_rawlib.bam.bai"))

  (bam-header-uri [_ bc]
    (str report-link bc "_rawlib.bam.header.sam")))


(defn result [json]
  (let [main-keys ["id" "resultsName" "resource_uri" "experiment" "status"
                   "pluginresults" "pluginState" "analysisVersion" "reportStatus" "pluginStore"
                   "bamLink" "fastqLink" "reportLink" "filesystempath" "reference"
                   "libmetrics" "tfmetrics" "analysismetrics" "qualitymetrics"]]
    (apply ->Result (concat (map (partial get json) main-keys)
                            [(inst/read-instant-date (get json "timeStamp"))
                             (boolean (get-in json ["metaData" "thumb"]))
                             (apply dissoc json main-keys)]))))

;;; PluginResult record

(defrecord PluginResult [id uri result-uri result-name state path report-link
                         name version versioned-name
                         library-type config-desc barcode-result-map target-name target-bed experiment-name
                         trimmed-reads? barcoded? start-time end-time raw-map]
  Object
  (toString [this] (pr-str this)))

(defn plugin-result [json]
  (let [main-keys ["id" "resource_uri" "result" "resultName" "state"
                   "path" "reportLink"]]
    (apply ->PluginResult (concat (map (partial get json) main-keys)
                                  (map (partial get (get json "plugin")) ["name" "version" "versionedName"])
                                  (map (partial get (get json "store")) ["Library Type" "Configuration" "barcodes" "Target Regions"
                                                                   "targets_bed" "Aligned Reads" "Trim Reads"])
                                  [(.equalsIgnoreCase "true" (get-in json ["store" "barcoded"])) ; string -> boolean
                                   (inst/read-instant-date (get json "starttime"))
                                   (inst/read-instant-date (get json "endtime"))
                                   (apply dissoc json main-keys)]))))


(def data-readers
  {'ion_torrent_api.core.Experiment ion-torrent-api.core/map->Experiment
   'ion_torrent_api.core.Result ion-torrent-api.core/map->Result
   'ion_torrent_api.core.PluginResult ion-torrent-api.core/map->PluginResult})

(defn- barcode-eas-map [m]
  (into {} (map (fn [[label {bc "barcodes"}]]
                 (assert (= 1 (count bc)) "Exactly 1 barcode per sample expected.")
                 [(first bc) label])
                m)))

(defn latest-result
  "Get the newest completed result matching the experiment from a collection of results."
  [e r-coll]
  (let [date (.getTime ^java.util.Date (:latest-result-date e))
        res (filter #(<= date (.getTime ^java.util.Date (:timestamp %)))
                    r-coll)]
    (assert (<= 0 (count res) 1) "0 or 1 latest results expected.")
    (first r-coll)))


;;;

(defn- get-json
    "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable.
host should "
    [ts resource & [opts]]
    (:body (io! (client/get (str (:server-url ts) (ensure-starts-with (:api-path ts) resource))
                            {:as :json-string-keys :basic-auth (:creds ts) :query-params opts}))))

(defn- get-completed-resource
  "Get resources with Completed status."
  [ts resource & [opts]]
  (get-json ts resource (assoc opts "status__startswith" "Completed")))



(comment
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
     (map #(get-json creds host %)
          (get res metric-name))))

  (def get-result-libmetrics
    (partial get-result-metrics "libmetrics"))

  (def get-result-qualitymetrics
    (partial get-result-metrics "qualitymetrics"))

  (def get-result-analysismetrics
    (partial get-result-metrics "analysismetrics"))

  (def get-result-tfmetrics
    (partial get-result-metrics "tfmetrics")))


;;; general
(def ^:const ^:private BUFFER-SIZE (* 16 1024))

(defn- ensure-starts-with
  "Ensure s starts with prefix."
  [^String prefix ^String s]
  (if (.startsWith s prefix) s (str prefix s)))

(defn file-name
  "File name without path."
    [f]
    (.. (io/as-file f) getName))

;;;
(comment

  (defn newest-variant-caller-plugin-result
    "Get the newest completed variantCaller if any from a collection of plugin-results."
    [pr-coll]
    (some pr/plugin-result-variant-caller? pr-coll))

  (defn newest-coverage-plugin-result
    "Get the newest completed variantCaller if any from a collection of plugin-results."
    [pr-coll]
    (some pr/plugin-result-coverage? pr-coll)))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;
;;; Query Torrent Server API
(comment
;;; generic calls for resources and resource files
  (defn- get-resource-file
    "Return a file from host."
    [creds host file-path]
    (:body (io! (client/get (str host file-path) {:basic-auth creds}))))

  (defn- get-resource-file-as-stream
    "Get a file from host as a stream."
    [creds host file-path & [opts]]
    (:body (io! (client/get (str host file-path) {:as :stream :basic-auth creds :query-params opts}))))

  (defn- get-resource-file-to-stream
    "Get a file from host and copy to stream."
    [creds host file-path out-stream & [opts]]
    (io/copy (get-resource-file-as-stream creds host file-path opts)
             out-stream :buffer-size BUFFER-SIZE))

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
)

;;; ;;;;;;;;;;;;;;;;;;;;;;;
;;; Get Result
(comment

  (defn get-experiment-results
    "All results for an experiment (completed, not thumbnails), in most-recent-first order."
    [creds host e]
    (->> (e/experiment-result-uri e)
         (map #(get-result-uri creds host %))
         (remove r/result-metadata-thumb) ; HACK how to exclude thumbs in the query API?
         sort-by-id-desc)))
