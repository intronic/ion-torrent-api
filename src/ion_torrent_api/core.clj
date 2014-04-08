(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure.core :as core]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]))

(declare get-json get-completed-resource ensure-starts-with filter-latest-result
         plugin-result-type-map barcode-eas-map plugin-result-api-path-prefix
         file-name BUFFER-SIZE)


(defprotocol TorrentServerAPI
  "Torrent Server API calls."
  (experiments [this] [this opts] [this limit offset]
    "Get experiments (with options 'opts' or by limit and offset).")
  (experiment-name [this name] [this name opts]
    "Get experiment by name (with options 'opts').")
  (experiment [this] [this id-or-uri] [this id-or-uri opts]
    "Get experiment by id or uri (with options 'opts').")
  (result [this] [this id-or-uri] [this id-or-uri opts]
    "Get result by id or uri (with options 'opts').")
  (plugin-result [this] [this id-or-uri] [this id-or-uri opts]
    "Get plugin-result by id or uri (with options 'opts').")
  (barcode-set [this]
    "Set of barcodes.")
  (complete? [this])
  (coverage? [this]
    "Coverage analysis plugin.")
  (variant-caller? [this]
    "Variant caller plugin.")
  (bam-uri [this bc]
  "BAM uri for barcode.")
  (bai-uri [this bc]
  "BAM BAI uri for barcode.")
  (bam-header-uri [this bc]
    "BAM header uri for barcode.")
  (pdf-uri [this]
    "PDF report uri.")
  (tsvc-vcf-uri [this bc]
    "TorrentSuite VCF uri for barcode.")
  (tsvc-vcf-tbi-uri [this bc]
    "TorrentSuite VCF TBI uri for barcode.")
  (tsvc-target-bed-uri [this]
    "TorrentSuite target bed uri.")
  (coverage-ampl-uri [this bc]
    "Amplicon Coverage analysis uri for barcode."))


(defprotocol UniqueID
    "Unique Identifier"
    (unique-id [this]))


(defrecord Experiment [torrent-server id name pgm-name display-name uri run-type chip-type
                       result-uri-set dir status ftp-status sample-map barcode-sample-map date
                       latest-result-date latest-result raw-map]

  Object
  (toString [this] (pr-str this))

  TorrentServerAPI
  (barcode-set [this] (into #{} (keys barcode-sample-map)))
  (complete? [_] (= ["run" "Complete"] [status ftp-status]))
  (result [this]
    (result this nil {}))
  (result [this _]
    (result this nil {}))
  (result [this _ opts]
    (if latest-result
      latest-result
      (->> result-uri-set
           (map #(result torrent-server % opts))
           (filter-latest-result this)))))


(defrecord Result [torrent-server id name uri experiment-uri status
                   plugin-result-uri-set plugin-state-map analysis-version report-status plugin-store-map
                   bam-link fastq-link report-link filesystem-path reference
                   lib-metrics-uri-set tf-metrics-uri-set analysis-metrics-uri-set quality-metrics-uri-set
                   timestamp thumbnail? plugin-result-set raw-map]

  Object
  (toString [this] (pr-str this))

  TorrentServerAPI

  ;; HACK alternatively, more complicated but possibly less assumptions and safer?:-
  ;; eg: /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/download_links/IonXpress_009_R_2013_03_11_23_41_27_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7.bam
  ;; (let [bam (io/as-file (result "bamLink"))]
  ;;     (str (io/file (.getParent bam) "download_links" (str (name barcode) "_" (.getName bam)))))
  ;; eg:
  ;; /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/IonXpress_009_rawlib.bam
  (plugin-result [this]
    (plugin-result this nil {}))
  (plugin-result [this _]
    (plugin-result this nil {}))
  (plugin-result [this _ opts]
    (if plugin-result-set
      plugin-result-set
      (into #{} (map #(plugin-result torrent-server % opts) plugin-result-uri-set))))
  (bam-uri [_ bc]
    (str report-link (core/name bc) "_rawlib.bam"))
  (bai-uri [this bc]
    (str (bam-uri this bc) ".bai"))
  (bam-header-uri [this bc]
    (str (bam-uri this bc) ".header.sam"))
  (complete? [_] (= "Completed" report-status))
  (pdf-uri [_]
    (format "/report/latex/%d.pdf" id)))


(defrecord PluginResult [type torrent-server id uri result-uri result-name state path report-link
                         name version versioned-name
                         library-type config-desc barcode-result-map target-name target-bed experiment-name
                         trimmed-reads? barcoded? start-time end-time raw-map]

  Object
  (toString [this] (pr-str this))

  TorrentServerAPI
  (barcode-set [_] (into #{} (keys barcode-result-map)))
  (complete? [_] (= "Completed" state))
  (coverage? [_] (= :coverage type))
  (variant-caller? [_] (= :tsvc type))
  (tsvc-vcf-uri [this bc]
    (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/TSVC_variants.vcf.gz"))
  (tsvc-vcf-tbi-uri [this bc]
    (str (tsvc-vcf-uri this bc) ".tbi"))
  (tsvc-target-bed-uri [this]
    target-bed)
  (coverage-ampl-uri [this bc]
    (if-let [prefix (get-in barcode-result-map [(core/name bc) "Alignments"])]
      (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/" prefix ".amplicon.cov.xls"))))

;; path:       "/results/analysis/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"
;; reportLink: "/output/Home/XXX-24-YYY/"
;; API path:   "/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"


(extend-protocol TorrentServerAPI
  ;; base constructors for data from TorrentServer 'JSON String Keys' Maps
  clojure.lang.IPersistentMap

  (experiment [json-map]
    (let [main-keys [:torrent-server "id" "expName" "pgmName" "displayName" "resource_uri"
                     "runtype" "chipType"
                     "results" "expDir" "status" "ftpStatus"]
          exp-name (get json-map "expName")
          bc-samp (distinct (map #(get % "barcodedSamples") (get json-map "eas_set")))
          bc-samp-map (barcode-eas-map (first bc-samp))
          samp-map (into {} (map (juxt #(get % "displayedName") identity) (get json-map "samples")))
          date (inst/read-instant-date (get json-map "date"))
          result-date (inst/read-instant-date (get json-map "resultDate"))]
      (assert date "date required.")
      (assert result-date "resultDate required.")
      (assert (<= 0 (count bc-samp) 1)
              (str "Zero or one distinct EAS set expected. Found " (count bc-samp)
                   " in experiment: " exp-name " with results: " (pr-str (get json-map "results")) "."))
      (assert (= (into #{} (keys samp-map)) (into #{} (vals bc-samp-map)))
              (str "Samples dont match barcoded samples. Experiment: " exp-name
                   ", Samples: " (pr-str (into #{} (keys samp-map))) ", Barcode samples: " (pr-str bc-samp-map) "."))
      (apply ->Experiment (concat (map (partial get json-map) main-keys)
                                  ;; for now, work with one label per barcode and one eas_set per experiment
                                  [samp-map
                                   bc-samp-map
                                   date
                                   result-date
                                   nil
                                   (apply dissoc json-map "log" main-keys)]))))

  (result [json-map]
    (let [main-keys [:torrent-server "id" "resultsName" "resource_uri" "experiment" "status"
                     "pluginresults" "pluginState" "analysisVersion" "reportStatus" "pluginStore"
                     "bamLink" "fastqLink" "reportLink" "filesystempath" "reference"
                     "libmetrics" "tfmetrics" "analysismetrics" "qualitymetrics"]]
      (assert (seq (get json-map "timeStamp")) "timeStamp required.")
      (apply ->Result (concat (map (partial get json-map) main-keys)
                              [(inst/read-instant-date (get json-map "timeStamp"))
                               (boolean (get-in json-map ["metaData" "thumb"]))
                               nil
                               (apply dissoc json-map main-keys)]))))

  (plugin-result [json-map]
    (let [main-keys [:torrent-server "id" "resource_uri" "result" "resultName" "state"
                     "path" "reportLink"]]
      (assert (seq (get json-map "starttime")) "starttime required.")
      (assert (seq (get json-map "endtime")) "endtime required.")
      (apply ->PluginResult (concat [(plugin-result-type-map (get-in json-map ["plugin" "name"]))]
                                    (map (partial get json-map) main-keys)
                                    (map (partial get (get json-map "plugin")) ["name" "version" "versionedName"])
                                    (map (partial get (get json-map "store")) ["Library Type" "Configuration" "barcodes"
                                                                         "Target Regions" "targets_bed"
                                                                         "Aligned Reads" "Trim Reads"])
                                    [(.equalsIgnoreCase "true" (get-in json-map ["store" "barcoded"])) ; string -> boolean
                                     (inst/read-instant-date (get json-map "starttime"))
                                     (inst/read-instant-date (get json-map "endtime"))
                                     (apply dissoc json-map main-keys)])))))

(defrecord TorrentServer [server-url api-path]
  Object
  (toString [this] (pr-str this))

  TorrentServerAPI
  (experiments [this]
    (experiments this {}))
  (experiments [this limit offset]
    (experiments this {"limit" limit "offset" offset }))
  (experiments [this opts]
    (get-json this "experiment/" (merge {"status__exact" "run" "ftpStatus__exact" "Complete"} opts)))

  (experiment-name [this name]
    (experiment-name this name {}))
  (experiment-name [this name opts]
    ;; query by options returns map with "meta" and "objects" keys
    (let [{objects "objects" {total-count "total_count" :as meta} "meta"}
          (experiments this {"expName__exact" name "limit" 2})]
      (assert (and meta total-count) "Invalid experiment name query response.")
      (assert (<= 0 total-count 1) (str "More than one experiment ("
                                        total-count ") for name[" name "]."))
      (let [{:keys [recurse?]} opts]
        (if-let [json (first objects)]
          (let [e (experiment (assoc json :torrent-server this))
                r (if recurse? (result e nil opts))]
            (assoc e :latest-result r))))))

  (experiment [this id-or-uri]
    (experiment this id-or-uri {}))
  (experiment [this id-or-uri opts]
    (let [{:keys [recurse?]} opts
          json (get-completed-resource this (ensure-starts-with (str (:api-path this) "experiment/")
                                                                (str id-or-uri)))
          e (experiment (assoc json :torrent-server this))
          r (if recurse? (result e nil opts))]
      (assoc e :latest-result r)))

  (result [this id-or-uri]
    (result this id-or-uri {}))
  (result [this id-or-uri opts]
    (let [{:keys [recurse?]} opts
          json (get-completed-resource this (ensure-starts-with (str (:api-path this) "results/")
                                                                (str id-or-uri)))
          r (result (assoc json :torrent-server this))
          pr-set (if recurse? (plugin-result r))]
      (assoc r :plugin-result-set pr-set)))

  (plugin-result [this id-or-uri]
    (plugin-result this id-or-uri {}))
  (plugin-result [this id-or-uri opts]
    (let [json (get-completed-resource this (ensure-starts-with (str (:api-path this) "pluginresult/")
                                                                (str id-or-uri)))]
      (plugin-result (assoc json :torrent-server this)))))


(defn torrent-server [server-url & {:keys [creds api-path] :or {api-path "/rundb/api/v1/"}}]
  ;; creds are attached to record as metadata
  (TorrentServer. server-url api-path {:creds creds} nil))


(def data-readers
  {'ion_torrent_api.core.TorrentServer ion-torrent-api.core/map->TorrentServer
   'ion_torrent_api.core.Experiment ion-torrent-api.core/map->Experiment
   'ion_torrent_api.core.Result ion-torrent-api.core/map->Result
   'ion_torrent_api.core.PluginResult ion-torrent-api.core/map->PluginResult})

(def ^:private plugin-result-type-map {"variantCaller" :tsvc
                                       "coverageAnalysis" :coverage})

(defn- plugin-result-api-path-prefix
  "Direct path to pluginresult files through API."
  [plugin-result]
  (let [^String path (:filesystem-path plugin-result)
        ^String link (:report-link plugin-result)]
    (subs path (.indexOf path link))))

(defn- barcode-eas-map [m]
  (into {} (map (fn [[label {bc "barcodes"}]]
                  (assert (= 1 (count bc)) "Exactly 1 barcode per sample expected.")
                  [(first bc) label])
                m)))

(defn filter-latest-result
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
                          {:as :json-string-keys :basic-auth (:creds (meta ts)) :query-params opts}))))

(defn- get-completed-resource
  "Get resources with Completed status."
  [ts resource & [opts]]
  (get-json ts resource (assoc opts "status__startswith" "Completed")))

(defn- get-resource-file
  "Return a file from host."
  [ts file-path]
  (:body (io! (client/get (str (:server-url ts) file-path)
                          {:basic-auth (:creds (meta ts))}))))


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
    (and f (.. (io/as-file f) getName)))

;;;
(comment

  (defn newest-variant-caller-plugin-result
    "Get the newest completed variantCaller if any from a collection of plugin-results."
    [pr-coll]
    (some pr/plugin-result-variant-caller? pr-coll))

  (defn newest-coverage-plugin-result
    "Get the newest completed variantCaller if any from a collection of plugin-results."
    [pr-coll]
    (some pr/plugin-result-coverage? pr-coll))

  (defn get-experiment-results
    "All results for an experiment (completed, not thumbnails), in most-recent-first order."
    [creds host e]
    (->> (e/experiment-result-uri e)
         (map #(get-result-uri creds host %))
         (remove r/result-metadata-thumb) ; HACK how to exclude thumbs in the query API?
         sort-by-id-desc)))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;
;;; Query Torrent Server API

;;; generic calls for resources and resource files

(comment
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

(comment
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
)
