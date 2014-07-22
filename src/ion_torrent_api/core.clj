(ns ion-torrent-api.core
  (:require [clj-http.client :as client]
            [clojure [core :as core] [set :as set]]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [slingshot.slingshot :refer (try+ throw+)]
            [schema.core :as s]
            [schema.macros :as sm]))

(declare get-json ensure-starts-with filter-latest-result plugin-result-type-map
         barcode-eas-map plugin-result-api-path-prefix
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
  (lib-metrics [this] [this id-or-uri] [this id-or-uri opts]
    "Get lib-metrics by id or uri (with options 'opts').")
  (tf-metrics [this] [this id-or-uri] [this id-or-uri opts]
    "Get tf-metrics by id or uri (with options 'opts').")
  (analysis-metrics [this] [this id-or-uri] [this id-or-uri opts]
    "Get analysis-metrics by id or uri (with options 'opts').")
  (quality-metrics [this] [this id-or-uri] [this id-or-uri opts]
    "Get quality-metrics by id or uri (with options 'opts').")
  (barcode-set [this] [this exp]
    "Set of barcodes.")
  (barcode-map [this] [this exp]
    "Map of barcodes to values.")
  (complete? [this])
  (coverage? [this]
    "Coverage analysis plugin.")
  (coverage [this]
    "Coverage analysis plugin.")
  (variant-caller? [this]
    "Variant caller plugin.")
  (variant-caller [this]
    "Variant caller plugin.")
  (sample-id? [this]
    "Sample ID plugin.")
  (sample-id [this]
    "Sample ID plugin.")
  (bam-uri [this bc]
  "BAM uri for barcode.")
  (bai-uri [this bc]
  "BAM BAI uri for barcode.")
  (bam-header-uri [this bc]
    "BAM header uri for barcode. ")
  (pdf-uri [this]
    "PDF report uri.")
  (tsvc-vcf-uri [this bc]
    "TorrentSuite VCF uri for barcode.")
  (tsvc-vcf-tbi-uri [this bc]
    "TorrentSuite VCF TBI uri for barcode.")
  (tsvc-variants-xls-uri [this bc]
    "TorrentSuite variants XLS uri for barcode.")
  (tsvc-alleles-xls-uri [this bc]
    "TorrentSuite alleles XLS uri for barcode.")
  (tsvc-target-bed-uri [this]
    "TorrentSuite target bed uri.")
  (coverage-ampl-uri [this bc]
    "Amplicon Coverage analysis uri for barcode."))

(sm/defrecord TorrentServer
    [server-url :- s/Str
     version :- s/Keyword
     api-path :- s/Str]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord PluginResult
    [type :- (s/maybe s/Keyword)
     torrent-server :- TorrentServer
     id :- s/Int
     uri :- s/Str
     result-uri :- s/Str
     result-name :- s/Str
     state :- s/Str
     path :- s/Str
     report-link :- s/Str
     name :- s/Str
     version :- s/Str
     versioned-name :- s/Str
     library-type :- (s/maybe s/Str)
     config-desc :- (s/maybe s/Str)
     target-name :- (s/maybe s/Str)
     target-bed :- (s/maybe s/Str)
     experiment-name :- (s/maybe s/Str)
     trimmed-reads? :- (s/maybe s/Bool)
     barcode-result-map :- (s/maybe {s/Str s/Any})
     barcoded? :- s/Bool
     start-time :- s/Inst
     end-time :- s/Inst
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord Result
    [torrent-server :- TorrentServer
     id :- s/Int
     name :- s/Str
     uri :- s/Str
     experiment-uri :- s/Str
     status :- s/Str
     plugin-result-uri-set :- [s/Str]
     plugin-state-map :- {s/Str s/Str}
     analysis-version :- s/Str
     report-status :- s/Str
     plugin-store-map :- {s/Str {s/Str s/Any}}
     bam-link :- s/Str
     fastq-link :- s/Str
     report-link :- s/Str
     filesystem-path :- s/Str
     reference :- s/Str
     lib-metrics-uri-set :- [s/Str]
     tf-metrics-uri-set :- [s/Str]
     analysis-metrics-uri-set :- [s/Str]
     quality-metrics-uri-set :- [s/Str]
     timestamp :- s/Inst
     thumbnail? :- s/Bool
     plugin-result-set :- (s/maybe #{PluginResult})
     lib-metrics-set :- (s/maybe #{{s/Any s/Any}})
     tf-metrics-set :- (s/maybe #{{s/Any s/Any}})
     analysis-metrics-set :- (s/maybe #{{s/Any s/Any}})
     quality-metrics-set :- (s/maybe #{{s/Any s/Any}})
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord Experiment
    [torrent-server :- TorrentServer
     id :- s/Int
     name :- s/Str
     pgm-name :- s/Str
     display-name :- s/Str
     uri :- s/Str
     run-type :- s/Str
     chip-type :- s/Str
     result-uri-set :- [s/Str]
     dir :- s/Str
     status :- s/Str
     ftp-status :- s/Str
     sample-map :- {s/Str {s/Any s/Any}}
     barcode-sample-map :- {s/Str s/Str}
     date :- s/Inst
     latest-result-date :- s/Inst
     latest-result :- (s/maybe Result)
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))

(def data-readers
  {'ion_torrent_api.schema.TorrentServer ion-torrent-api.core/map->TorrentServer
   'ion_torrent_api.schema.Experiment    ion-torrent-api.core/map->Experiment
   'ion_torrent_api.schema.Result        ion-torrent-api.core/map->Result
   'ion_torrent_api.schema.PluginResult  ion-torrent-api.core/map->PluginResult
   'ion_torrent_api.core.TorrentServer   ion-torrent-api.core/map->TorrentServer
   'ion_torrent_api.core.Experiment      ion-torrent-api.core/map->Experiment
   'ion_torrent_api.core.Result          ion-torrent-api.core/map->Result
   'ion_torrent_api.core.PluginResult    ion-torrent-api.core/map->PluginResult})

(defn torrent-server
  [server-url & {:keys [creds version api-path] :or {version :v1}}]
  ;; creds are attached to record as metadata
  (TorrentServer. server-url version (or api-path ({:v1 "/rundb/api/v1/"} version))
                  {:creds creds} nil))

(extend-protocol TorrentServerAPI

  ion_torrent_api.core.Experiment
  (barcode-set [this] (into #{} (keys (barcode-map this))))
  (barcode-map [this] (-> this :barcode-sample-map))
  (complete? [this] (= ["run" "Complete"] (-> this ((juxt :status :ftp-status)))))
  (result
    ([this] (result this nil {}))
    ([this _] (result this nil {}))
    ([this _ opts]
       (or (-> this :latest-result)
           (->> this
                :result-uri-set
                (map #(result (:torrent-server this) % opts))
                (filter-latest-result this)))))
  (variant-caller [this]
    (->> this :latest-result :plugin-result-set (filter variant-caller?)))
  (coverage [this]
    (->> this :latest-result :plugin-result-set (filter coverage?)))
  (sample-id [this]
    (->> this :latest-result :plugin-result-set (filter sample-id?)))

  ion_torrent_api.core.Result

  (barcode-set     ;    "Set of all barcodes found in any plugin or experiment."
    ([this]
       (->> this :plugin-result-set (map barcode-set) (reduce set/union)))
    ([this exp]
       (into (barcode-set exp) (barcode-set this))))
  ;; HACK alternatively, more complicated but possibly less assumptions and safer?:-
  ;; eg: /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/download_links/IonXpress_009_R_2013_03_11_23_41_27_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7.bam
  ;; (let [bam (io/as-file (result "bamLink"))]
  ;;     (str (io/file (.getParent bam) "download_links" (str (name barcode) "_" (.getName bam)))))
  ;; eg:
  ;; /output/Home/Auto_user_XXX-6-Ion_AmpliSeq_Comprehensive_Cancer_Panel_7_011/IonXpress_009_rawlib.bam
  (plugin-result
    ([this] (plugin-result this nil {}))
    ([this _] (plugin-result this nil {}))
    ([this _ opts]
       (or (-> this :plugin-result-set)
           (into #{} (map #(plugin-result (-> this :torrent-server) % opts)
                          (-> this :plugin-result-uri-set))))))
  (lib-metrics
    ([this] (lib-metrics this nil {}))
    ([this _] (lib-metrics this nil {}))
    ([this _ opts]
       (or (-> this :lib-metrics-set)
           (into #{} (map #(lib-metrics (-> this :torrent-server) % opts)
                          (-> this :lib-metrics-uri-set))))))
  (tf-metrics
    ([this] (tf-metrics this nil {}))
    ([this _] (tf-metrics this nil {}))
    ([this _ opts]
       (or (-> this :tf-metrics-set)
           (into #{} (map #(tf-metrics (-> this :torrent-server) % opts)
                          (-> this :tf-metrics-uri-set))))))
  (analysis-metrics
    ([this] (analysis-metrics this nil {}))
    ([this _] (analysis-metrics this nil {}))
    ([this _ opts]
       (or (-> this :analysis-metrics-set)
           (into #{} (map #(analysis-metrics (-> this :torrent-server) % opts)
                          (-> this :analysis-metrics-uri-set))))))
  (quality-metrics
    ([this] (quality-metrics this nil {}))
    ([this _] (quality-metrics this nil {}))
    ([this _ opts]
       (or (-> this :quality-metrics-set)
           (into #{} (map #(quality-metrics (-> this :torrent-server) % opts)
                          (-> this :quality-metrics-uri-set))))))
  (bam-uri [this bc] (str (-> this :report-link) (core/name bc) "_rawlib.bam"))
  (bai-uri [this bc] (str (bam-uri this bc) ".bai"))
  (bam-header-uri [this bc] (str (bam-uri this bc) ".header.sam"))
  (complete? [this] (= "Completed" (-> this :status)))
  (pdf-uri [this] (format "/report/latex/%d.pdf" (-> this :id)))

  ion_torrent_api.core.PluginResult

  (barcode-set
    ([this] (into #{} (keys (barcode-map this))))
    ([this exp] (into #{} (keys (barcode-map this exp)))))
  (barcode-map
    ([this] (-> this :barcode-result-map))
    ([this exp] (select-keys (barcode-map this) (if (set? exp) exp (barcode-set exp)))))
  (complete? [this] (= "Completed" (-> this :state)))
  (coverage? [this] (= :coverage (-> this :type)))
  (variant-caller? [this] (= :tsvc (-> this :type)))
  (sample-id? [this] (= :sample-id (-> this :type)))
  (bam-uri [this bc]
    (if (variant-caller? this)
      (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/" (core/name bc)
           "_rawlib" (if (-> this :trimmed-reads?) "_PTRIM") ".bam")))
  (bai-uri [this bc]
    (if (variant-caller? this)
      (str (bam-uri this bc) ".bai")))
  (tsvc-vcf-uri [this bc]
    (if (variant-caller? this)
      (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/TSVC_variants.vcf.gz")))
  (tsvc-vcf-tbi-uri [this bc]
    (if (variant-caller? this)
      (str (tsvc-vcf-uri this bc) ".tbi")))
  (tsvc-variants-xls-uri [this bc]
    (if (variant-caller? this)
      (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/variants.xls")))
  (tsvc-alleles-xls-uri [this bc]
    (if (variant-caller? this)
      (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/alleles.xls")))
  (tsvc-target-bed-uri [this]
    (if (variant-caller? this)
      (str (plugin-result-api-path-prefix this) "/" (-> this :target-name) ".bed")))
  (coverage-ampl-uri [this bc]
    (if (coverage? this)
      (if-let [prefix (get-in (-> this :barcode-result-map) [(core/name bc) "Alignments"])]
        (str (plugin-result-api-path-prefix this) "/" (core/name bc) "/" prefix ".amplicon.cov.xls"))))

  ;; path:       "/results/analysis/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"
  ;; reportLink: "/output/Home/XXX-24-YYY/"
  ;; API path:   "/output/Home/XXX-24-YYY/plugin_out/coverageAnalysis_out"

  ion_torrent_api.core.TorrentServer

  (experiments
    ([this] (experiments this {}))
    ([this opts]
       (get-json this "experiment/" (merge {"status__exact" "run" "ftpStatus__exact" "Complete"} opts)))
    ([this opts name]
       (experiments this (merge opts (if name {(str "expName__"
                                                    (if (some #(Character/isUpperCase ^Character %) (seq name))
                                                      "contains"
                                                      "icontains")) name})))))

  (experiment-name
    ([this name] (experiment-name this name {}))
    ([this name opts]
       ;; query by options returns map with "meta" and "objects" keys
       ;; if name has any upper case char then do case-sensitive search
       (let [{objects "objects" {total-count "total_count" :as meta} "meta"}
             (experiments this {(str "expName__" (if (some #(Character/isUpperCase ^Character %) (seq name))
                                                   "contains"
                                                   "icontains")) name "limit" 2})]
         (assert (and meta total-count) "Invalid experiment name query response.")
         (assert (not (> total-count 1)) (str "More than one experiment matching name '" name "': "
                                              (pr-str (map #(get % "expName") objects))))
         (assert (not (zero? total-count)) (str "No experiments matching name '" name "'."))
         (let [{:keys [recurse?]} opts]
           (if-let [json (first objects)]
             (let [e (experiment (assoc json :torrent-server this))
                   r (if recurse? (result e nil opts))]
               (assoc e :latest-result r)))))))

  (experiment
    ([this id-or-uri] (experiment this id-or-uri {}))
    ([this id-or-uri opts]
       (let [{:keys [recurse?]} opts
             json (get-json this (ensure-starts-with (str (:api-path this) "experiment/")
                                                     (str id-or-uri)))]
         (let [ e (experiment (assoc json :torrent-server this))]
           (merge e (if recurse? {:latest-result (result e nil opts)}))))))

  (result
    ([this id-or-uri] (result this id-or-uri {}))
    ([this id-or-uri opts]
       (let [{:keys [recurse?]} opts
             json (get-json this (ensure-starts-with (str (:api-path this) "results/")
                                                     (str id-or-uri)))
             r (result (assoc json :torrent-server this))]
         (merge r (when recurse?
                    {:plugin-result-set (plugin-result r)
                     :lib-metrics-set (into #{} (map #(lib-metrics this % opts)
                                                     (:lib-metrics-uri-set r)))
                     :tf-metrics-set (into #{} (map #(tf-metrics this % opts)
                                                    (:tf-metrics-uri-set r)))
                     :analysis-metrics-set (into #{} (map #(analysis-metrics this % opts)
                                                          (:analysis-metrics-uri-set r)))
                     :quality-metrics-set (into #{} (map #(quality-metrics this % opts)
                                                         (:quality-metrics-uri-set r)))})))))

  (plugin-result
    ([this id-or-uri]
       (plugin-result this id-or-uri {}))
    ([this id-or-uri opts]
       (let [json (get-json this (ensure-starts-with (str (:api-path this) "pluginresult/")
                                                     (str id-or-uri)))]
         (plugin-result (assoc json :torrent-server this)))))

  (lib-metrics
    ([this id-or-uri] (lib-metrics this id-or-uri {}))
    ([this id-or-uri opts]
       (get-json this (ensure-starts-with (str (:api-path this) "libmetrics/")
                                          (str id-or-uri)))))
  (tf-metrics
    ([this id-or-uri] (tf-metrics this id-or-uri {}))
    ([this id-or-uri opts]
       (get-json this (ensure-starts-with (str (:api-path this) "tfmetrics/")
                                          (str id-or-uri)))))
  (analysis-metrics
    ([this id-or-uri] (analysis-metrics this id-or-uri {}))
    ([this id-or-uri opts]
       (get-json this (ensure-starts-with (str (:api-path this) "analysismetrics/")
                                          (str id-or-uri)))))
  (quality-metrics
    ([this id-or-uri] (quality-metrics this id-or-uri {}))
    ([this id-or-uri opts]
       (get-json this (ensure-starts-with (str (:api-path this) "qualitymetrics/")
                                          (str id-or-uri)))))

  ;; base constructors for data from TorrentServer 'JSON String Keys' Maps
  clojure.lang.APersistentMap

  (experiment [json-map]
    (let [main-keys [:torrent-server "id" "expName" "pgmName" "displayName" "resource_uri"
                     "runtype" "chipType"
                     "results" "expDir" "status" "ftpStatus"]
          id (get json-map "id")
          exp-name (get json-map "expName")
          bc-samp (distinct (map #(get % "barcodedSamples") (get json-map "eas_set")))
          bc-samp-map (barcode-eas-map (first bc-samp))
          samp-map (into {} (map (juxt #(get % "displayedName") identity) (get json-map "samples")))
          date (get json-map "date")
          result-date (get json-map "resultDate")]
      (assert id "id required.")
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
                                      (inst/read-instant-date date)
                                      (inst/read-instant-date result-date)
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
                                  nil nil nil nil
                                  (apply dissoc json-map main-keys)]))))

  (plugin-result [json-map]
    (let [main-keys [:torrent-server "id" "resource_uri" "result" "resultName" "state"
                     "path" "reportLink"]
          bc-map (get-in json-map ["store" "barcodes"])
          ;; api changed between v3 and v4
          name (or (get-in json-map ["plugin" "name"])
                   (get json-map "pluginName"))
          version  (or (get-in json-map ["plugin" "version"])
                       (get json-map "pluginVersion"))
          versioned-name (or (get-in json-map ["plugin" "versionedName"])
                             (str name "--v" version))
          type (plugin-result-type-map name)]
      (assert (seq (get json-map "starttime")) "starttime required.")
      (assert (seq (get json-map "endtime")) "endtime required.")
      (apply ->PluginResult (concat [type]
                                    (map (partial get json-map) main-keys)
                                    [name version versioned-name]
                                    (map (partial get (get json-map "store")) ["Library Type" "Configuration"
                                                                               "Target Regions" "targets_bed"
                                                                               "Aligned Reads" "Trim Reads"])
                                    [(if (= "sampleID" name)
                                       (fmap #(get % "SampleID") bc-map)
                                       bc-map)
                                     (.equalsIgnoreCase "true" (get-in json-map ["store" "barcoded"])) ; string -> boolean
                                     (inst/read-instant-date (get json-map "starttime"))
                                     (inst/read-instant-date (get json-map "endtime"))
                                     (apply dissoc json-map main-keys)]))))

  (barcode-map [this bc-map]
    (select-keys this bc-map))

  (barcode-set [this] (into #{} (keys this)))

  clojure.lang.APersistentSet
  (barcode-set [this] this)

  clojure.lang.ISeq
  (barcode-set [this] (into #{} this)))


(def ^:private plugin-result-type-map {"variantCaller" :tsvc
                                       "coverageAnalysis" :coverage
                                       "sampleID" :sample-id})

(defn- plugin-result-api-path-prefix
  "Direct path to pluginresult files through API."
  [plugin-result]
  (let [^String path (:path plugin-result)
        ^String link (:report-link plugin-result)]
    (subs path (.indexOf path link))))

(defn- barcode-eas-map [m]
  (into {} (map (fn [[label {bc "barcodes"}]]
                  (assert (= 1 (count bc)) "Exactly 1 barcode per sample expected.")
                  [(first bc) label])
                m)))

(defn filter-latest-result
  "Get the newest, completed, non-thumbnail result matching the experiment from a collection of results."
  [exp res-coll]
  (let [date (.getTime ^java.util.Date (:latest-result-date exp))
        r-coll (filter #(and (<= date (.getTime ^java.util.Date (:timestamp %)))
                             (complete? %)
                             (not (:thumbnail? %)))
                       res-coll)
        res (first r-coll)]
    (if-not (<= 0 (count r-coll) 1)
      (throw+ {:data {:exp-id (:id exp) :exp-name (:name exp) :count (count r-coll) :res (mapv :id r-coll)}
               :msg "0 or 1 latest results expected."}))
    (if (:thumbnail? res)  (throw+ {:data {:res-id (:id res) :res-name (:name res) :exp-id (:id exp) :exp-name (:name exp)}
                                    :msg "Latest result is thumbnail."}))
    res))

;;;

(defn get-json
  "Return a JSON resource from host.
Keys are not coerced to keywords as the JSON keys can have spaces in them which are invalid as keywords and not printable+readable.
host should "
  [ts resource & [opts]]
  (:body (io! (client/get (str (:server-url ts) (ensure-starts-with (:api-path ts) resource))
                          {:as :json-string-keys :basic-auth (:creds (meta ts)) :query-params opts}))))

(defn get-file
  "Return a file as bytes from host."
  [ts file-path]
  (:body (io! (client/get (str (:server-url ts) file-path)
                          {:basic-auth (:creds (meta ts))}))))

(defn get-file-as-stream
  "Get a file from host as a stream."
  [ts file-path & [opts]]
  (:body (io! (client/get (str (:server-url ts) file-path)
                          {:as :stream :basic-auth (:creds (meta ts)) :query-params opts}))))

(defn get-file-to-stream
  "Get a file from host and copy to stream."
  [ts file-path out-stream & [opts]]
  (io/copy (get-file-as-stream ts file-path opts)
           out-stream :buffer-size BUFFER-SIZE))

(defn get-file-to-file
  "Get a file from host to local file. Deletes the local file if an exception occurs."
  [ts file-path dest-file & [opts]]
  (try
    (with-open [out (io/output-stream dest-file)]
      (get-file-to-stream ts file-path out opts)
      dest-file)
    (catch Exception e
      (io/delete-file dest-file)
      (throw e))))

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
