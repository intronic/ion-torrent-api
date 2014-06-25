(ns ion-torrent-api.expectations.core
  (:require [expectations :refer :all]
            [ion-torrent-api.schema :as sc]
            [ion-torrent-api.core :refer :all :as ion]
            [ion-torrent-api.expectations.util :refer :all]
            [ion-torrent-api.expectations.schema :refer :all]
            [clj-http.client :as client]
            [clj-http.fake :refer :all]
            [clojure [string :as str] [edn :as edn]])
  (:import [ion_torrent_api.schema Experiment Result PluginResult TorrentServer]))

(def creds ["user" "pass"])
(def host "http://my-intranet-torrent-server.com")
(def ts (torrent-server host :creds creds))

(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                              :version :v1 :api-path "/rundb/api/v1/"}
        ts)

(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                              :version :v1 :api-path "/rundb/api/v1/"}
        (torrent-server host :version :v1))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                              :version :v1 :api-path "/some/other/path"}
        (torrent-server host :version :v1 :api-path "/some/other/path"))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                              :version :v2 :api-path nil}
        (torrent-server host :version :v2))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                              :version :v2 :api-path "/some/other/path"}
        (torrent-server host :version :v2 :api-path "/some/other/path"))

(expect creds (:creds (meta ts)))
(expect "/rundb/api/v1/" (:api-path ts))
(expect (torrent-server "h")
        (torrent-server "h"))
(expect (torrent-server "h")
        (read-string (str (torrent-server "h"))))
(expect-let [ts (torrent-server "h" :api-path "p")]
            ts
            (edn/read-string {:readers sc/data-readers} (str ts)))
(expect-let [ts (torrent-server "h" :api-path "p")]
            ts
            (read-string (str ts)))

;;; utilities

;;; Note: test private functions by accessing vars directly
(expect "abcdef" (#'ion/ensure-starts-with "abc" "def"))
(expect "abcxabcdef" (#'ion/ensure-starts-with "abc" "xabcdef"))
(expect "abcdef" (#'ion/ensure-starts-with "abc" "abcdef"))

(expect "/rundb/api/v1/results/99/" (#'ion/ensure-starts-with (str "/rundb/api/v1/" "results/") (str 99 "/")))

(expect creds (:creds (meta ts)))
(expect "/rundb/api/v1/" (:api-path ts))

(expect "f1" (file-name "/path/to/f1"))
(expect "f1.bed" (file-name "/path/../to//f1.bed"))

;;; Reading from dummy torrent server
(expect {:status 200 :body "12345"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {} :body "12345"})}
              (client/get (str host  "/rundb/api/v1/")))))

(expect {:status 200}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {} :body (slurp (uri-to-file uri :json))})}
              (client/get (str host  "/rundb/api/v1/" "experiment/schema/")))))

(expect 9648
        (count (:body (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {} :body (slurp (uri-to-file uri :json))})}

                        (client/get (str host  "/rundb/api/v1/" "experiment/schema/"))))))

;;; test meta stuff
(expect {"meta" {"limit" 20 "total_count" 1 "next" nil "previous" nil "offset" 0}}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (get-json ts "experiment/name-XXX-24"))))

;;; test object
(expect {"expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (first (get (with-fake-routes-in-isolation
                          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                   {:status 200 :headers {"Content-Type" "application/json"}
                                                    :body (slurp (uri-to-file uri :json))})}
                          (get-json ts "experiment/name-XXX-24"))
                        "objects"))))

(expect {"expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (first (get (with-fake-routes-in-isolation
                          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                   {:status 200 :headers {"Content-Type" "application/json"}
                                                    :body (slurp (uri-to-file uri :json))})}
                          (get-json ts "experiment/name-XXX-24"))
                        "objects"))))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TorrentServerAPI Protocol
;;; experiments

(expect (more-of x
                 "R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71" (:name x))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (do (experiment ts 95 {:recurse? false}))))


(expect {"limit" 20, "next" "/rundb/api/v1/experiment/?limit=20&offset=20", "offset" 0, "previous" nil, "total_count" 77}
        (get (with-fake-routes-in-isolation
               {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                        {:status 200 :headers {"Content-Type" "application/json"}
                                         :body (slurp (uri-to-file uri :json))})}
               (experiments ts))
             "meta"))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (experiments ts))
                    "objects")))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (experiments ts {"some" "opts"}))
                    "objects")))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (experiments ts {} "namefilter"))
                    "objects")))

;;; experiment-name

(expect '(:torrent-server :id :name :pgm-name :display-name :uri :run-type :chip-type :result-uri-set :dir :status :ftp-status :sample-map :barcode-sample-map :date :latest-result-date :latest-result :raw-map)
        (keys
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
           (experiment-name ts "name-XXX-24"))))

(expect {:pgm-name "XXXNPROTON" :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
              (experiment-name ts "name-XXX-24"))))

;;; experiment
(expect {"expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (first (get (with-fake-routes-in-isolation
                          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                   {:status 200 :headers {"Content-Type" "application/json"}
                                                    :body (slurp (uri-to-file uri :json))})}
                          (get-json ts "experiment/name-XXX-24"))
                        "objects"))))

(def e50 (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (experiment ts 50)))

(expect {:pgm-name "XXXNPROTON" :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in e50))


(expect {:pgm-name "XXXNPROTON" :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (experiment ts "/rundb/api/v1/experiment/50/"))))

(expect {:pgm-name "XXXNPROTON" :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (experiment ts "/rundb/api/v1/experiment/50/" {}))))

(expect-let [x e50]
            x (edn/read-string {:readers sc/data-readers} (str x)))

(expect-let [x e50]
            x (read-string (str x)))

(expect-let [e e50]
            #ion_torrent_api.schema.Experiment{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                               :id 50,
                                               :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                               :pgm-name "XXXNPROTON",
                                               :display-name "user XXX-24-AmpliSeq CCP 24",
                                               :uri "/rundb/api/v1/experiment/50/",
                                               :run-type "AMPS", :chip-type "900",
                                               :sample-map {"inq-037-me" {"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil}
                                                            "inq-052-tt" {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil}
                                                            "inq-024-me" {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil}
                                                            "inq-022-me" {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil}
                                                            "inq-025-tt" {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}},
                                               :result-uri-set ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"],
                                               :dir "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                               :status "run",
                                               :ftp-status "Complete",
                                               :barcode-sample-map {"IonXpressRNA_003" "inq-022-me", "IonXpressRNA_004" "inq-024-me", "IonXpressRNA_005" "inq-025-tt", "IonXpressRNA_002" "inq-037-me", "IonXpressRNA_001" "inq-052-tt"},
                                               :date #inst "2013-06-03T13:31:54.000-00:00",
                                               :latest-result-date #inst "2013-07-23T00:32:14.000-00:00",
                                               :raw-map {"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400, "baselineRun" false, "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/", "sample" "inq-037-me", "resultDate" "2013-07-23T00:32:14.000226+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-06-03T13:31:54+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "", "libraryKey" "TCAG", "thumbnailbasecallerargs" "", "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment", "userInput" "", "version" "3.6.56201"}}, "thumbnailanalysisargs" "", "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]}, "inq-024-me" {"barcodes" ["IonXpressRNA_004"]}, "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]}, "inq-037-me" {"barcodes" ["IonXpressRNA_002"]}, "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}}, "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-06-04T03:26:53.000155+00:00", "status" "run", "thumbnailalignmentargs" "", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47, "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "", "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}
            (edn/read-string {:readers sc/data-readers} (str e)))

;;; result

(expect "/rundb/api/v1/results/77"
        (let [id-or-uri 77] (#'ion/ensure-starts-with (str (:api-path ts) "results/")
                                                      (str id-or-uri))))
(expect {"id" 77}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (get-json ts "results/77"))))

(def r77 (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (result ts 77)))

(expect ion_torrent_api.schema.Result r77)

(expect #ion_torrent_api.schema.Result{:torrent-server
                                       #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"},
                                       :id 77,
                                       :name "24_reanalyze",
                                       :uri "/rundb/api/v1/results/77/",
                                       :experiment-uri "/rundb/api/v1/experiment/50/",
                                       :status "Completed",
                                       :plugin-result-uri-set ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"],
                                       :plugin-state-map {"IonReporterUploader" "Completed", "variantCaller" "Completed"},
                                       :analysis-version "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,",
                                       :report-status "Nothing",
                                       :plugin-store-map {"IonReporterUploader" {}, "variantCaller" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}},
                                       :bam-link "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam",
                                       :fastq-link "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq",
                                       :report-link "/output/Home/24_reanalyze_077/",
                                       :filesystem-path "/results/analysis/output/Home/24_reanalyze_077",
                                       :reference "hg19",
                                       :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/68/"],
                                       :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/68/"],
                                       :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/74/"],
                                       :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/74/"],
                                       :timestamp #inst "2013-07-23T05:18:31.000-00:00",
                                       :thumbnail? false,
                                       :plugin-result-set nil,
                                       :lib-metrics-set nil,
                                       :tf-metrics-set nil,
                                       :analysis-metrics-set nil,
                                       :quality-metrics-set nil,
                                       :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "3XNXT", "metaData" {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}
        r77)

(expect #ion_torrent_api.schema.Result{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 77, :name "24_reanalyze", :uri "/rundb/api/v1/results/77/", :experiment-uri "/rundb/api/v1/experiment/50/", :status "Completed", :plugin-result-uri-set ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"], :plugin-state-map {"IonReporterUploader" "Completed", "variantCaller" "Completed"}, :analysis-version "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,", :report-status "Nothing", :plugin-store-map {"IonReporterUploader" {}, "variantCaller" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}}, :bam-link "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam", :fastq-link "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq", :report-link "/output/Home/24_reanalyze_077/", :filesystem-path "/results/analysis/output/Home/24_reanalyze_077", :reference "hg19", :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/68/"], :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/68/"], :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/74/"], :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/74/"], :timestamp #inst "2013-07-23T05:18:31.000-00:00", :thumbnail? false, :plugin-result-set #{#ion_torrent_api.schema.PluginResult{:type :tsvc, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 209, :uri "/rundb/api/v1/pluginresult/209/", :result-uri "/rundb/api/v1/results/77/", :result-name "24_reanalyze", :state "Completed", :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out", :report-link "/output/Home/24_reanalyze_077/", :name "variantCaller", :version "4.0-r76860", :versioned-name "variantCaller--v4.0-r76860", :library-type "AmpliSeq", :config-desc "Somatic - Proton - Low Stringency", :target-name "4477685_Comprehensive_CCP_bedfile_20120517", :target-bed "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", :experiment-name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", :trimmed-reads? true, :barcode-result-map {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, :barcoded? true, :start-time #inst "2014-02-17T05:50:42.000-00:00", :end-time #inst "2014-02-17T09:37:51.000-00:00", :raw-map {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}} #ion_torrent_api.schema.PluginResult{:type nil, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 89, :uri "/rundb/api/v1/pluginresult/89/", :result-uri "/rundb/api/v1/results/77/", :result-name "24_reanalyze", :state "Completed", :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/IonReporterUploader_out", :report-link "/output/Home/24_reanalyze_077/", :name "IonReporterUploader", :version "3.6.2-r62833", :versioned-name "IonReporterUploader--v3.6.2-r62833", :library-type nil, :config-desc nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcode-result-map nil, :barcoded? false, :start-time #inst "2013-07-30T13:50:08.000-00:00", :end-time #inst "2013-07-30T13:50:55.000-00:00", :raw-map {"size" "173732", "store" {}, "config" {}, "endtime" "2013-07-30T13:50:55.000586+00:00", "inodes" "119", "starttime" "2013-07-30T13:50:08.000084+00:00", "owner" {"last_login" "2014-04-01T05:48:53.000369+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "IonReporterUploader--v3.6.2-r62833", "config" {"port" "443", "protocol" "https", "server" "dataloader.ionreporter.iontorrent.com", "token" "njmr5ajT9xg85qdT3g4WLZH04FJEj89j7G2CeMmesrABrJirGBWYc2qgzEcsMK5gieZU9OgSgSIA5JGWsxQJaw==", "version" "IR16"}, "path" "", "active" false, "autorunMutable" true, "script" "IonReporterUploader.py", "name" "IonReporterUploader", "isConfig" false, "date" "2013-07-03T03:30:34+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" false, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" ["export"], "runlevel" ["pre" "block" "post"], "runtype" ["thumbnail" "wholechip" "composite"]}, "version" "3.6.2-r62833", "userinputfields" {}, "id" 35, "resource_uri" "/rundb/api/v1/plugin/35/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'IonReporterUploader' v3.6.2-r62833\n\n[ Please update python class documentation to provide a short description and documentation for your plugin. ]", "isInstance" false}, "duration" "0:00:47.501694", "jobid" nil}}}, :lib-metrics-set #{{"sampled_q47_alignments" 0, "extrapolated_mapped_bases_in_q10_alignments" "0", "sampled_200q20_reads" 0, "i600Q20_reads" 0, "q20_alignments" 53385851, "sampled_300q10_reads" 0, "extrapolated_q47_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q20_alignments" "0", "sampled_100q20_reads" 0, "s50Q20" 0, "i500Q20_reads" 0, "sampled_200q10_reads" 0, "i600Q10_reads" 0, "extrapolated_from_number_of_sampled_reads" 0, "sampled_q47_coverage_percentage" 0, "q47_mapped_bases" "3781427950", "sampled_q17_longest_alignment" 0, "q20_coverage_percentage" 0, "sampled_100q10_reads" 0, "i500Q10_reads" 0, "r50Q20" 0, "q10_coverage_percentage" 0, "s50Q10" 0, "extrapolated_50q20_reads" 0, "i400Q20_reads" 0, "extrapolated_50q10_reads" 0, "cf" 0.27535108383745, "q7_mean_alignment_length" 91, "r50Q10" 0, "q47_longest_alignment" 211, "sNumAlignments" 0, "sampled_400q47_reads" 0, "i300Q20_reads" 0, "i400Q10_reads" 0, "align_sample" 0, "sampled_300q47_reads" 0, "i200Q20_reads" 46, "i300Q10_reads" 0, "sampled_q17_coverage_percentage" 0, "sampled_50q7_reads" 0, "sampled_q10_alignments" 0, "sampled_q7_alignments" 0, "extrapolated_q17_mean_alignment_length" 0, "rNumAlignments" 0, "i200Q10_reads" 179, "q10_mapped_bases" "5752686797", "extrapolated_100q7_reads" 0, "total_mapped_target_bases" "5830701139", "sampled_q20_alignments" 0, "sampled_200q47_reads" 0, "i600Q47_reads" 0, "i100Q20_reads" 17975937, "extrapolated_q7_longest_alignment" 0, "extrapolated_q20_mean_coverage_depth" 0, "q17_longest_alignment" 224, "sampled_100q47_reads" 0, "i500Q47_reads" 0, "q20_mapped_bases" "4235251115", "i100Q10_reads" 25792090, "i550Q20_reads" 0, "sampled_400q17_reads" 0, "extrapolated_200q7_reads" 0, "q47_coverage_percentage" 0, "sampled_q20_mean_alignment_length" 0, "i400Q47_reads" 0, "i150Q7_reads" 39620, "rMeanAlignLen" 0, "i450Q20_reads" 0, "extrapolated_50q47_reads" 0, "sampled_300q17_reads" 0, "extrapolated_300q7_reads" 0, "i550Q10_reads" 0, "extrapolated_q10_mean_coverage_depth" 0, "i350Q20_reads" 0, "sampled_200q17_reads" 0, "i600Q17_reads" 0, "i450Q10_reads" 0, "extrapolated_400q7_reads" 0, "sampled_q7_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q7_alignments" "0", "sampled_q10_mean_alignment_length" 0, "extrapolated_q7_coverage_percentage" 0, "extrapolated_q17_alignments" 0, "i50Q7_reads" 57344941, "i300Q47_reads" 0, "i250Q7_reads" 0, "sMeanAlignLen" 0, "s50Q17" 0, "duplicate_reads" nil, "extrapolated_400q20_reads" 0, "i200Q47_reads" 9, "i350Q7_reads" 0, "q7_alignments" 63110718, "i250Q20_reads" 0, "sampled_100q17_reads" 0, "i500Q17_reads" 0, "q17_coverage_percentage" 0, "i50Q20_reads" 40379815, "i350Q10_reads" 0, "extrapolated_300q20_reads" 0, "i100Q47_reads" 10672707, "r50Q17" 0, "extrapolated_400q10_reads" 0, "i450Q7_reads" 0, "i150Q20_reads" 6631, "i400Q17_reads" 0, "i250Q10_reads" 0, "i50Q10_reads" 56924835, "extrapolated_50q17_reads" 0, "sysSNR" 13.7530774782199, "i300Q17_reads" 0, "i150Q10_reads" 29664, "extrapolated_q47_mean_coverage_depth" 0, "extrapolated_q47_alignments" 0, "Index_Version" "tmap-f3", "extrapolated_200q20_reads" 0, "total_number_of_sampled_reads" 0, "extrapolated_300q10_reads" 0, "i550Q47_reads" 0, "i550Q7_reads" 0, "q20_mean_alignment_length" 79, "extrapolated_100q20_reads" 0, "total_mapped_reads" "65565617", "extrapolated_200q10_reads" 0, "i450Q47_reads" 0, "sampled_q47_mean_alignment_length" 0, "i100Q7_reads" 25953991, "i200Q17_reads" 90, "q17_qscore_bases" "0", "extrapolated_100q10_reads" 0, "i350Q47_reads" 0, "rLongestAlign" 0, "q10_mean_alignment_length" 91, "i100Q17_reads" 21597651, "i200Q7_reads" 187, "sampled_50q20_reads" 0, "dr" 0.167434278409928, "i300Q7_reads" 0, "i550Q17_reads" 0, "extrapolated_q17_mean_coverage_depth" 0, "extrapolated_q20_longest_alignment" 0, "sampled_50q10_reads" 0, "extrapolated_400q47_reads" 0, "i250Q47_reads" 0, "sLongestAlign" 0, "sampled_mapped_bases_in_q17_alignments" "0", "i50Q47_reads" 37409631, "q7_mapped_bases" "5794278906", "extrapolated_q10_alignments" 0, "extrapolated_300q47_reads" 0, "extrapolated_50q7_reads" 0, "sampled_q17_mean_alignment_length" 0, "i150Q47_reads" 2525, "r200Q20" 0, "q47_qscore_bases" "0", "extrapolated_q10_longest_alignment" 0, "i400Q7_reads" 0, "i450Q17_reads" 0, "Genome_Version" "hg19", "s200Q20" 0, "r100Q20" 0, "i500Q7_reads" 0, "i350Q17_reads" 0, "r200Q10" 0, "sampled_100q7_reads" 0, "extrapolated_q20_alignments" 0, "raw_accuracy" 98.7, "extrapolated_200q47_reads" 0, "i600Q7_reads" 0, "i250Q17_reads" 0, "s100Q20" 0, "sampled_200q7_reads" 0, "extrapolated_q20_coverage_percentage" 0, "i50Q17_reads" 50537490, "s200Q10" 0, "r100Q10" 0, "extrapolated_100q47_reads" 0, "sampled_q20_mean_coverage_depth" 0, "q47_mean_alignment_length" 72, "sampled_mapped_bases_in_q47_alignments" "0", "extrapolated_400q17_reads" 0, "s100Q10" 0, "extrapolated_300q17_reads" 0, "extrapolated_q10_coverage_percentage" 0, "sampled_50q47_reads" 0, "i150Q17_reads" 12004, "sampled_q7_mean_coverage_depth" 0, "sampled_q10_mean_coverage_depth" 0, "sampled_300q7_reads" 0, "q10_qscore_bases" "0", "extrapolated_200q17_reads" 0, "extrapolated_q7_mean_alignment_length" 0, "extrapolated_q47_longest_alignment" 0, "sampled_mapped_bases_in_q7_alignments" "0", "sampled_400q7_reads" 0, "aveKeyCounts" 89, "report" "/rundb/api/v1/results/77/", "q17_mean_alignment_length" 87, "q20_qscore_bases" "0", "extrapolated_100q17_reads" 0, "sampled_50q17_reads" 0, "sampled_mapped_bases_in_q10_alignments" "0", "q7_longest_alignment" 224, "sampled_mapped_bases_in_q20_alignments" "0", "extrapolated_q47_coverage_percentage" 0, "sampled_q47_mean_coverage_depth" 0, "extrapolated_q17_longest_alignment" 0, "genome" "hg19", "r200Q17" 0, "genomesize" "3095693981", "q7_coverage_percentage" 0, "s200Q17" 0, "r100Q17" 0, "q17_alignments" 57135955, "extrapolated_q7_alignments" 0, "s100Q17" 0, "extrapolated_q17_coverage_percentage" 0, "sampled_q20_longest_alignment" 0, "sampled_q17_mean_coverage_depth" 0, "extrapolated_mapped_bases_in_q17_alignments" "0", "rCoverage" 0, "sampled_q7_longest_alignment" 0, "sampled_q10_longest_alignment" 0, "id" 68, "totalNumReads" 67420355, "q47_alignments" 52254752, "resource_uri" "/rundb/api/v1/libmetrics/68/", "sCoverage" 0, "ie" 0.617759954184294, "extrapolated_q20_mean_alignment_length" 0, "sampled_q20_coverage_percentage" 0, "extrapolated_mapped_bases_in_q47_alignments" "0", "q7_qscore_bases" "0", "sampled_q17_alignments" 0, "extrapolated_q7_mean_coverage_depth" 0, "genomelength" 0, "extrapolated_q10_mean_alignment_length" 0, "sampled_q7_coverage_percentage" 0, "sampled_q10_coverage_percentage" 0, "sampled_400q20_reads" 0, "sampled_q47_longest_alignment" 0, "q20_longest_alignment" 224, "q17_mapped_bases" "5004408711", "q10_alignments" 62602410, "sampled_400q10_reads" 0, "q10_longest_alignment" 224, "sampled_300q20_reads" 0}}, :tf-metrics-set #{{"aveKeyCount" 92, "HPAccuracy" "0 : 331532643/333171476, 1 : 237654520/240430336, 2 : 10582544/10810316, 3 : 0/0, 4 : 2478849/2694841, 5 : 0/0, 6 : 0/0, 7 : 0/0", "sequence" "TACGAGCGTGTAGACGTGTCGTACGTGCGACGTAGTGAGTATACATGCTCTGACACTATGTACGATCTGAGACTGCCAAGGCACACAGGGGATAGG", "Q17ReadCount" 2422568, "corrHPSNR" "", "Q10ReadCount" 2623331, "number" 2755545, "SysSNR" 9.80844083888461, "name" "TF_C", "keypass" 2755545, "report" "/rundb/api/v1/results/77/", "Q10Mean" 91, "Q17Histo" "8359 38630 27283 25575 7839 3555 11554 9211 10021 14301 10666 27098 10045 13259 5789 7613 3527 7326 5427 6931 3776 2241 2257 2837 2658 1421 7936 10362 880 4733 1934 3657 3597 3144 884 431 4430 4016 2006 1674 686 2476 2764 2151 1669 577 826 1100 1144 701 6365 6499 5230 7266 3399 12241 10272 6734 6330 2250 2632 2916 2791 1261 2211 2420 1303 1448 1064 3411 1589 8234 1543 1308 2131 2348 5087 1125 2528 904 5728 1214 601 774 1092 1782 3355 2952 3359 13335 16913 70845 13506 943 1371 36139 1947587 186232 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0", "id" 68, "resource_uri" "/rundb/api/v1/tfmetrics/68/", "Q17Mean" 84, "Q10Histo" "4258 8094 6541 2829 1904 567 861 714 3153 2903 4833 6334 4421 4839 2068 2419 2117 2593 2753 1278 2723 3995 4440 2808 1325 1364 4212 5678 1709 1323 1271 2879 3721 2476 1296 871 1142 2277 1894 1573 1795 1876 1834 2106 1986 1618 1561 1681 1765 1536 2215 2777 2083 1540 2377 2144 2248 1580 952 970 953 866 658 541 986 551 520 389 414 319 385 565 559 934 595 854 2773 1037 1434 983 5210 1581 1030 834 1362 1345 3702 2440 4928 15418 19648 74742 18929 10620 10554 45045 1965728 202921 69360 39695 28229 20974 16135 12327 8942 5048 1382 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"}}, :analysis-metrics-set #{{"keypass_all_beads" 0, "washout_dud" 0, "tfFinal" 2831301, "lib" 133931487, "bead" 137867956, "sysIE" 0.617759954184294, "excluded" 0, "tfMix" 0, "libKp" 0, "washout_library" 0, "live" 137119356, "empty" 6727891, "washout_test_fragment" 0, "washout" 0, "lib_pass_basecaller" 0, "lib_pass_cafie" 0, "sysCF" 0.27535108383745, "libLive" 0, "libFinal" 67443042, "ignored" 3782896, "tf" 3187869, "dud" 748600, "report" "/rundb/api/v1/results/77/", "amb" 0, "libMix" 0, "tfKp" 0, "pinned" 16320393, "sysDR" 0.167434278409928, "id" 74, "resource_uri" "/rundb/api/v1/analysismetrics/74/", "washout_ambiguous" 0, "washout_live" 0, "tfLive" 0}}, :quality-metrics-set #{{"q0_bases" "6015091255", "q20_max_read_length" 297, "q20_mean_read_length" 59, "q0_50bp_reads" 58676073, "q20_150bp_reads" 26132, "q17_max_read_length" 299, "q20_100bp_reads" 13225065, "q20_reads" 67420355, "q17_50bp_reads" 52999900, "q17_150bp_reads" 122924, "q20_bases" "4849408686", "q0_max_read_length" 299, "report" "/rundb/api/v1/results/77/", "q17_100bp_reads" 23394392, "q17_reads" 67420355, "q0_mean_read_length" 89, "q20_50bp_reads" 38652987, "q0_150bp_reads" 208782, "q17_bases" "5267814486", "id" 74, "resource_uri" "/rundb/api/v1/qualitymetrics/74/", "q0_100bp_reads" 27101827, "q17_mean_read_length" 80, "q0_reads" 67420355}}, :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "3XNXT", "metaData" {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts 77 {:recurse? true})))

(expect r77 (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (result ts 77 {})))

(expect r77 (read-string (pr-str r77)))

(expect [:torrent-server :id :name :uri :experiment-uri :status :plugin-result-uri-set :plugin-state-map :analysis-version :report-status :plugin-store-map :bam-link :fastq-link :report-link :filesystem-path :reference :lib-metrics-uri-set :tf-metrics-uri-set :analysis-metrics-uri-set :quality-metrics-uri-set :timestamp :thumbnail? :plugin-result-set :lib-metrics-set :tf-metrics-set :analysis-metrics-set :quality-metrics-set :raw-map]
        (keys r77))

(expect r77 (edn/read-string {:readers sc/data-readers} (pr-str r77)))

(expect r77
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts "/rundb/api/v1/results/77/")))

;;; plugin-result

(def pr209j  (with-fake-routes-in-isolation
               {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                        {:status 200 :headers {"Content-Type" "application/json"}
                                         :body (slurp (uri-to-file uri :json))})}
               (get-json ts "pluginresult/209")))

(expect {"id" 209 "resultName" "24_reanalyze"} (in pr209j))
(expect ion_torrent_api.schema.TorrentServer (type ts))
(def pr209 (with-fake-routes-in-isolation
             {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                      {:status 200 :headers {"Content-Type" "application/json"}
                                       :body (slurp (uri-to-file uri :json))})}
             (plugin-result ts 209 {})))
(expect pr209 (assoc (plugin-result pr209j) :torrent-server ts))
(expect #ion_torrent_api.schema.PluginResult{:type :tsvc,
                                             :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                             :id 209,
                                             :uri "/rundb/api/v1/pluginresult/209/",
                                             :result-uri "/rundb/api/v1/results/77/",
                                             :result-name "24_reanalyze",
                                             :state "Completed",
                                             :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out",
                                             :report-link "/output/Home/24_reanalyze_077/",
                                             :name "variantCaller",
                                             :version "4.0-r76860",
                                             :versioned-name "variantCaller--v4.0-r76860",
                                             :library-type "AmpliSeq",
                                             :config-desc "Somatic - Proton - Low Stringency",
                                             :barcode-result-map {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}},
                                             :target-name "4477685_Comprehensive_CCP_bedfile_20120517",
                                             :target-bed "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
                                             :experiment-name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                             :trimmed-reads? true,
                                             :barcoded? true,
                                             :start-time #inst "2014-02-17T05:50:42.000-00:00",
                                             :end-time #inst "2014-02-17T09:37:51.000-00:00",
                                             :raw-map {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}}
        pr209)

(expect (more-of x
                 (:uri r77) (:result-uri x)
                 50 (:id e50)
                 {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
                 (:barcode-result-map x)
                 ["IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"]
                 (keys (:barcode-result-map x))
                 #{"IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"}
                 (barcode-set e50)
                 )
        pr209)

(expect pr209
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 209 {})))

(expect pr209
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts "/rundb/api/v1/pluginresult/209")))

(expect pr209 (read-string (pr-str pr209)))

(expect pr209 (edn/read-string {:readers sc/data-readers} (pr-str pr209)))

(expect (more->
         :tsvc :type
         true  variant-caller?
         "variantCaller" :name
         "variantCaller--v4.0-r76860" :versioned-name)
        pr209)

(expect (more-of x
                 ["variantCaller" "variantCaller--v4.0-r76860"]
                 ((juxt #(% "name") #(% "versionedName")) (get x "plugin"))
                 {"IonXpressRNA_001"
                  {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046,
                                              "homo_indels" 21, "homo_snps" 267,
                                              "no_call" 0, "other" 9, "variants" 1447}},
                  "IonXpressRNA_002"
                  {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850,
                                              "homo_indels" 24, "homo_snps" 306,
                                              "no_call" 0, "other" 6, "variants" 1312}},
                  "IonXpressRNA_003"
                  {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799,
                                              "homo_indels" 22, "homo_snps" 303,
                                              "no_call" 0, "other" 11, "variants" 1248}},
                  "IonXpressRNA_004"
                  {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937,
                                              "homo_indels" 26, "homo_snps" 292,
                                              "no_call" 0, "other" 6, "variants" 1388}},
                  "IonXpressRNA_005"
                  {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841,
                                              "homo_indels" 21, "homo_snps" 316,
                                              "no_call" 0, "other" 6, "variants" 1304}}}
                 (get-in x ["store" "barcodes"]))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (get-json ts (#'ion/ensure-starts-with (str (:api-path ts) "pluginresult/")
                                                 (str 209)))))

(expect (more-of x
                 true (complete? x)
                 "variantCaller" (:name x)
                 :tsvc (:type x)
                 true (variant-caller? x)
                 false (coverage? x))
        pr209)

;;; barcode map of variantCaller results

(expect (more-of x
                 ["variantCaller" "variantCaller--v4.0-r76860"]
                 ((juxt :name :versioned-name) x)
                 {"IonXpressRNA_001"
                  {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046,
                                              "homo_indels" 21, "homo_snps" 267,
                                              "no_call" 0, "other" 9, "variants" 1447}},
                  "IonXpressRNA_002"
                  {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850,
                                              "homo_indels" 24, "homo_snps" 306,
                                              "no_call" 0, "other" 6, "variants" 1312}},
                  "IonXpressRNA_003"
                  {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799,
                                              "homo_indels" 22, "homo_snps" 303,
                                              "no_call" 0, "other" 11, "variants" 1248}},
                  "IonXpressRNA_004"
                  {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937,
                                              "homo_indels" 26, "homo_snps" 292,
                                              "no_call" 0, "other" 6, "variants" 1388}},
                  "IonXpressRNA_005"
                  {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841,
                                              "homo_indels" 21, "homo_snps" 316,
                                              "no_call" 0, "other" 6, "variants" 1304}}}
                 (:barcode-result-map x))
        pr209)

;;; barcode map of coverageAnalysis
(expect (more-of x
                 ["coverageAnalysis" "coverageAnalysis--v3.6.58977"]
                 ((juxt #(% "name") #(% "versionedName")) (get x "plugin"))
                 {"IonXpressRNA_001"
                  {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.97%", "Target bases with no strand bias" "79.94%", "Number of mapped reads" "13541550", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.72%", "Target base coverage at 20x" "96.51%", "Total aligned base reads" "1185203398", "Average reads per amplicon" "794.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_001_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.94%", "Uniformity of base coverage" "83.69%", "Percent reads on target" "93.77%", "Total assigned amplicon reads" "12697352", "Percent assigned amplicon reads" "93.77%", "Amplicons with at least 100 reads" "90.96%", "Average base coverage depth" "671.9", "Target base coverage at 500x" "47.27%", "Amplicons with at least 500 reads" "53.41%", "Reference (File)" "hg19", "Percent base reads on target" "95.73%", "Total base reads on target" "1134571007", "Target base coverage at 1x" "99.34%", "Target base coverage at 100x" "88.24%", "Amplicons with at least 1 read" "99.51%", "Amplicons reading end-to-end" "8.67%"},
                  "IonXpressRNA_002"
                  {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.33%", "Target bases with no strand bias" "79.28%", "Number of mapped reads" "13579387", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "86.64%", "Target base coverage at 20x" "95.16%", "Total aligned base reads" "1228217548", "Average reads per amplicon" "815.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_002_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "95.56%", "Uniformity of base coverage" "85.89%", "Percent reads on target" "95.97%", "Total assigned amplicon reads" "13032709", "Percent assigned amplicon reads" "95.97%", "Amplicons with at least 100 reads" "90.11%", "Average base coverage depth" "707.7", "Target base coverage at 500x" "52.31%", "Amplicons with at least 500 reads" "59.97%", "Reference (File)" "hg19", "Percent base reads on target" "97.30%", "Total base reads on target" "1195050872", "Target base coverage at 1x" "99.00%", "Target base coverage at 100x" "89.04%", "Amplicons with at least 1 read" "99.19%", "Amplicons reading end-to-end" "8.34%"},
                  "IonXpressRNA_003"
                  {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.60%", "Target bases with no strand bias" "80.47%", "Number of mapped reads" "12145531", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "88.99%", "Target base coverage at 20x" "95.99%", "Total aligned base reads" "1085455295", "Average reads per amplicon" "733.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_003_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.33%", "Uniformity of base coverage" "88.03%", "Percent reads on target" "96.56%", "Total assigned amplicon reads" "11727495", "Percent assigned amplicon reads" "96.56%", "Amplicons with at least 100 reads" "91.72%", "Average base coverage depth" "624.3", "Target base coverage at 500x" "47.29%", "Amplicons with at least 500 reads" "56.77%", "Reference (File)" "hg19", "Percent base reads on target" "97.12%", "Total base reads on target" "1054159542", "Target base coverage at 1x" "99.33%", "Target base coverage at 100x" "90.07%", "Amplicons with at least 1 read" "99.50%", "Amplicons reading end-to-end" "8.90%"},
                  "IonXpressRNA_004"
                  {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.52%", "Target bases with no strand bias" "78.41%", "Number of mapped reads" "11984953", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "83.01%", "Target base coverage at 20x" "95.09%", "Total aligned base reads" "1041277993", "Average reads per amplicon" "719.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_004_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "95.80%", "Uniformity of base coverage" "82.07%", "Percent reads on target" "95.94%", "Total assigned amplicon reads" "11498272", "Percent assigned amplicon reads" "95.94%", "Amplicons with at least 100 reads" "87.60%", "Average base coverage depth" "596.4", "Target base coverage at 500x" "42.30%", "Amplicons with at least 500 reads" "51.26%", "Reference (File)" "hg19", "Percent base reads on target" "96.73%", "Total base reads on target" "1007191043", "Target base coverage at 1x" "99.14%", "Target base coverage at 100x" "84.54%", "Amplicons with at least 1 read" "99.37%", "Amplicons reading end-to-end" "7.47%"},
                  "IonXpressRNA_005"
                  {"Number of amplicons" "15992", "Amplicons with no strand bias" "94.23%", "Target bases with no strand bias" "81.41%", "Number of mapped reads" "13717156", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "89.76%", "Target base coverage at 20x" "96.53%", "Total aligned base reads" "1215435073", "Average reads per amplicon" "825.4", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_005_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.85%", "Uniformity of base coverage" "88.68%", "Percent reads on target" "96.23%", "Total assigned amplicon reads" "13200287", "Percent assigned amplicon reads" "96.23%", "Amplicons with at least 100 reads" "92.85%", "Average base coverage depth" "697.9", "Target base coverage at 500x" "53.15%", "Amplicons with at least 500 reads" "63.03%", "Reference (File)" "hg19", "Percent base reads on target" "96.96%", "Total base reads on target" "1178467512", "Target base coverage at 1x" "99.34%", "Target base coverage at 100x" "91.41%", "Amplicons with at least 1 read" "99.49%", "Amplicons reading end-to-end" "8.25%"}}
                 (get-in x ["store" "barcodes"]))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (get-json ts (#'ion/ensure-starts-with (str (:api-path ts) "pluginresult/")
                                                 (str 66)))))

(expect (more-of x
                 ion_torrent_api.schema.PluginResult x
                 66 (:id x)
                 :coverage (:type x)
                 "/output/Home/Auto_user_XXX-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out"
                 (#'ion/plugin-result-api-path-prefix x)
                 #{"IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"}
                 (barcode-set x)
                 "IonXpressRNA_001_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50"
                 (get-in (:barcode-result-map x) [(name :IonXpressRNA_001) "Alignments"])
                 "/output/Home/Auto_user_XXX-24-AmpliSeq_CCP_24_50_061/plugin_out/coverageAnalysis_out/IonXpressRNA_001/IonXpressRNA_001_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50.amplicon.cov.xls"
                 (coverage-ampl-uri x :IonXpressRNA_001)
                 nil
                 (bam-uri x :IonXpressRNA_001)
                 nil
                 (bai-uri x :IonXpressRNA_001))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 66)))

(expect '[:type :torrent-server :id :uri :result-uri :result-name :state :path :report-link :name :version :versioned-name :library-type :config-desc :target-name :target-bed :experiment-name :trimmed-reads? :barcode-result-map :barcoded? :start-time :end-time :raw-map]
        (keys pr209))

(expect pr209
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts "/rundb/api/v1/pluginresult/209/")))

(expect pr209
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 209 {})))

;;; Experiment record
(expect  #ion_torrent_api.schema.Experiment{:id 50, :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                            :pgm-name "XXXNPROTON", :display-name "user XXX-24-AmpliSeq CCP 24",
                                            :uri "/rundb/api/v1/experiment/50/",
                                            :run-type "AMPS",
                                            :chip-type "900",
                                            :barcode-sample-map
                                            {"IonXpressRNA_003" "inq-022-me"
                                             "IonXpressRNA_004" "inq-024-me"
                                             "IonXpressRNA_005" "inq-025-tt"
                                             "IonXpressRNA_002" "inq-037-me"
                                             "IonXpressRNA_001" "inq-052-tt"}
                                            :sample-map {"inq-037-me" {"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil}
                                                         "inq-052-tt" {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil}
                                                         "inq-024-me" {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil}
                                                         "inq-022-me" {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil}
                                                         "inq-025-tt" {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}},
                                            :result-uri-set ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"],
                                            :dir "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                            :status "run", :ftp-status "Complete",
                                            :date #inst "2013-06-03T13:31:54.000-00:00",
                                            :latest-result-date #inst "2013-07-23T00:32:14.000-00:00",
                                            :raw-map {"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400, "baselineRun" false, "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/", "sample" "inq-037-me", "resultDate" "2013-07-23T00:32:14.000226+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-06-03T13:31:54+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "", "libraryKey" "TCAG", "thumbnailbasecallerargs" "", "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment", "userInput" "", "version" "3.6.56201"}}, "thumbnailanalysisargs" "", "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]}, "inq-024-me" {"barcodes" ["IonXpressRNA_004"]}, "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]}, "inq-037-me" {"barcodes" ["IonXpressRNA_002"]}, "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}}, "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-06-04T03:26:53.000155+00:00", "status" "run", "thumbnailalignmentargs" "", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47, "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "", "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}
                                            :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}}
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (experiment ts 50)))

;;; Result record

(expect #ion_torrent_api.schema.Result{:id 77, :name "24_reanalyze",
                                       :uri "/rundb/api/v1/results/77/",
                                       :experiment-uri "/rundb/api/v1/experiment/50/", :status "Completed",
                                       :plugin-result-set nil
                                       :plugin-result-uri-set ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"],
                                       :plugin-state-map {"IonReporterUploader" "Completed", "variantCaller" "Completed"},
                                       :analysis-version "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,",
                                       :report-status "Nothing",
                                       :plugin-store-map
                                       {"IonReporterUploader" {},
                                        "variantCaller"
                                        {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                         "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
                                         "barcoded" "true",
                                         "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517",
                                         "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}},
                                         "Configuration" "Somatic - Proton - Low Stringency",
                                         "Target Loci" "Not using",
                                         "Trim Reads" true, "Library Type" "AmpliSeq"}},
                                       :bam-link "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam",
                                       :fastq-link "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq",
                                       :report-link "/output/Home/24_reanalyze_077/",
                                       :filesystem-path "/results/analysis/output/Home/24_reanalyze_077",
                                       :reference "hg19",
                                       :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/68/"],
                                       :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/68/"],
                                       :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/74/"],
                                       :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/74/"]
                                       :timestamp #inst "2013-07-23T05:18:31.000209000-00:00", :thumbnail? false
                                       :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "3XNXT", "metaData" {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}
                                       :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts "/rundb/api/v1/results/77/")))

;;; PluginResult record

(expect #ion_torrent_api.schema.PluginResult{:type :tsvc
                                             :name "variantCaller", :version "4.0-r76860",
                                             :versioned-name "variantCaller--v4.0-r76860",
                                             :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out",
                                             :state "Completed", :result-uri "/rundb/api/v1/results/77/",
                                             :uri "/rundb/api/v1/pluginresult/209/"
                                             :result-name "24_reanalyze", :id 209
                                             :report-link "/output/Home/24_reanalyze_077/"
                                             :target-name "4477685_Comprehensive_CCP_bedfile_20120517"
                                             :target-bed "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed"
                                             :library-type "AmpliSeq"
                                             :barcode-result-map
                                             {"IonXpressRNA_001" {"hotspots" {},
                                                                  "variants" {"het_indels" 104, "het_snps" 1046,
                                                                              "homo_indels" 21, "homo_snps" 267,
                                                                              "no_call" 0, "other" 9, "variants" 1447}},
                                              "IonXpressRNA_002" {"hotspots" {},
                                                                  "variants" {"het_indels" 126, "het_snps" 850,
                                                                              "homo_indels" 24, "homo_snps" 306,
                                                                              "no_call" 0, "other" 6, "variants" 1312}},
                                              "IonXpressRNA_003" {"hotspots" {},
                                                                  "variants" {"het_indels" 113, "het_snps" 799,
                                                                              "homo_indels" 22, "homo_snps" 303,
                                                                              "no_call" 0, "other" 11, "variants" 1248}},
                                              "IonXpressRNA_004" {"hotspots" {},
                                                                  "variants" {"het_indels" 127, "het_snps" 937,
                                                                              "homo_indels" 26, "homo_snps" 292,
                                                                              "no_call" 0, "other" 6, "variants" 1388}},
                                              "IonXpressRNA_005" {"hotspots" {},
                                                                  "variants" {"het_indels" 120, "het_snps" 841,
                                                                              "homo_indels" 21, "homo_snps" 316,
                                                                              "no_call" 0, "other" 6, "variants" 1304}}},
                                             :experiment-name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                             :config-desc "Somatic - Proton - Low Stringency",
                                             :barcoded? true,
                                             :trimmed-reads? true
                                             :start-time #inst "2014-02-17T05:50:42.000089000-00:00"
                                             :end-time #inst "2014-02-17T09:37:51.000879000-00:00"
                                             :raw-map
                                             {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}
                                             :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}}
        pr209)

(expect (more-> {"IonXpressRNA_001"
                 {"hotspots" {},
                  "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21,
                              "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}},
                 "IonXpressRNA_002"
                 {"hotspots" {},
                  "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24,
                              "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}},
                 "IonXpressRNA_003"
                 {"hotspots" {},
                  "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22,
                              "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}},
                 "IonXpressRNA_004"
                 {"hotspots" {},
                  "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26,
                              "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}},
                 "IonXpressRNA_005"
                 {"hotspots" {},
                  "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21,
                              "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
                :barcode-result-map
                #{"IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"}
                barcode-set)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 209)))

(expect pr209
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts "/rundb/api/v1/pluginresult/209/")))

;;; sampleID plugin-result

(expect {:b 2 :c 3} (select-keys {:a 1 :b 2 :c 3 :d 4} #{:b :c}))
(expect {:b 2 :c 3} (barcode-map {:a 1 :b 2 :c 3 :d 4} #{:b :c}))
(expect #{:b :c} (barcode-set #{:b :c}))
(expect #{:b :c} (barcode-set {:b 1 :c 2}))

(def e71 (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (experiment ts 71 {:recurse? true})))

(expect #ion_torrent_api.schema.Experiment{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 71, :name "R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", :pgm-name "XXXNPROTON", :display-name "user XXX-43-AmpliSeq CCP 1-42", :uri "/rundb/api/v1/experiment/71/", :run-type "AMPS", :chip-type "P1.1.17", :result-uri-set ["/rundb/api/v1/results/103/" "/rundb/api/v1/results/104/"], :dir "/rawdata/XXXNPROTON/R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", :status "run", :ftp-status "Complete", :sample-map {"INQ0141TT01-MP-D" {"externalId" "2169-100B", "name" "INQ0141TT01-MP-D", "displayedName" "INQ0141TT01-MP-D", "date" "2013-11-18T01:14:57.000502+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 224, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/224/", "description" "6pM"}, "INQ0138TT01-5F-D" {"externalId" "2169-100B", "name" "INQ0138TT01-5F-D", "displayedName" "INQ0138TT01-5F-D", "date" "2013-11-18T01:14:57.000506+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 225, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/225/", "description" "6pM"}, "INQ0131TT01-7C-D" {"externalId" "2169-100B", "name" "INQ0131TT01-7C-D", "displayedName" "INQ0131TT01-7C-D", "date" "2013-11-18T01:14:57.000508+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 226, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/226/", "description" "6pM"}, "INQ0160TT01-SH-D" {"externalId" "2169-100B", "name" "INQ0160TT01-SH-D", "displayedName" "INQ0160TT01-SH-D", "date" "2013-11-18T01:14:57.000510+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 227, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/227/", "description" "6pM"}, "INQ0136TT01-Q5-D" {"externalId" "2169-100B", "name" "INQ0136TT01-Q5-D", "displayedName" "INQ0136TT01-Q5-D", "date" "2013-11-18T01:14:57.000513+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 228, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/228/", "description" "6pM"}, "INQ0152TT01-RP-D" {"externalId" "2169-100B", "name" "INQ0152TT01-RP-D", "displayedName" "INQ0152TT01-RP-D", "date" "2013-11-18T01:14:57.000515+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 229, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/229/", "description" "6pM"}, "INQ0151TT01-2R-D" {"externalId" "2169-100B", "name" "INQ0151TT01-2R-D", "displayedName" "INQ0151TT01-2R-D", "date" "2013-11-18T01:14:57.000517+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 230, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/230/", "description" "6pM"}}, :barcode-sample-map {"IonXpress_006" "INQ0131TT01-7C-D", "IonXpress_002" "INQ0136TT01-Q5-D", "IonXpress_015" "INQ0138TT01-5F-D", "IonXpress_005" "INQ0141TT01-MP-D", "IonXpress_018" "INQ0151TT01-2R-D", "IonXpress_004" "INQ0152TT01-RP-D", "IonXpress_014" "INQ0160TT01-SH-D"}, :date #inst "2013-11-17T12:33:49.000-00:00", :latest-result-date #inst "2013-11-18T09:53:43.000-00:00", :latest-result #ion_torrent_api.schema.Result{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 103, :name "Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", :uri "/rundb/api/v1/results/103/", :experiment-uri "/rundb/api/v1/experiment/71/", :status "Completed", :plugin-result-uri-set ["/rundb/api/v1/pluginresult/158/" "/rundb/api/v1/pluginresult/157/" "/rundb/api/v1/pluginresult/156/"], :plugin-state-map {"coverageAnalysis" "Completed", "sampleID" "Completed", "variantCaller" "Completed"}, :analysis-version "db:4.0.21-1,an:4.0.5-1,", :report-status "Nothing", :plugin-store-map {"coverageAnalysis" {"Non-duplicate" "No", "Target padding" "0", "Targetted regions" "/results/uploads/BED/1/hg19/merged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "Uniquely mapped" "No", "barcoded" "true", "barcodes" {"IonXpress_002" {"Number of amplicons" "15992", "Amplicons with no strand bias" "88.73%", "Target bases with no strand bias" "81.14%", "Sample Name" "INQ0136TT01-Q5-D", "Number of mapped reads" "6748981", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "86.31%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "641008255", "Average reads per amplicon" "396.5", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_002_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.03%", "Uniformity of base coverage" "85.52%", "Percent reads on target" "93.96%", "Total assigned amplicon reads" "6341404", "Percent assigned amplicon reads" "93.96%", "Amplicons with at least 100 reads" "83.95%", "Average base coverage depth" "362.1", "Target base coverage at 500x" "25.44%", "Amplicons with at least 500 reads" "30.23%", "Reference (File)" "hg19", "Percent base reads on target" "95.40%", "Total base reads on target" "611508036", "Target base coverage at 1x" "98.11%", "Target base coverage at 100x" "81.87%", "Amplicons with at least 1 read" "98.75%", "Amplicons reading end-to-end" "43.11%"}, "IonXpress_004" {"Number of amplicons" "15992", "Amplicons with no strand bias" "87.22%", "Target bases with no strand bias" "79.41%", "Sample Name" "INQ0152TT01-RP-D", "Number of mapped reads" "5166623", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.91%", "Target base coverage at 20x" "91.32%", "Total aligned base reads" "485332704", "Average reads per amplicon" "304.8", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_004_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.50%", "Uniformity of base coverage" "83.88%", "Percent reads on target" "94.33%", "Total assigned amplicon reads" "4873727", "Percent assigned amplicon reads" "94.33%", "Amplicons with at least 100 reads" "78.38%", "Average base coverage depth" "274.3", "Target base coverage at 500x" "13.88%", "Amplicons with at least 500 reads" "17.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.42%", "Total base reads on target" "463124273", "Target base coverage at 1x" "97.73%", "Target base coverage at 100x" "75.20%", "Amplicons with at least 1 read" "98.48%", "Amplicons reading end-to-end" "38.50%"}, "IonXpress_005" {"Number of amplicons" "15992", "Amplicons with no strand bias" "90.75%", "Target bases with no strand bias" "82.75%", "Sample Name" "INQ0141TT01-MP-D", "Number of mapped reads" "12498342", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "80.38%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "1118196811", "Average reads per amplicon" "729.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_005_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.20%", "Uniformity of base coverage" "79.35%", "Percent reads on target" "93.32%", "Total assigned amplicon reads" "11663441", "Percent assigned amplicon reads" "93.32%", "Amplicons with at least 100 reads" "84.87%", "Average base coverage depth" "628.9", "Target base coverage at 500x" "44.51%", "Amplicons with at least 500 reads" "50.69%", "Reference (File)" "hg19", "Percent base reads on target" "94.98%", "Total base reads on target" "1062030713", "Target base coverage at 1x" "98.44%", "Target base coverage at 100x" "82.40%", "Amplicons with at least 1 read" "99.18%", "Amplicons reading end-to-end" "31.76%"}, "IonXpress_006" {"Number of amplicons" "15992", "Amplicons with no strand bias" "91.70%", "Target bases with no strand bias" "83.93%", "Sample Name" "INQ0131TT01-7C-D", "Number of mapped reads" "8488131", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.81%", "Target base coverage at 20x" "91.72%", "Total aligned base reads" "775619927", "Average reads per amplicon" "496.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_006_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.90%", "Uniformity of base coverage" "78.86%", "Percent reads on target" "93.51%", "Total assigned amplicon reads" "7937065", "Percent assigned amplicon reads" "93.51%", "Amplicons with at least 100 reads" "79.67%", "Average base coverage depth" "437.5", "Target base coverage at 500x" "27.47%", "Amplicons with at least 500 reads" "32.13%", "Reference (File)" "hg19", "Percent base reads on target" "95.26%", "Total base reads on target" "738833701", "Target base coverage at 1x" "98.06%", "Target base coverage at 100x" "76.97%", "Amplicons with at least 1 read" "98.81%", "Amplicons reading end-to-end" "36.53%"}, "IonXpress_014" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.15%", "Target bases with no strand bias" "84.57%", "Sample Name" "INQ0160TT01-SH-D", "Number of mapped reads" "7185798", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.73%", "Target base coverage at 20x" "91.04%", "Total aligned base reads" "654660133", "Average reads per amplicon" "420.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_014_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.27%", "Uniformity of base coverage" "78.87%", "Percent reads on target" "93.46%", "Total assigned amplicon reads" "6716054", "Percent assigned amplicon reads" "93.46%", "Amplicons with at least 100 reads" "77.05%", "Average base coverage depth" "367.8", "Target base coverage at 500x" "22.45%", "Amplicons with at least 500 reads" "26.97%", "Reference (File)" "hg19", "Percent base reads on target" "94.87%", "Total base reads on target" "621043917", "Target base coverage at 1x" "97.84%", "Target base coverage at 100x" "73.68%", "Amplicons with at least 1 read" "98.61%", "Amplicons reading end-to-end" "36.99%"}, "IonXpress_015" {"Number of amplicons" "15992", "Amplicons with no strand bias" "89.66%", "Target bases with no strand bias" "82.33%", "Sample Name" "INQ0138TT01-5F-D", "Number of mapped reads" "7308490", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "81.82%", "Target base coverage at 20x" "91.77%", "Total aligned base reads" "667495055", "Average reads per amplicon" "427.4", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_015_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.86%", "Uniformity of base coverage" "80.87%", "Percent reads on target" "93.53%", "Total assigned amplicon reads" "6835733", "Percent assigned amplicon reads" "93.53%", "Amplicons with at least 100 reads" "79.56%", "Average base coverage depth" "376.4", "Target base coverage at 500x" "25.82%", "Amplicons with at least 500 reads" "31.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.21%", "Total base reads on target" "635549725", "Target base coverage at 1x" "97.99%", "Target base coverage at 100x" "76.46%", "Amplicons with at least 1 read" "98.77%", "Amplicons reading end-to-end" "37.39%"}, "IonXpress_018" {"Number of amplicons" "15992", "Amplicons with no strand bias" "84.50%", "Target bases with no strand bias" "77.75%", "Sample Name" "INQ0151TT01-2R-D", "Number of mapped reads" "8822299", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.30%", "Target base coverage at 20x" "93.70%", "Total aligned base reads" "841346599", "Average reads per amplicon" "523.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_018_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.50%", "Uniformity of base coverage" "83.59%", "Percent reads on target" "94.80%", "Total assigned amplicon reads" "8363758", "Percent assigned amplicon reads" "94.80%", "Amplicons with at least 100 reads" "84.82%", "Average base coverage depth" "475.6", "Target base coverage at 500x" "37.03%", "Amplicons with at least 500 reads" "41.67%", "Reference (File)" "hg19", "Percent base reads on target" "95.45%", "Total base reads on target" "803054084", "Target base coverage at 1x" "98.38%", "Target base coverage at 100x" "82.97%", "Amplicons with at least 1 read" "99.06%", "Amplicons reading end-to-end" "43.34%"}}}, "sampleID" {"Library Type" "", "Target Loci" "", "Target Regions" "", "Variant Detection" "", "barcoded" "true", "barcodes" {"IonXpress_002" {"SampleID" "F-TKRYRSRT", "hotspot_coverage" {"Average base coverage depth" "2450.6", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1946", "Male sample ID region reads" "0", "Number of mapped reads" "6748981", "Number of reads in sample ID regions" "24628", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "641008255"}, "target_coverage" {"Average base coverage depth" "2349.8", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_004" {"SampleID" "F-YGAYRSAA", "hotspot_coverage" {"Average base coverage depth" "1762.8", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1112", "Male sample ID region reads" "0", "Number of mapped reads" "5166623", "Number of reads in sample ID regions" "17951", "Percent base reads in sample ID regions" "0.34%", "Percent reads in sample ID regions" "0.35%", "Total base reads in sample ID regions" "485332704"}, "target_coverage" {"Average base coverage depth" "1687.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_005" {"SampleID" "F-TKAYGSGW", "hotspot_coverage" {"Average base coverage depth" "5172.9", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2686", "Male sample ID region reads" "0", "Number of mapped reads" "12498342", "Number of reads in sample ID regions" "52649", "Percent base reads in sample ID regions" "0.42%", "Percent reads in sample ID regions" "0.42%", "Total base reads in sample ID regions" "1118196811"}, "target_coverage" {"Average base coverage depth" "4781.7", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_006" {"SampleID" "F-TKG?GSAT", "hotspot_coverage" {"Average base coverage depth" "3259.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1512", "Male sample ID region reads" "0", "Number of mapped reads" "8488131", "Number of reads in sample ID regions" "32457", "Percent base reads in sample ID regions" "0.38%", "Percent reads in sample ID regions" "0.38%", "Total base reads in sample ID regions" "775619927"}, "target_coverage" {"Average base coverage depth" "2961.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_014" {"SampleID" "F-TTGYRSRW", "hotspot_coverage" {"Average base coverage depth" "2576.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1002", "Male sample ID region reads" "0", "Number of mapped reads" "7185798", "Number of reads in sample ID regions" "25805", "Percent base reads in sample ID regions" "0.35%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "654660133"}, "target_coverage" {"Average base coverage depth" "2338.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_015" {"SampleID" "F-TGGYGCAT", "hotspot_coverage" {"Average base coverage depth" "2599.4", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1593", "Male sample ID region reads" "0", "Number of mapped reads" "7308490", "Number of reads in sample ID regions" "26810", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "667495055"}, "target_coverage" {"Average base coverage depth" "2446.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_018" {"SampleID" "F-YTAYGCGT", "hotspot_coverage" {"Average base coverage depth" "3248.2", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2115", "Male sample ID region reads" "0", "Number of mapped reads" "8822299", "Number of reads in sample ID regions" "32819", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "841346599"}, "target_coverage" {"Average base coverage depth" "3067.5", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}}}, "variantCaller" {"Aligned Reads" "R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpress_002" {"hotspots" {}, "variants" {"het_indels" 160, "het_snps" 762, "homo_indels" 36, "homo_snps" 281, "no_call" 0, "other" 10, "variants" 1249}}, "IonXpress_004" {"hotspots" {}, "variants" {"het_indels" 171, "het_snps" 908, "homo_indels" 37, "homo_snps" 234, "no_call" 0, "other" 7, "variants" 1357}}, "IonXpress_005" {"hotspots" {}, "variants" {"het_indels" 179, "het_snps" 1100, "homo_indels" 31, "homo_snps" 263, "no_call" 0, "other" 13, "variants" 1586}}, "IonXpress_006" {"hotspots" {}, "variants" {"het_indels" 159, "het_snps" 904, "homo_indels" 35, "homo_snps" 274, "no_call" 0, "other" 8, "variants" 1380}}, "IonXpress_014" {"hotspots" {}, "variants" {"het_indels" 147, "het_snps" 940, "homo_indels" 39, "homo_snps" 272, "no_call" 0, "other" 9, "variants" 1407}}, "IonXpress_015" {"hotspots" {}, "variants" {"het_indels" 148, "het_snps" 980, "homo_indels" 37, "homo_snps" 239, "no_call" 0, "other" 11, "variants" 1415}}, "IonXpress_018" {"hotspots" {}, "variants" {"het_indels" 154, "het_snps" 850, "homo_indels" 33, "homo_snps" 254, "no_call" 0, "other" 16, "variants" 1307}}}, "Configuration" "Somatic - PGM - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}}, :bam-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71.bam", :fastq-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/basecaller_results/R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71.fastq", :report-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/", :filesystem-path "/results/analysis/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103", :reference "hg19", :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/95/"], :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/95/"], :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/102/"], :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/101/"], :timestamp #inst "2013-11-18T09:53:43.000-00:00", :thumbnail? false, :plugin-result-set #{#ion_torrent_api.schema.PluginResult{:type :coverage, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 156, :uri "/rundb/api/v1/pluginresult/156/", :result-uri "/rundb/api/v1/results/103/", :result-name "Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", :state "Completed", :path "/results/analysis/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/plugin_out/coverageAnalysis_out", :report-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/", :name "coverageAnalysis", :version "4.0-r73765", :versioned-name "coverageAnalysis--v4.0-r73765", :library-type nil, :config-desc nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcode-result-map {"IonXpress_002" {"Number of amplicons" "15992", "Amplicons with no strand bias" "88.73%", "Target bases with no strand bias" "81.14%", "Sample Name" "INQ0136TT01-Q5-D", "Number of mapped reads" "6748981", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "86.31%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "641008255", "Average reads per amplicon" "396.5", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_002_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.03%", "Uniformity of base coverage" "85.52%", "Percent reads on target" "93.96%", "Total assigned amplicon reads" "6341404", "Percent assigned amplicon reads" "93.96%", "Amplicons with at least 100 reads" "83.95%", "Average base coverage depth" "362.1", "Target base coverage at 500x" "25.44%", "Amplicons with at least 500 reads" "30.23%", "Reference (File)" "hg19", "Percent base reads on target" "95.40%", "Total base reads on target" "611508036", "Target base coverage at 1x" "98.11%", "Target base coverage at 100x" "81.87%", "Amplicons with at least 1 read" "98.75%", "Amplicons reading end-to-end" "43.11%"}, "IonXpress_004" {"Number of amplicons" "15992", "Amplicons with no strand bias" "87.22%", "Target bases with no strand bias" "79.41%", "Sample Name" "INQ0152TT01-RP-D", "Number of mapped reads" "5166623", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.91%", "Target base coverage at 20x" "91.32%", "Total aligned base reads" "485332704", "Average reads per amplicon" "304.8", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_004_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.50%", "Uniformity of base coverage" "83.88%", "Percent reads on target" "94.33%", "Total assigned amplicon reads" "4873727", "Percent assigned amplicon reads" "94.33%", "Amplicons with at least 100 reads" "78.38%", "Average base coverage depth" "274.3", "Target base coverage at 500x" "13.88%", "Amplicons with at least 500 reads" "17.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.42%", "Total base reads on target" "463124273", "Target base coverage at 1x" "97.73%", "Target base coverage at 100x" "75.20%", "Amplicons with at least 1 read" "98.48%", "Amplicons reading end-to-end" "38.50%"}, "IonXpress_005" {"Number of amplicons" "15992", "Amplicons with no strand bias" "90.75%", "Target bases with no strand bias" "82.75%", "Sample Name" "INQ0141TT01-MP-D", "Number of mapped reads" "12498342", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "80.38%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "1118196811", "Average reads per amplicon" "729.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_005_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.20%", "Uniformity of base coverage" "79.35%", "Percent reads on target" "93.32%", "Total assigned amplicon reads" "11663441", "Percent assigned amplicon reads" "93.32%", "Amplicons with at least 100 reads" "84.87%", "Average base coverage depth" "628.9", "Target base coverage at 500x" "44.51%", "Amplicons with at least 500 reads" "50.69%", "Reference (File)" "hg19", "Percent base reads on target" "94.98%", "Total base reads on target" "1062030713", "Target base coverage at 1x" "98.44%", "Target base coverage at 100x" "82.40%", "Amplicons with at least 1 read" "99.18%", "Amplicons reading end-to-end" "31.76%"}, "IonXpress_006" {"Number of amplicons" "15992", "Amplicons with no strand bias" "91.70%", "Target bases with no strand bias" "83.93%", "Sample Name" "INQ0131TT01-7C-D", "Number of mapped reads" "8488131", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.81%", "Target base coverage at 20x" "91.72%", "Total aligned base reads" "775619927", "Average reads per amplicon" "496.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_006_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.90%", "Uniformity of base coverage" "78.86%", "Percent reads on target" "93.51%", "Total assigned amplicon reads" "7937065", "Percent assigned amplicon reads" "93.51%", "Amplicons with at least 100 reads" "79.67%", "Average base coverage depth" "437.5", "Target base coverage at 500x" "27.47%", "Amplicons with at least 500 reads" "32.13%", "Reference (File)" "hg19", "Percent base reads on target" "95.26%", "Total base reads on target" "738833701", "Target base coverage at 1x" "98.06%", "Target base coverage at 100x" "76.97%", "Amplicons with at least 1 read" "98.81%", "Amplicons reading end-to-end" "36.53%"}, "IonXpress_014" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.15%", "Target bases with no strand bias" "84.57%", "Sample Name" "INQ0160TT01-SH-D", "Number of mapped reads" "7185798", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.73%", "Target base coverage at 20x" "91.04%", "Total aligned base reads" "654660133", "Average reads per amplicon" "420.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_014_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.27%", "Uniformity of base coverage" "78.87%", "Percent reads on target" "93.46%", "Total assigned amplicon reads" "6716054", "Percent assigned amplicon reads" "93.46%", "Amplicons with at least 100 reads" "77.05%", "Average base coverage depth" "367.8", "Target base coverage at 500x" "22.45%", "Amplicons with at least 500 reads" "26.97%", "Reference (File)" "hg19", "Percent base reads on target" "94.87%", "Total base reads on target" "621043917", "Target base coverage at 1x" "97.84%", "Target base coverage at 100x" "73.68%", "Amplicons with at least 1 read" "98.61%", "Amplicons reading end-to-end" "36.99%"}, "IonXpress_015" {"Number of amplicons" "15992", "Amplicons with no strand bias" "89.66%", "Target bases with no strand bias" "82.33%", "Sample Name" "INQ0138TT01-5F-D", "Number of mapped reads" "7308490", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "81.82%", "Target base coverage at 20x" "91.77%", "Total aligned base reads" "667495055", "Average reads per amplicon" "427.4", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_015_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.86%", "Uniformity of base coverage" "80.87%", "Percent reads on target" "93.53%", "Total assigned amplicon reads" "6835733", "Percent assigned amplicon reads" "93.53%", "Amplicons with at least 100 reads" "79.56%", "Average base coverage depth" "376.4", "Target base coverage at 500x" "25.82%", "Amplicons with at least 500 reads" "31.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.21%", "Total base reads on target" "635549725", "Target base coverage at 1x" "97.99%", "Target base coverage at 100x" "76.46%", "Amplicons with at least 1 read" "98.77%", "Amplicons reading end-to-end" "37.39%"}, "IonXpress_018" {"Number of amplicons" "15992", "Amplicons with no strand bias" "84.50%", "Target bases with no strand bias" "77.75%", "Sample Name" "INQ0151TT01-2R-D", "Number of mapped reads" "8822299", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.30%", "Target base coverage at 20x" "93.70%", "Total aligned base reads" "841346599", "Average reads per amplicon" "523.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_018_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.50%", "Uniformity of base coverage" "83.59%", "Percent reads on target" "94.80%", "Total assigned amplicon reads" "8363758", "Percent assigned amplicon reads" "94.80%", "Amplicons with at least 100 reads" "84.82%", "Average base coverage depth" "475.6", "Target base coverage at 500x" "37.03%", "Amplicons with at least 500 reads" "41.67%", "Reference (File)" "hg19", "Percent base reads on target" "95.45%", "Total base reads on target" "803054084", "Target base coverage at 1x" "98.38%", "Target base coverage at 100x" "82.97%", "Amplicons with at least 1 read" "99.06%", "Amplicons reading end-to-end" "43.34%"}}, :barcoded? true, :start-time #inst "2013-11-18T09:53:47.000-00:00", :end-time #inst "2013-11-18T10:37:56.000-00:00", :raw-map {"size" "511029918", "store" {"Non-duplicate" "No", "Target padding" "0", "Targetted regions" "/results/uploads/BED/1/hg19/merged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "Uniquely mapped" "No", "barcoded" "true", "barcodes" {"IonXpress_002" {"Number of amplicons" "15992", "Amplicons with no strand bias" "88.73%", "Target bases with no strand bias" "81.14%", "Sample Name" "INQ0136TT01-Q5-D", "Number of mapped reads" "6748981", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "86.31%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "641008255", "Average reads per amplicon" "396.5", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_002_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.03%", "Uniformity of base coverage" "85.52%", "Percent reads on target" "93.96%", "Total assigned amplicon reads" "6341404", "Percent assigned amplicon reads" "93.96%", "Amplicons with at least 100 reads" "83.95%", "Average base coverage depth" "362.1", "Target base coverage at 500x" "25.44%", "Amplicons with at least 500 reads" "30.23%", "Reference (File)" "hg19", "Percent base reads on target" "95.40%", "Total base reads on target" "611508036", "Target base coverage at 1x" "98.11%", "Target base coverage at 100x" "81.87%", "Amplicons with at least 1 read" "98.75%", "Amplicons reading end-to-end" "43.11%"}, "IonXpress_004" {"Number of amplicons" "15992", "Amplicons with no strand bias" "87.22%", "Target bases with no strand bias" "79.41%", "Sample Name" "INQ0152TT01-RP-D", "Number of mapped reads" "5166623", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.91%", "Target base coverage at 20x" "91.32%", "Total aligned base reads" "485332704", "Average reads per amplicon" "304.8", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_004_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.50%", "Uniformity of base coverage" "83.88%", "Percent reads on target" "94.33%", "Total assigned amplicon reads" "4873727", "Percent assigned amplicon reads" "94.33%", "Amplicons with at least 100 reads" "78.38%", "Average base coverage depth" "274.3", "Target base coverage at 500x" "13.88%", "Amplicons with at least 500 reads" "17.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.42%", "Total base reads on target" "463124273", "Target base coverage at 1x" "97.73%", "Target base coverage at 100x" "75.20%", "Amplicons with at least 1 read" "98.48%", "Amplicons reading end-to-end" "38.50%"}, "IonXpress_005" {"Number of amplicons" "15992", "Amplicons with no strand bias" "90.75%", "Target bases with no strand bias" "82.75%", "Sample Name" "INQ0141TT01-MP-D", "Number of mapped reads" "12498342", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "80.38%", "Target base coverage at 20x" "93.18%", "Total aligned base reads" "1118196811", "Average reads per amplicon" "729.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_005_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.20%", "Uniformity of base coverage" "79.35%", "Percent reads on target" "93.32%", "Total assigned amplicon reads" "11663441", "Percent assigned amplicon reads" "93.32%", "Amplicons with at least 100 reads" "84.87%", "Average base coverage depth" "628.9", "Target base coverage at 500x" "44.51%", "Amplicons with at least 500 reads" "50.69%", "Reference (File)" "hg19", "Percent base reads on target" "94.98%", "Total base reads on target" "1062030713", "Target base coverage at 1x" "98.44%", "Target base coverage at 100x" "82.40%", "Amplicons with at least 1 read" "99.18%", "Amplicons reading end-to-end" "31.76%"}, "IonXpress_006" {"Number of amplicons" "15992", "Amplicons with no strand bias" "91.70%", "Target bases with no strand bias" "83.93%", "Sample Name" "INQ0131TT01-7C-D", "Number of mapped reads" "8488131", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.81%", "Target base coverage at 20x" "91.72%", "Total aligned base reads" "775619927", "Average reads per amplicon" "496.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_006_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.90%", "Uniformity of base coverage" "78.86%", "Percent reads on target" "93.51%", "Total assigned amplicon reads" "7937065", "Percent assigned amplicon reads" "93.51%", "Amplicons with at least 100 reads" "79.67%", "Average base coverage depth" "437.5", "Target base coverage at 500x" "27.47%", "Amplicons with at least 500 reads" "32.13%", "Reference (File)" "hg19", "Percent base reads on target" "95.26%", "Total base reads on target" "738833701", "Target base coverage at 1x" "98.06%", "Target base coverage at 100x" "76.97%", "Amplicons with at least 1 read" "98.81%", "Amplicons reading end-to-end" "36.53%"}, "IonXpress_014" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.15%", "Target bases with no strand bias" "84.57%", "Sample Name" "INQ0160TT01-SH-D", "Number of mapped reads" "7185798", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "79.73%", "Target base coverage at 20x" "91.04%", "Total aligned base reads" "654660133", "Average reads per amplicon" "420.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_014_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.27%", "Uniformity of base coverage" "78.87%", "Percent reads on target" "93.46%", "Total assigned amplicon reads" "6716054", "Percent assigned amplicon reads" "93.46%", "Amplicons with at least 100 reads" "77.05%", "Average base coverage depth" "367.8", "Target base coverage at 500x" "22.45%", "Amplicons with at least 500 reads" "26.97%", "Reference (File)" "hg19", "Percent base reads on target" "94.87%", "Total base reads on target" "621043917", "Target base coverage at 1x" "97.84%", "Target base coverage at 100x" "73.68%", "Amplicons with at least 1 read" "98.61%", "Amplicons reading end-to-end" "36.99%"}, "IonXpress_015" {"Number of amplicons" "15992", "Amplicons with no strand bias" "89.66%", "Target bases with no strand bias" "82.33%", "Sample Name" "INQ0138TT01-5F-D", "Number of mapped reads" "7308490", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "81.82%", "Target base coverage at 20x" "91.77%", "Total aligned base reads" "667495055", "Average reads per amplicon" "427.4", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_015_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "92.86%", "Uniformity of base coverage" "80.87%", "Percent reads on target" "93.53%", "Total assigned amplicon reads" "6835733", "Percent assigned amplicon reads" "93.53%", "Amplicons with at least 100 reads" "79.56%", "Average base coverage depth" "376.4", "Target base coverage at 500x" "25.82%", "Amplicons with at least 500 reads" "31.20%", "Reference (File)" "hg19", "Percent base reads on target" "95.21%", "Total base reads on target" "635549725", "Target base coverage at 1x" "97.99%", "Target base coverage at 100x" "76.46%", "Amplicons with at least 1 read" "98.77%", "Amplicons reading end-to-end" "37.39%"}, "IonXpress_018" {"Number of amplicons" "15992", "Amplicons with no strand bias" "84.50%", "Target bases with no strand bias" "77.75%", "Sample Name" "INQ0151TT01-2R-D", "Number of mapped reads" "8822299", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.30%", "Target base coverage at 20x" "93.70%", "Total aligned base reads" "841346599", "Average reads per amplicon" "523.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpress_018_R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42_Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", "Amplicons with at least 20 reads" "94.50%", "Uniformity of base coverage" "83.59%", "Percent reads on target" "94.80%", "Total assigned amplicon reads" "8363758", "Percent assigned amplicon reads" "94.80%", "Amplicons with at least 100 reads" "84.82%", "Average base coverage depth" "475.6", "Target base coverage at 500x" "37.03%", "Amplicons with at least 500 reads" "41.67%", "Reference (File)" "hg19", "Percent base reads on target" "95.45%", "Total base reads on target" "803054084", "Target base coverage at 1x" "98.38%", "Target base coverage at 100x" "82.97%", "Amplicons with at least 1 read" "99.06%", "Amplicons reading end-to-end" "43.34%"}}}, "config" {}, "endtime" "2013-11-18T10:37:56.000983+00:00", "inodes" "264", "starttime" "2013-11-18T09:53:47.000378+00:00", "owner" {"last_login" "2014-04-14T23:02:27.000581+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "coverageAnalysis--v4.0-r73765", "config" {}, "path" "", "active" false, "autorunMutable" true, "script" "coverageAnalysis.py", "name" "coverageAnalysis", "isConfig" false, "date" "2013-10-22T06:09:40.000309+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" [], "runlevel" ["default"], "runtype" ["wholechip" "thumbnail" "composite"]}, "version" "4.0-r73765", "userinputfields" {}, "id" 44, "resource_uri" "/rundb/api/v1/plugin/44/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'coverageAnalysis' v4.0-r73765", "isInstance" false}, "duration" "0:44:09.605601", "jobid" nil}} #ion_torrent_api.schema.PluginResult{:type :sample-id, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 157, :uri "/rundb/api/v1/pluginresult/157/", :result-uri "/rundb/api/v1/results/103/", :result-name "Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", :state "Completed", :path "/results/analysis/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/plugin_out/sampleID_out", :report-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/", :name "sampleID", :version "4.0-r73765", :versioned-name "sampleID--v4.0-r73765", :library-type "", :config-desc nil, :target-name "", :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcode-result-map {"IonXpress_002" "F-TKRYRSRT", "IonXpress_004" "F-YGAYRSAA", "IonXpress_005" "F-TKAYGSGW", "IonXpress_006" "F-TKG?GSAT", "IonXpress_014" "F-TTGYRSRW", "IonXpress_015" "F-TGGYGCAT", "IonXpress_018" "F-YTAYGCGT"}, :barcoded? true, :start-time #inst "2013-11-18T09:53:47.000-00:00", :end-time #inst "2013-11-18T10:39:18.000-00:00", :raw-map {"size" "423349", "store" {"Library Type" "", "Target Loci" "", "Target Regions" "", "Variant Detection" "", "barcoded" "true", "barcodes" {"IonXpress_002" {"SampleID" "F-TKRYRSRT", "hotspot_coverage" {"Average base coverage depth" "2450.6", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1946", "Male sample ID region reads" "0", "Number of mapped reads" "6748981", "Number of reads in sample ID regions" "24628", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "641008255"}, "target_coverage" {"Average base coverage depth" "2349.8", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_004" {"SampleID" "F-YGAYRSAA", "hotspot_coverage" {"Average base coverage depth" "1762.8", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1112", "Male sample ID region reads" "0", "Number of mapped reads" "5166623", "Number of reads in sample ID regions" "17951", "Percent base reads in sample ID regions" "0.34%", "Percent reads in sample ID regions" "0.35%", "Total base reads in sample ID regions" "485332704"}, "target_coverage" {"Average base coverage depth" "1687.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_005" {"SampleID" "F-TKAYGSGW", "hotspot_coverage" {"Average base coverage depth" "5172.9", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2686", "Male sample ID region reads" "0", "Number of mapped reads" "12498342", "Number of reads in sample ID regions" "52649", "Percent base reads in sample ID regions" "0.42%", "Percent reads in sample ID regions" "0.42%", "Total base reads in sample ID regions" "1118196811"}, "target_coverage" {"Average base coverage depth" "4781.7", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_006" {"SampleID" "F-TKG?GSAT", "hotspot_coverage" {"Average base coverage depth" "3259.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1512", "Male sample ID region reads" "0", "Number of mapped reads" "8488131", "Number of reads in sample ID regions" "32457", "Percent base reads in sample ID regions" "0.38%", "Percent reads in sample ID regions" "0.38%", "Total base reads in sample ID regions" "775619927"}, "target_coverage" {"Average base coverage depth" "2961.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_014" {"SampleID" "F-TTGYRSRW", "hotspot_coverage" {"Average base coverage depth" "2576.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1002", "Male sample ID region reads" "0", "Number of mapped reads" "7185798", "Number of reads in sample ID regions" "25805", "Percent base reads in sample ID regions" "0.35%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "654660133"}, "target_coverage" {"Average base coverage depth" "2338.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_015" {"SampleID" "F-TGGYGCAT", "hotspot_coverage" {"Average base coverage depth" "2599.4", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1593", "Male sample ID region reads" "0", "Number of mapped reads" "7308490", "Number of reads in sample ID regions" "26810", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "667495055"}, "target_coverage" {"Average base coverage depth" "2446.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_018" {"SampleID" "F-YTAYGCGT", "hotspot_coverage" {"Average base coverage depth" "3248.2", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2115", "Male sample ID region reads" "0", "Number of mapped reads" "8822299", "Number of reads in sample ID regions" "32819", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "841346599"}, "target_coverage" {"Average base coverage depth" "3067.5", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}}}, "config" {}, "endtime" "2013-11-18T10:39:18.000987+00:00", "inodes" "79", "starttime" "2013-11-18T09:53:47.000320+00:00", "owner" {"last_login" "2014-04-12T06:52:17.000339+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "sampleID--v4.0-r73765", "config" {}, "path" "/results/plugins/sampleID", "active" true, "autorunMutable" true, "script" "sampleID.py", "name" "sampleID", "isConfig" false, "date" "2013-10-22T06:09:39.000975+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["wholechip" "thumbnail" "composite"]}, "version" "4.0-r73765", "userinputfields" {}, "id" 40, "resource_uri" "/rundb/api/v1/plugin/40/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'sampleID' v4.0-r73765", "isInstance" false}, "duration" "0:45:31.666857", "jobid" nil}} #ion_torrent_api.schema.PluginResult{:type :tsvc, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 158, :uri "/rundb/api/v1/pluginresult/158/", :result-uri "/rundb/api/v1/results/103/", :result-name "Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", :state "Completed", :path "/results/analysis/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/plugin_out/variantCaller_out", :report-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/", :name "variantCaller", :version "4.0-r73742", :versioned-name "variantCaller--v4.0-r73742", :library-type "AmpliSeq", :config-desc "Somatic - PGM - Low Stringency", :target-name "4477685_Comprehensive_CCP_bedfile_20120517", :target-bed "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", :experiment-name "R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", :trimmed-reads? true, :barcode-result-map {"IonXpress_002" {"hotspots" {}, "variants" {"het_indels" 160, "het_snps" 762, "homo_indels" 36, "homo_snps" 281, "no_call" 0, "other" 10, "variants" 1249}}, "IonXpress_004" {"hotspots" {}, "variants" {"het_indels" 171, "het_snps" 908, "homo_indels" 37, "homo_snps" 234, "no_call" 0, "other" 7, "variants" 1357}}, "IonXpress_005" {"hotspots" {}, "variants" {"het_indels" 179, "het_snps" 1100, "homo_indels" 31, "homo_snps" 263, "no_call" 0, "other" 13, "variants" 1586}}, "IonXpress_006" {"hotspots" {}, "variants" {"het_indels" 159, "het_snps" 904, "homo_indels" 35, "homo_snps" 274, "no_call" 0, "other" 8, "variants" 1380}}, "IonXpress_014" {"hotspots" {}, "variants" {"het_indels" 147, "het_snps" 940, "homo_indels" 39, "homo_snps" 272, "no_call" 0, "other" 9, "variants" 1407}}, "IonXpress_015" {"hotspots" {}, "variants" {"het_indels" 148, "het_snps" 980, "homo_indels" 37, "homo_snps" 239, "no_call" 0, "other" 11, "variants" 1415}}, "IonXpress_018" {"hotspots" {}, "variants" {"het_indels" 154, "het_snps" 850, "homo_indels" 33, "homo_snps" 254, "no_call" 0, "other" 16, "variants" 1307}}}, :barcoded? true, :start-time #inst "2013-11-18T10:37:59.000-00:00", :end-time #inst "2013-11-18T15:54:12.000-00:00", :raw-map {"size" "22601236340", "store" {"Aligned Reads" "R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpress_002" {"hotspots" {}, "variants" {"het_indels" 160, "het_snps" 762, "homo_indels" 36, "homo_snps" 281, "no_call" 0, "other" 10, "variants" 1249}}, "IonXpress_004" {"hotspots" {}, "variants" {"het_indels" 171, "het_snps" 908, "homo_indels" 37, "homo_snps" 234, "no_call" 0, "other" 7, "variants" 1357}}, "IonXpress_005" {"hotspots" {}, "variants" {"het_indels" 179, "het_snps" 1100, "homo_indels" 31, "homo_snps" 263, "no_call" 0, "other" 13, "variants" 1586}}, "IonXpress_006" {"hotspots" {}, "variants" {"het_indels" 159, "het_snps" 904, "homo_indels" 35, "homo_snps" 274, "no_call" 0, "other" 8, "variants" 1380}}, "IonXpress_014" {"hotspots" {}, "variants" {"het_indels" 147, "het_snps" 940, "homo_indels" 39, "homo_snps" 272, "no_call" 0, "other" 9, "variants" 1407}}, "IonXpress_015" {"hotspots" {}, "variants" {"het_indels" 148, "het_snps" 980, "homo_indels" 37, "homo_snps" 239, "no_call" 0, "other" 11, "variants" 1415}}, "IonXpress_018" {"hotspots" {}, "variants" {"het_indels" 154, "het_snps" 850, "homo_indels" 33, "homo_snps" 254, "no_call" 0, "other" 16, "variants" 1307}}}, "Configuration" "Somatic - PGM - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2013-11-18T15:54:12.000188+00:00", "inodes" "534", "starttime" "2013-11-18T10:37:59.000100+00:00", "owner" {"last_login" "2014-04-14T23:02:27.000581+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r73742", "config" {}, "path" "", "active" false, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-10-22T06:09:40.000450+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r73742", "userinputfields" {}, "id" 45, "resource_uri" "/rundb/api/v1/plugin/45/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'variantCaller' v4.0-r73742", "isInstance" false}, "duration" "5:16:13.088145", "jobid" nil}}}, :lib-metrics-set #{{"sampled_q47_alignments" 0, "extrapolated_mapped_bases_in_q10_alignments" "0", "sampled_200q20_reads" 0, "i600Q20_reads" 0, "q20_alignments" 43501022, "sampled_300q10_reads" 0, "extrapolated_q47_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q20_alignments" "0", "sampled_100q20_reads" 0, "s50Q20" 0, "i500Q20_reads" 0, "sampled_200q10_reads" 0, "i600Q10_reads" 0, "extrapolated_from_number_of_sampled_reads" 0, "sampled_q47_coverage_percentage" 0.0, "q47_mapped_bases" "3187303993", "sampled_q17_longest_alignment" 0, "q20_coverage_percentage" 0.0, "sampled_100q10_reads" 0, "i500Q10_reads" 0, "r50Q20" 0, "q10_coverage_percentage" 0.0, "s50Q10" 0, "extrapolated_50q20_reads" 0, "i400Q20_reads" 0, "extrapolated_50q10_reads" 0, "cf" 0.210523931309581, "q7_mean_alignment_length" 97, "r50Q10" 0, "q47_longest_alignment" 227, "sNumAlignments" 0, "sampled_400q47_reads" 0, "i300Q20_reads" 0, "i400Q10_reads" 0, "align_sample" 0, "sampled_300q47_reads" 0, "i200Q20_reads" 394, "i300Q10_reads" 0, "sampled_q17_coverage_percentage" 0.0, "sampled_50q7_reads" 0, "sampled_q10_alignments" 0, "sampled_q7_alignments" 0, "extrapolated_q17_mean_alignment_length" 0, "rNumAlignments" 0, "i200Q10_reads" 1553, "q10_mapped_bases" "5141745381", "extrapolated_100q7_reads" 0, "total_mapped_target_bases" "5241711527", "sampled_q20_alignments" 0, "sampled_200q47_reads" 0, "i600Q47_reads" 0, "i100Q20_reads" 18888095, "extrapolated_q7_longest_alignment" 0, "extrapolated_q20_mean_coverage_depth" 0.0, "q17_longest_alignment" 235, "sampled_100q47_reads" 0, "i500Q47_reads" 0, "q20_mapped_bases" "3656628660", "i100Q10_reads" 27365394, "i550Q20_reads" 0, "sampled_400q17_reads" 0, "extrapolated_200q7_reads" 0, "q47_coverage_percentage" 0.0, "sampled_q20_mean_alignment_length" 0, "i400Q47_reads" 0, "i150Q7_reads" 146841, "rMeanAlignLen" 0, "i450Q20_reads" 0, "extrapolated_50q47_reads" 0, "sampled_300q17_reads" 0, "extrapolated_300q7_reads" 0, "i550Q10_reads" 0, "extrapolated_q10_mean_coverage_depth" 0.0, "i350Q20_reads" 0, "sampled_200q17_reads" 0, "i600Q17_reads" 0, "i450Q10_reads" 0, "extrapolated_400q7_reads" 0, "sampled_q7_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q7_alignments" "0", "sampled_q10_mean_alignment_length" 0, "extrapolated_q7_coverage_percentage" 0.0, "extrapolated_q17_alignments" 0, "i50Q7_reads" 48495571, "i300Q47_reads" 0, "i250Q7_reads" 0, "sMeanAlignLen" 0, "s50Q17" 0, "duplicate_reads" nil, "extrapolated_400q20_reads" 0, "i200Q47_reads" 62, "i350Q7_reads" 0, "q7_alignments" 53326206, "i250Q20_reads" 0, "sampled_100q17_reads" 0, "i500Q17_reads" 0, "q17_coverage_percentage" 0.0, "i50Q20_reads" 32734590, "i350Q10_reads" 0, "extrapolated_300q20_reads" 0, "i100Q47_reads" 11698830, "r50Q17" 0, "extrapolated_400q10_reads" 0, "i450Q7_reads" 0, "i150Q20_reads" 40819, "i400Q17_reads" 0, "i250Q10_reads" 0, "i50Q10_reads" 48052604, "extrapolated_50q17_reads" 0, "sysSNR" 11.3101695367594, "i300Q17_reads" 0, "i150Q10_reads" 116168, "extrapolated_q47_mean_coverage_depth" 0.0, "extrapolated_q47_alignments" 0, "Index_Version" "tmap-f3", "extrapolated_200q20_reads" 0, "total_number_of_sampled_reads" 0, "extrapolated_300q10_reads" 0, "i550Q47_reads" 0, "i550Q7_reads" 0, "q20_mean_alignment_length" 84, "extrapolated_100q20_reads" 0, "total_mapped_reads" "56802039", "extrapolated_200q10_reads" 0, "i450Q47_reads" 0, "sampled_q47_mean_alignment_length" 0, "i100Q7_reads" 27601161, "i200Q17_reads" 831, "q17_qscore_bases" "0", "extrapolated_100q10_reads" 0, "i350Q47_reads" 0, "rLongestAlign" 0, "q10_mean_alignment_length" 97, "i100Q17_reads" 22491863, "i200Q7_reads" 1634, "sampled_50q20_reads" 0, "dr" 0.410582590848207, "i300Q7_reads" 0, "i550Q17_reads" 0, "extrapolated_q17_mean_coverage_depth" 0.0, "extrapolated_q20_longest_alignment" 0, "sampled_50q10_reads" 0, "extrapolated_400q47_reads" 0, "i250Q47_reads" 0, "sLongestAlign" 0, "sampled_mapped_bases_in_q17_alignments" "0", "i50Q47_reads" 29855240, "q7_mapped_bases" "5190055317", "extrapolated_q10_alignments" 0, "extrapolated_300q47_reads" 0, "extrapolated_50q7_reads" 0, "sampled_q17_mean_alignment_length" 0, "i150Q47_reads" 18994, "r200Q20" 0, "q47_qscore_bases" "0", "extrapolated_q10_longest_alignment" 0, "i400Q7_reads" 0, "i450Q17_reads" 0, "Genome_Version" "hg19", "s200Q20" 0, "r100Q20" 0, "i500Q7_reads" 0, "i350Q17_reads" 0, "r200Q10" 0, "sampled_100q7_reads" 0, "extrapolated_q20_alignments" 0, "raw_accuracy" 98.4, "extrapolated_200q47_reads" 0, "i600Q7_reads" 0, "i250Q17_reads" 0, "s100Q20" 0, "sampled_200q7_reads" 0, "extrapolated_q20_coverage_percentage" 0.0, "i50Q17_reads" 41074187, "s200Q10" 0, "r100Q10" 0, "extrapolated_100q47_reads" 0, "sampled_q20_mean_coverage_depth" 0.0, "q47_mean_alignment_length" 75, "sampled_mapped_bases_in_q47_alignments" "0", "extrapolated_400q17_reads" 0, "s100Q10" 0, "extrapolated_300q17_reads" 0, "extrapolated_q10_coverage_percentage" 0.0, "sampled_50q47_reads" 0, "i150Q17_reads" 70340, "sampled_q7_mean_coverage_depth" 0.0, "sampled_q10_mean_coverage_depth" 0.0, "sampled_300q7_reads" 0, "q10_qscore_bases" "0", "extrapolated_200q17_reads" 0, "extrapolated_q7_mean_alignment_length" 0, "extrapolated_q47_longest_alignment" 0, "sampled_mapped_bases_in_q7_alignments" "0", "sampled_400q7_reads" 0, "aveKeyCounts" 82.0, "report" "/rundb/api/v1/results/103/", "q17_mean_alignment_length" 92, "q20_qscore_bases" "0", "extrapolated_100q17_reads" 0, "sampled_50q17_reads" 0, "sampled_mapped_bases_in_q10_alignments" "0", "q7_longest_alignment" 247, "sampled_mapped_bases_in_q20_alignments" "0", "extrapolated_q47_coverage_percentage" 0.0, "sampled_q47_mean_coverage_depth" 0.0, "extrapolated_q17_longest_alignment" 0, "genome" "hg19", "r200Q17" 0, "genomesize" "3095693981", "q7_coverage_percentage" 0.0, "s200Q17" 0, "r100Q17" 0, "q17_alignments" 46641652, "extrapolated_q7_alignments" 0, "s100Q17" 0, "extrapolated_q17_coverage_percentage" 0.0, "sampled_q20_longest_alignment" 0, "sampled_q17_mean_coverage_depth" 0.0, "extrapolated_mapped_bases_in_q17_alignments" "0", "rCoverage" 0.0, "sampled_q7_longest_alignment" 0, "sampled_q10_longest_alignment" 0, "id" 95, "totalNumReads" 59805830, "q47_alignments" 42349634, "resource_uri" "/rundb/api/v1/libmetrics/95/", "sCoverage" 0.0, "ie" 0.613608676940203, "extrapolated_q20_mean_alignment_length" 0, "sampled_q20_coverage_percentage" 0.0, "extrapolated_mapped_bases_in_q47_alignments" "0", "q7_qscore_bases" "0", "sampled_q17_alignments" 0, "extrapolated_q7_mean_coverage_depth" 0.0, "genomelength" 0, "extrapolated_q10_mean_alignment_length" 0, "sampled_q7_coverage_percentage" 0.0, "sampled_q10_coverage_percentage" 0.0, "sampled_400q20_reads" 0, "sampled_q47_longest_alignment" 0, "q20_longest_alignment" 233, "q17_mapped_bases" "4310514065", "q10_alignments" 52811412, "sampled_400q10_reads" 0, "q10_longest_alignment" 247, "sampled_300q20_reads" 0}}, :tf-metrics-set #{{"aveKeyCount" 104, "HPAccuracy" "0 : 130746804/131390012, 1 : 93894408/94767457, 2 : 4206853/4278896, 3 : 0/0, 4 : 1004894/1069552, 5 : 0/0, 6 : 0/0, 7 : 0/0", "sequence" "TACGAGCGTGTAGACGTGTCGTACGTGCGACGTAGTGAGTATACATGCTCTGACACTATGTACGATCTGAGACTGCCAAGGCACACAGGGGATAGG", "Q17ReadCount" 977763, "corrHPSNR" "", "Q10ReadCount" 1037991, "number" 1092958, "SysSNR" 11.8936850996752, "name" "TF_C", "keypass" 1092958, "report" "/rundb/api/v1/results/103/", "Q10Mean" 92, "Q17Histo" "5085 8923 4708 6972 2028 970 4920 2808 3627 3733 2107 2433 2349 4597 3885 4861 2209 1976 2521 5989 3157 3553 1076 2674 1165 765 1909 4452 1160 1514 794 1274 3414 1152 310 214 1713 1058 604 634 164 1412 1295 953 352 240 162 792 398 134 1704 1198 822 892 352 1943 1033 1293 832 478 606 634 513 437 512 315 325 302 207 403 348 945 268 210 370 376 1121 198 391 60 271 55 223 170 151 311 343 835 99 386 8497 24643 3394 517 688 12717 860487 44888 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0", "id" 95, "resource_uri" "/rundb/api/v1/tfmetrics/95/", "Q17Mean" 86, "Q10Histo" "1954 2035 1294 962 384 149 544 210 1718 1001 994 1100 962 2385 2251 2926 1573 1102 1331 2656 1969 2654 1305 1614 818 560 950 2288 811 686 611 1342 2730 966 381 250 547 862 404 477 474 839 663 843 600 303 235 503 446 305 362 331 251 230 236 233 315 234 168 145 189 172 124 105 99 92 59 65 59 63 66 110 89 95 80 119 74 113 90 65 112 124 103 115 152 174 348 271 217 659 8939 25253 4881 3258 3121 14849 864022 48647 20447 11949 8273 5812 4369 3201 2372 1361 529 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"}}, :analysis-metrics-set #{{"keypass_all_beads" 0, "washout_dud" 0, "tfFinal" 1111113, "lib" 128962409, "bead" 130478436, "sysIE" 0.613608676940203, "excluded" 16095180, "tfMix" 0, "libKp" 0, "washout_library" 0, "live" 130125788, "empty" 11838499, "washout_test_fragment" 0, "washout" 0, "lib_pass_basecaller" 0, "lib_pass_cafie" 0, "sysCF" 0.210523931309581, "libLive" 0, "libFinal" 59842839, "ignored" 5299710, "tf" 1163379, "dud" 352648, "report" "/rundb/api/v1/results/103/", "amb" 0, "libMix" 0, "tfKp" 0, "pinned" 987311, "sysDR" 0.410582590848207, "id" 102, "resource_uri" "/rundb/api/v1/analysismetrics/102/", "washout_ambiguous" 0, "washout_live" 0, "tfLive" 0}}, :quality-metrics-set #{{"q0_bases" "5585491106", "q20_max_read_length" 297.0, "q20_mean_read_length" 61, "q0_50bp_reads" 49971173, "q20_150bp_reads" 329919, "q17_max_read_length" 299, "q20_100bp_reads" 16619543, "q20_reads" 59805830, "q17_50bp_reads" 44005343, "q17_150bp_reads" 1096007, "q20_bases" "4426799345", "q0_max_read_length" 300, "report" "/rundb/api/v1/results/103/", "q17_100bp_reads" 26005341, "q17_reads" 59805830, "q0_mean_read_length" 93.0, "q20_50bp_reads" 33075942, "q0_150bp_reads" 1500239, "q17_bases" "4778449244", "id" 101, "resource_uri" "/rundb/api/v1/qualitymetrics/101/", "q0_100bp_reads" 29739466, "q17_mean_read_length" 82.0, "q0_reads" 59805830}}, :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 143462, "log" "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/log.html", "runid" "HHGW0", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "BTO38", "metaData" {}, "resultsType" "", "timeStamp" "2013-11-18T09:53:43.000114+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/69/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}, :raw-map {"sequencekitname" "IonPGM200Kit-v2", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 214054, "flows" 400, "baselineRun" false, "samples" [{"externalId" "2169-100B", "name" "INQ0141TT01-MP-D", "displayedName" "INQ0141TT01-MP-D", "date" "2013-11-18T01:14:57.000502+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 224, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/224/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0138TT01-5F-D", "displayedName" "INQ0138TT01-5F-D", "date" "2013-11-18T01:14:57.000506+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 225, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/225/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0131TT01-7C-D", "displayedName" "INQ0131TT01-7C-D", "date" "2013-11-18T01:14:57.000508+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 226, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/226/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0160TT01-SH-D", "displayedName" "INQ0160TT01-SH-D", "date" "2013-11-18T01:14:57.000510+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 227, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/227/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0136TT01-Q5-D", "displayedName" "INQ0136TT01-Q5-D", "date" "2013-11-18T01:14:57.000513+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 228, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/228/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0152TT01-RP-D", "displayedName" "INQ0152TT01-RP-D", "date" "2013-11-18T01:14:57.000515+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 229, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/229/", "description" "6pM"} {"externalId" "2169-100B", "name" "INQ0151TT01-2R-D", "displayedName" "INQ0151TT01-2R-D", "date" "2013-11-18T01:14:57.000517+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/71/"], "id" 230, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/230/", "description" "6pM"}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/74/", "sample" "INQ0151TT01-2R-D", "resultDate" "2013-11-18T09:53:43.000114+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-11-17T12:33:49+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_11_17_22_32_21_user_XXX-43-AmpliSeq_CCP_1-42", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "stage1 map4", "barcodeKitName" "IonXpress", "prethumbnailbasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 100000 --calibration-training=100000 --flow-signals-type scaled-residual", "libraryKey" "TCAG", "thumbnailbasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 100000", "selectedPlugins" {"Alignment" {"features" [], "id" 42, "name" "Alignment", "userInput" "", "version" "4.0-r70791"}, "coverageAnalysis" {"features" [], "id" 44, "name" "coverageAnalysis", "userInput" "", "version" "4.0-r73765"}, "sampleID" {"features" [], "id" 40, "name" "sampleID", "userInput" "", "version" "4.0-r73765"}, "variantCaller" {"features" [], "id" 45, "name" "variantCaller", "userInput" {"freebayes" {"allow_indels" "1", "min_mapping_qv" "4", "gen_min_coverage" "6", "min_base_qv" "2", "gen_min_alt_allele_freq" "0.03", "gen_min_indel_alt_allele_freq" "0.1", "read_max_mismatch_fraction" "1", "read_mismatch_limit" "10", "allow_snps" "1", "allow_mnps" "0"}, "long_indel_assembler" {"kmer_len" "19", "max_hp_length" "8", "min_indel_size" "4", "min_var_count" "5", "min_var_freq" "0.15", "relative_strand_bias" "0.8", "short_suffix_match" "5"}, "meta" {"configuration" "somatic_low_stringency", "trimreads" true, "tvcargs" "tvc"}, "torrent_variant_caller" {"snp_min_variant_score" "6", "hotspot_strand_bias" "0.95", "snp_min_allele_freq" "0.02", "data_quality_stringency" "6.5", "hotspot_min_coverage" "6", "filter_unusual_predictions" "0.3", "heavy_tailed" "3", "snp_strand_bias" "0.95", "indel_min_cov_each_strand" "2", "suppress_recalibration" "1", "filter_insertion_predictions" "0.2", "indel_min_coverage" "15", "prediction_precision" "1", "outlier_probability" "0.005", "hotspot_min_cov_each_strand" "2", "do_snp_realignment" "1", "indel_min_variant_score" "6", "indel_min_allele_freq" "0.05", "snp_min_cov_each_strand" "0", "hp_max_length" "8", "downsample_to_coverage" "2000", "indel_strand_bias" "0.9", "filter_deletion_predictions" "0.2", "hotspot_min_variant_score" "6", "hotspot_min_allele_freq" "0.01", "snp_min_coverage" "6"}}, "version" "4.0-r73742"}}, "thumbnailanalysisargs" "Analysis --from-beadfind --clonal-filter-bkgmodel on --region-size=100x100 --bkg-bfmask-update off --gpuWorkLoad 1 --bkg-debug-param 1 --beadfind-thumbnail 1", "barcodedSamples" {"INQ0131TT01-7C-D" {"barcodeSampleInfo" {"IonXpress_006" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_006"]}, "INQ0136TT01-Q5-D" {"barcodeSampleInfo" {"IonXpress_002" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_002"]}, "INQ0138TT01-5F-D" {"barcodeSampleInfo" {"IonXpress_015" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_015"]}, "INQ0141TT01-MP-D" {"barcodeSampleInfo" {"IonXpress_005" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_005"]}, "INQ0151TT01-2R-D" {"barcodeSampleInfo" {"IonXpress_018" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_018"]}, "INQ0152TT01-RP-D" {"barcodeSampleInfo" {"IonXpress_004" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_004"]}, "INQ0160TT01-SH-D" {"barcodeSampleInfo" {"IonXpress_014" {"description" "6pM", "externalId" "2169-100B"}}, "barcodes" ["IonXpress_014"]}}, "libraryKitBarcode" nil, "libraryKitName" "Ion AmpliSeq 2.0 Library Kit", "thumbnailbeadfindargs" "justBeadFind --beadfind-minlivesnr 3 --region-size=100x100 --beadfind-thumbnail 1", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-11-18T01:14:57.000497+00:00", "status" "run", "thumbnailalignmentargs" "stage1 map4", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/103/" "/rundb/api/v1/results/104/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 1000", "analysisargs" "Analysis --from-beadfind --clonal-filter-bkgmodel on --region-size=216x224 --bkg-bfmask-update off --gpuWorkLoad 1 --total-timeout 600", "hotSpotRegionBedFile" "", "id" 69, "resource_uri" "/rundb/api/v1/experimentanalysissettings/69/", "prebasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 1000 --calibration-training=100000 --flow-signals-type scaled-residual --max-phasing-levels 2", "isDuplicateReads" false, "beadfindargs" "justBeadFind --beadfind-minlivesnr 3 --region-size=216x224 --total-timeout 600", "experiment" "/rundb/api/v1/experiment/71/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}
        e71)

(def pr157 (with-fake-routes-in-isolation
             {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                      {:status 200 :headers {"Content-Type" "application/json"}
                                       :body (slurp (uri-to-file uri :json))})}
             (plugin-result ts 157)))

(expect ion_torrent_api.schema.Experiment e71)

(expect #ion_torrent_api.schema.PluginResult{:type :sample-id, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1, :api-path "/rundb/api/v1/"}, :id 157, :uri "/rundb/api/v1/pluginresult/157/", :result-uri "/rundb/api/v1/results/103/", :result-name "Auto_user_XXX-43-AmpliSeq_CCP_1-42_71", :state "Completed", :path "/results/analysis/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/plugin_out/sampleID_out", :report-link "/output/Home/Auto_user_XXX-43-AmpliSeq_CCP_1-42_71_103/", :name "sampleID", :version "4.0-r73765", :versioned-name "sampleID--v4.0-r73765", :library-type "", :config-desc nil, :target-name "", :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcode-result-map {"IonXpress_002" "F-TKRYRSRT", "IonXpress_004" "F-YGAYRSAA", "IonXpress_005" "F-TKAYGSGW", "IonXpress_006" "F-TKG?GSAT", "IonXpress_014" "F-TTGYRSRW", "IonXpress_015" "F-TGGYGCAT", "IonXpress_018" "F-YTAYGCGT"}, :barcoded? true, :start-time #inst "2013-11-18T09:53:47.000-00:00", :end-time #inst "2013-11-18T10:39:18.000-00:00", :raw-map {"size" "423349", "store" {"Library Type" "", "Target Loci" "", "Target Regions" "", "Variant Detection" "", "barcoded" "true", "barcodes" {"IonXpress_002" {"SampleID" "F-TKRYRSRT", "hotspot_coverage" {"Average base coverage depth" "2450.6", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1946", "Male sample ID region reads" "0", "Number of mapped reads" "6748981", "Number of reads in sample ID regions" "24628", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "641008255"}, "target_coverage" {"Average base coverage depth" "2349.8", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_004" {"SampleID" "F-YGAYRSAA", "hotspot_coverage" {"Average base coverage depth" "1762.8", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1112", "Male sample ID region reads" "0", "Number of mapped reads" "5166623", "Number of reads in sample ID regions" "17951", "Percent base reads in sample ID regions" "0.34%", "Percent reads in sample ID regions" "0.35%", "Total base reads in sample ID regions" "485332704"}, "target_coverage" {"Average base coverage depth" "1687.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_005" {"SampleID" "F-TKAYGSGW", "hotspot_coverage" {"Average base coverage depth" "5172.9", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2686", "Male sample ID region reads" "0", "Number of mapped reads" "12498342", "Number of reads in sample ID regions" "52649", "Percent base reads in sample ID regions" "0.42%", "Percent reads in sample ID regions" "0.42%", "Total base reads in sample ID regions" "1118196811"}, "target_coverage" {"Average base coverage depth" "4781.7", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_006" {"SampleID" "F-TKG?GSAT", "hotspot_coverage" {"Average base coverage depth" "3259.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1512", "Male sample ID region reads" "0", "Number of mapped reads" "8488131", "Number of reads in sample ID regions" "32457", "Percent base reads in sample ID regions" "0.38%", "Percent reads in sample ID regions" "0.38%", "Total base reads in sample ID regions" "775619927"}, "target_coverage" {"Average base coverage depth" "2961.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_014" {"SampleID" "F-TTGYRSRW", "hotspot_coverage" {"Average base coverage depth" "2576.1", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1002", "Male sample ID region reads" "0", "Number of mapped reads" "7185798", "Number of reads in sample ID regions" "25805", "Percent base reads in sample ID regions" "0.35%", "Percent reads in sample ID regions" "0.36%", "Total base reads in sample ID regions" "654660133"}, "target_coverage" {"Average base coverage depth" "2338.3", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_015" {"SampleID" "F-TGGYGCAT", "hotspot_coverage" {"Average base coverage depth" "2599.4", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "1593", "Male sample ID region reads" "0", "Number of mapped reads" "7308490", "Number of reads in sample ID regions" "26810", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "667495055"}, "target_coverage" {"Average base coverage depth" "2446.6", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}, "IonXpress_018" {"SampleID" "F-YTAYGCGT", "hotspot_coverage" {"Average base coverage depth" "3248.2", "Bases in target regions" "8", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}, "mapped_reads" {"Female sample ID region reads" "2115", "Male sample ID region reads" "0", "Number of mapped reads" "8822299", "Number of reads in sample ID regions" "32819", "Percent base reads in sample ID regions" "0.36%", "Percent reads in sample ID regions" "0.37%", "Total base reads in sample ID regions" "841346599"}, "target_coverage" {"Average base coverage depth" "3067.5", "Bases in target regions" "988", "Coverage at 100x" "100.0%", "Coverage at 1x" "100.0%", "Coverage at 20x" "100.0%", "Uniformity of coverage" "100.0%"}}}}, "config" {}, "endtime" "2013-11-18T10:39:18.000987+00:00", "inodes" "79", "starttime" "2013-11-18T09:53:47.000320+00:00", "owner" {"last_login" "2014-04-12T06:52:17.000339+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "sampleID--v4.0-r73765", "config" {}, "path" "/results/plugins/sampleID", "active" true, "autorunMutable" true, "script" "sampleID.py", "name" "sampleID", "isConfig" false, "date" "2013-10-22T06:09:39.000975+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["wholechip" "thumbnail" "composite"]}, "version" "4.0-r73765", "userinputfields" {}, "id" 40, "resource_uri" "/rundb/api/v1/plugin/40/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'sampleID' v4.0-r73765", "isInstance" false}, "duration" "0:45:31.666857", "jobid" nil}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 157 {:recurse? true})))

(expect [:type :torrent-server :id :uri :result-uri :result-name :state :path :report-link :name :version :versioned-name :library-type :config-desc :target-name :target-bed :experiment-name :trimmed-reads? :barcode-result-map :barcoded? :start-time :end-time :raw-map]
        (keys pr157))

(expect {"IonXpress_006" "F-TKG?GSAT", "IonXpress_005" "F-TKAYGSGW"}
        (barcode-map pr157 #{"IonXpress_005" "IonXpress_006" }))

(expect (more->
         (:uri (:latest-result e71))
         :result-uri

         true
         sample-id?

         {"IonXpress_018" "F-YTAYGCGT", "IonXpress_006" "F-TKG?GSAT", "IonXpress_005" "F-TKAYGSGW",
          "IonXpress_015" "F-TGGYGCAT", "IonXpress_004" "F-YGAYRSAA", "IonXpress_014" "F-TTGYRSRW",
          "IonXpress_002" "F-TKRYRSRT"}
         (barcode-map e71)

         ;; experiment barcodes
         #{"IonXpress_002" "IonXpress_014" "IonXpress_004" "IonXpress_015" "IonXpress_005" "IonXpress_006" "IonXpress_018"}
         (barcode-set e71)

         ;; PluginResult  barcodes
         #{"IonXpress_002" "IonXpress_004" "IonXpress_005" "IonXpress_006" "IonXpress_014" "IonXpress_015" "IonXpress_018"}
         barcode-set

         {"IonXpress_002" "F-TKRYRSRT",
          "IonXpress_004" "F-YGAYRSAA",
          "IonXpress_005" "F-TKAYGSGW",
          "IonXpress_006" "F-TKG?GSAT",
          "IonXpress_014" "F-TTGYRSRW",
          "IonXpress_015" "F-TGGYGCAT",
          "IonXpress_018" "F-YTAYGCGT"}
         :barcode-result-map

         {"IonXpress_018" "F-YTAYGCGT", "IonXpress_005" "F-TKAYGSGW", "IonXpress_002" "F-TKRYRSRT"}
         (barcode-map #{"IonXpress_002" "IonXpress_005" "IonXpress_018"}))
        pr157)


;;; coverage plugin-result
(def e58 (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (experiment ts 58 {:recurse? true})))

(def pr94 (with-fake-routes-in-isolation
            {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                     {:status 200 :headers {"Content-Type" "application/json"}
                                      :body (slurp (uri-to-file uri :json))})}
            (plugin-result ts 94)))

(expect ion_torrent_api.schema.Experiment e58)
(expect ion_torrent_api.schema.PluginResult pr94)

(expect  (more-of x
                  #{["coverageAnalysis" :coverage] ["variantCaller" :tsvc] ["IonReporterUploader" nil]}
                  (set (map (juxt :name :type) (:plugin-result-set (:latest-result x))))

                  {"IonXpress_001" "07ME01 SEQ001-63", "IonXpress_002" "72TT01 SEQ001-63",
                   "IonXpress_003" "74TT01 SEQ001-63", "IonXpress_004" "76ME01 SEQ001-63",
                   "IonXpress_005" "86D SEQ001-63", "IonXpress_006" "88ME01 SEQ001-63",
                   "IonXpress_007" "96ME01 SEQ001-63"}
                  (:barcode-sample-map x)

                  {"IonXpress_001" {"hotspots" {}, "variants" {"het_indels" 89, "het_snps" 860, "homo_indels" 27, "homo_snps" 251, "no_call" 0, "other" 0, "variants" 1227}},
                   "IonXpress_012" {"hotspots" {}, "variants" {"het_indels" 0, "het_snps" 0, "homo_indels" 0, "homo_snps" 0, "no_call" 0, "other" 0, "variants" 0}},
                   "IonXpress_034" {"hotspots" {}, "variants" {"het_indels" 0, "het_snps" 0, "homo_indels" 0, "homo_snps" 0, "no_call" 0, "other" 0, "variants" 0}},
                   "IonXpress_002" {"hotspots" {}, "variants" {"het_indels" 87, "het_snps" 947, "homo_indels" 32, "homo_snps" 311, "no_call" 0, "other" 1, "variants" 1378}},
                   "IonXpress_003" {"hotspots" {}, "variants" {"het_indels" 95, "het_snps" 1053, "homo_indels" 28, "homo_snps" 301, "no_call" 0, "other" 1, "variants" 1478}},
                   "IonXpress_025" {"hotspots" {}, "variants" {"het_indels" 0, "het_snps" 0, "homo_indels" 0, "homo_snps" 0, "no_call" 0, "other" 0, "variants" 0}},
                   "IonXpress_004" {"hotspots" {}, "variants" {"het_indels" 15, "het_snps" 117, "homo_indels" 5, "homo_snps" 47, "no_call" 0, "other" 0, "variants" 184}},
                   "IonXpress_005" {"hotspots" {}, "variants" {"het_indels" 99, "het_snps" 792, "homo_indels" 35, "homo_snps" 293, "no_call" 0, "other" 0, "variants" 1219}},
                   "IonXpress_006" {"hotspots" {}, "variants" {"het_indels" 90, "het_snps" 751, "homo_indels" 48, "homo_snps" 312, "no_call" 0, "other" 1, "variants" 1202}},
                   "IonXpress_007" {"hotspots" {}, "variants" {"het_indels" 65, "het_snps" 833, "homo_indels" 11, "homo_snps" 168, "no_call" 0, "other" 0, "variants" 1077}},
                   "IonXpress_008" {"hotspots" {}, "variants" {"het_indels" 0, "het_snps" 0, "homo_indels" 0, "homo_snps" 0, "no_call" 0, "other" 0, "variants" 0}}}
                  (:barcode-result-map (first (filter variant-caller? (:plugin-result-set (:latest-result x)))))

                  #{"IonXpress_001" "IonXpress_002" "IonXpress_003" "IonXpress_004" "IonXpress_005"
                    "IonXpress_006" "IonXpress_007"}
                  (barcode-set x)

                  {"IonXpress_001" "07ME01 SEQ001-63", "IonXpress_002" "72TT01 SEQ001-63",
                   "IonXpress_003" "74TT01 SEQ001-63", "IonXpress_004" "76ME01 SEQ001-63",
                   "IonXpress_005" "86D SEQ001-63", "IonXpress_006" "88ME01 SEQ001-63",
                   "IonXpress_007" "96ME01 SEQ001-63"}
                  (barcode-map x)

                  (:id pr94)
                  (:id (first (filter variant-caller? (:plugin-result-set (:latest-result x))))))
         e58)

(expect (more-of x
                 #{"IonXpress_001" "IonXpress_002" "IonXpress_003" "IonXpress_004" "IonXpress_005"
                   "IonXpress_006" "IonXpress_007"
                   "IonXpress_008" "IonXpress_012" "IonXpress_025" "IonXpress_034" }
                 (barcode-set x)
                 #{"IonXpress_001" "IonXpress_002" "IonXpress_003" "IonXpress_004" "IonXpress_005"
                   "IonXpress_006" "IonXpress_007"}
                 (barcode-set x e58)

                 #{"IonXpress_001" "IonXpress_002" "IonXpress_003" "IonXpress_004" "IonXpress_005"
                   "IonXpress_006" "IonXpress_007"}
                 (into #{} (keys (barcode-map x e58))))
        pr94)

;;; coverageAvnalysis plugin-result
(def pr66 (with-fake-routes-in-isolation
            {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                     {:status 200 :headers {"Content-Type" "application/json"}
                                      :body (slurp (uri-to-file uri :json))})}
            (plugin-result ts 66)))

(expect [:type :torrent-server :id :uri :result-uri :result-name :state :path :report-link :name :version :versioned-name :library-type :config-desc :target-name :target-bed :experiment-name :trimmed-reads? :barcode-result-map :barcoded? :start-time :end-time :raw-map]
        (keys pr66))

(expect (more->
         true
         coverage?
         {"IonXpressRNA_001" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.97%", "Target bases with no strand bias" "79.94%", "Number of mapped reads" "13541550", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "84.72%", "Target base coverage at 20x" "96.51%", "Total aligned base reads" "1185203398", "Average reads per amplicon" "794.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_001_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.94%", "Uniformity of base coverage" "83.69%", "Percent reads on target" "93.77%", "Total assigned amplicon reads" "12697352", "Percent assigned amplicon reads" "93.77%", "Amplicons with at least 100 reads" "90.96%", "Average base coverage depth" "671.9", "Target base coverage at 500x" "47.27%", "Amplicons with at least 500 reads" "53.41%", "Reference (File)" "hg19", "Percent base reads on target" "95.73%", "Total base reads on target" "1134571007", "Target base coverage at 1x" "99.34%", "Target base coverage at 100x" "88.24%", "Amplicons with at least 1 read" "99.51%", "Amplicons reading end-to-end" "8.67%"},
          "IonXpressRNA_002" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.33%", "Target bases with no strand bias" "79.28%", "Number of mapped reads" "13579387", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "86.64%", "Target base coverage at 20x" "95.16%", "Total aligned base reads" "1228217548", "Average reads per amplicon" "815.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_002_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "95.56%", "Uniformity of base coverage" "85.89%", "Percent reads on target" "95.97%", "Total assigned amplicon reads" "13032709", "Percent assigned amplicon reads" "95.97%", "Amplicons with at least 100 reads" "90.11%", "Average base coverage depth" "707.7", "Target base coverage at 500x" "52.31%", "Amplicons with at least 500 reads" "59.97%", "Reference (File)" "hg19", "Percent base reads on target" "97.30%", "Total base reads on target" "1195050872", "Target base coverage at 1x" "99.00%", "Target base coverage at 100x" "89.04%", "Amplicons with at least 1 read" "99.19%", "Amplicons reading end-to-end" "8.34%"},
          "IonXpressRNA_003" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.60%", "Target bases with no strand bias" "80.47%", "Number of mapped reads" "12145531", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "88.99%", "Target base coverage at 20x" "95.99%", "Total aligned base reads" "1085455295", "Average reads per amplicon" "733.3", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_003_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.33%", "Uniformity of base coverage" "88.03%", "Percent reads on target" "96.56%", "Total assigned amplicon reads" "11727495", "Percent assigned amplicon reads" "96.56%", "Amplicons with at least 100 reads" "91.72%", "Average base coverage depth" "624.3", "Target base coverage at 500x" "47.29%", "Amplicons with at least 500 reads" "56.77%", "Reference (File)" "hg19", "Percent base reads on target" "97.12%", "Total base reads on target" "1054159542", "Target base coverage at 1x" "99.33%", "Target base coverage at 100x" "90.07%", "Amplicons with at least 1 read" "99.50%", "Amplicons reading end-to-end" "8.90%"},
          "IonXpressRNA_004" {"Number of amplicons" "15992", "Amplicons with no strand bias" "92.52%", "Target bases with no strand bias" "78.41%", "Number of mapped reads" "11984953", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "83.01%", "Target base coverage at 20x" "95.09%", "Total aligned base reads" "1041277993", "Average reads per amplicon" "719.0", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_004_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "95.80%", "Uniformity of base coverage" "82.07%", "Percent reads on target" "95.94%", "Total assigned amplicon reads" "11498272", "Percent assigned amplicon reads" "95.94%", "Amplicons with at least 100 reads" "87.60%", "Average base coverage depth" "596.4", "Target base coverage at 500x" "42.30%", "Amplicons with at least 500 reads" "51.26%", "Reference (File)" "hg19", "Percent base reads on target" "96.73%", "Total base reads on target" "1007191043", "Target base coverage at 1x" "99.14%", "Target base coverage at 100x" "84.54%", "Amplicons with at least 1 read" "99.37%", "Amplicons reading end-to-end" "7.47%"},
          "IonXpressRNA_005" {"Number of amplicons" "15992", "Amplicons with no strand bias" "94.23%", "Target bases with no strand bias" "81.41%", "Number of mapped reads" "13717156", "Using" "All Mapped Reads", "Uniformity of amplicon coverage" "89.76%", "Target base coverage at 20x" "96.53%", "Total aligned base reads" "1215435073", "Average reads per amplicon" "825.4", "Bases in target regions" "1688650", "Targeted Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "Alignments" "IonXpressRNA_005_R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_Auto_user_XXX-24-AmpliSeq_CCP_24_50", "Amplicons with at least 20 reads" "96.85%", "Uniformity of base coverage" "88.68%", "Percent reads on target" "96.23%", "Total assigned amplicon reads" "13200287", "Percent assigned amplicon reads" "96.23%", "Amplicons with at least 100 reads" "92.85%", "Average base coverage depth" "697.9", "Target base coverage at 500x" "53.15%", "Amplicons with at least 500 reads" "63.03%", "Reference (File)" "hg19", "Percent base reads on target" "96.96%", "Total base reads on target" "1178467512", "Target base coverage at 1x" "99.34%", "Target base coverage at 100x" "91.41%", "Amplicons with at least 1 read" "99.49%", "Amplicons reading end-to-end" "8.25%"}}
         :barcode-result-map)
        pr66)

;;; Results for one experiment

(expect (more-> #inst "2013-07-23T00:32:14.000226000+00:00" :latest-result-date
                ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"]
                :result-uri-set)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 50)))

(expect (more-> 77 :id
                "/rundb/api/v1/experiment/50/" :experiment-uri
                "Completed" :status
                ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"]
                :plugin-result-uri-set
                {"IonReporterUploader" "Completed", "variantCaller" "Completed"}
                :plugin-state-map
                #inst "2013-07-23T05:18:31.000209000-00:00" :timestamp)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts "/rundb/api/v1/results/77/")))

(expect (more-> 62 :id
                "/rundb/api/v1/experiment/50/" :experiment-uri
                "Completed" :status
                ["/rundb/api/v1/pluginresult/60/"]  :plugin-result-uri-set
                {"Alignment" "Completed"} :plugin-state-map
                #inst "2013-06-04T06:12:35.000941000+00:00" :timestamp)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts "/rundb/api/v1/results/62/")))

(expect (more->  61 :id
                 "/rundb/api/v1/experiment/50/" :experiment-uri
                 "Completed" :status
                 ["/rundb/api/v1/pluginresult/71/", "/rundb/api/v1/pluginresult/66/", "/rundb/api/v1/pluginresult/61/"]
                 :plugin-result-uri-set
                 {"IonReporterUploader" "Completed", "coverageAnalysis" "Completed", "variantCaller" "Completed"}
                 :plugin-state-map
                 #inst "2013-06-04T12:26:33.000330000+00:00" :timestamp)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts "/rundb/api/v1/results/61/")))

;;; the result with the newest timestamp
(expect [[#inst "2013-07-23T00:32:14.000226000+00:00" #inst "2013-06-03T13:31:54.000000000-00:00"]
         #inst "2013-07-23T05:18:31.000209000+00:00"
         #inst "2013-06-04T06:12:35.000941000+00:00"
         #inst "2013-06-04T12:26:33.000330000+00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          [((juxt :latest-result-date :date) (experiment ts 50))
           (:timestamp (result ts "/rundb/api/v1/results/77/"))
           (:timestamp (result ts "/rundb/api/v1/results/62/"))
           (:timestamp (result ts "/rundb/api/v1/results/61/"))]))

(expect
 [[;; one result has timestamp after experiment latest-result-date
   ;; the same result is only one that has plugin start-times after
   ;; eg: result, and pluginresults must have ids that are numerically later,
   ;; and dates that are later than experiment latest-result-date
   #inst "2013-07-23T00:32:14.000226000-00:00" #inst "2013-06-03T13:31:54.000000000-00:00"]
  ;; plugin start dates after result timestamp
  #inst "2013-07-23T05:18:31.000209000-00:00"
  [#inst "2014-02-17T05:50:42.000089000-00:00" #inst "2014-02-17T09:37:51.000879000-00:00"]
  [#inst "2013-07-30T13:50:08.000084000-00:00" #inst "2013-07-30T13:50:55.000586000-00:00"]
  ;; plugin start dates after result timestamp
  #inst "2013-06-04T06:12:35.000941000-00:00"
  [#inst "2013-06-05T00:43:52.000099000-00:00" #inst "2013-06-05T01:00:02.000682000-00:00"]
  ;; plugin start dates after result timestamp
  #inst "2013-06-04T12:26:33.000330000-00:00"
  [#inst "2013-07-04T14:14:01.000083000-00:00" #inst "2013-07-04T14:16:23.000038000-00:00"]
  [#inst "2013-06-07T05:01:28.000148000-00:00" #inst "2013-06-07T10:39:24.000736000-00:00"]
  [#inst "2013-06-05T01:00:04.000082000-00:00" #inst "2013-06-05T09:59:00.000684000-00:00"]]
 (with-fake-routes-in-isolation
   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                            {:status 200 :headers {"Content-Type" "application/json"}
                             :body (slurp (uri-to-file uri :json))})}
   [((juxt :latest-result-date :date) (experiment ts 50))
    (:timestamp (result ts "/rundb/api/v1/results/77/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/209/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/89/"))
    (:timestamp (result ts "/rundb/api/v1/results/62/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/60/"))
    (:timestamp (result ts "/rundb/api/v1/results/61/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/71/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/66/"))
    ((juxt :start-time :end-time) (plugin-result ts "/rundb/api/v1/pluginresult/61/"))]))

;;; expect that the dates are in the order:
;;; newest plugin result for newest result
;;; experiment latest-result-date

;;; then, the later plugin results and results can be in various orders
;;; depending on when the plugins were run, but all results are older than their
;;; pluginresults
;;; and the experiment is older than the results
(expect '(true true true true true true true true true true)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (map (partial apply >)
               (partition
                2 1 (map #(.getTime ^java.util.Date %)
                         [(:start-time  (plugin-result ts "/rundb/api/v1/pluginresult/209/"))
                          (:start-time  (plugin-result ts "/rundb/api/v1/pluginresult/89/"))
                          (:timestamp (result ts "/rundb/api/v1/results/77/"))
                          ;; newest result is older than its
                          ;; plugin-results, but newer than the
                          ;; experiment latest-result-date
                          (:latest-result-date  (experiment ts 50))
                          ;; and all other plugin-results and results
                          ;; are older than this
                          ;; but pluginresults are newer than their
                          ;; results
                          ;; and I'm not sure how result 61 is newer
                          ;; than 62
                          (:start-time  (plugin-result ts "/rundb/api/v1/pluginresult/71/")) ; res 61
                          (:start-time  (plugin-result ts "/rundb/api/v1/pluginresult/66/")) ; res 61
                          (:start-time  (plugin-result ts "/rundb/api/v1/pluginresult/61/")) ; res 61
                          (:start-time   (plugin-result ts "/rundb/api/v1/pluginresult/60/")) ;res 62
                          (:timestamp  (result ts "/rundb/api/v1/results/61/"))
                          (:timestamp  (result ts "/rundb/api/v1/results/62/"))
                          ;; experiment was first
                          (:date  (experiment ts 50))])))))

;;; testing RNA seq stuff
;;; '("sequencekitname" "pgmName" "notes" "pinnedRepResult" "storageHost" "flowsInOrder" "diskusage" "log" "runtype" "flows" "chipType" "baselineRun" "expName" "samples" "seqKitBarcode" "plan" "sample" "resultDate" "sequencekitbarcode" "cycles" "displayName" "runMode" "reagentBarcode" "date" "metaData" "expDir" "reverse_primer" "unique" "star" "status" "isReverseRun" "chipBarcode" "ftpStatus" "user_ack" "results" "storage_options" "expCompInfo" "eas_set" "id" "resource_uri" "usePreBeadfind" "autoAnalyze" "rawdatastyle")

(expect ["RNA"                          ; RNA-seq run type
         "P1.1.17"                      ; p1 chip
         ["/rundb/api/v1/results/155/" "/rundb/api/v1/results/156/"]
         "/rawdata/XXXNPROTON/R_2014_03_26_23_36_32_user_XXX-65-RNASeq_1-73"
         "run" "Complete"
         #inst "2014-03-27T11:24:17.000-00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          ((juxt :run-type
                 :chip-type
                 :result-uri-set
                 :dir
                 :status :ftp-status :latest-result-date)
           (experiment ts 97))))

(expect {"INQ0082TT01 F2  2257-112"
         {"externalId" "", "name" "INQ0082TT01_F2__2257-112", "displayedName" "INQ0082TT01 F2  2257-112",
          "date" "2014-03-27T01:19:13.000203+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 344, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/344/", "description" ""}
         "INQ0159TT01 PS  2257-112"
         {"externalId" "", "name" "INQ0159TT01_PS__2257-112", "displayedName" "INQ0159TT01 PS  2257-112",
          "date" "2014-03-27T01:19:13.000206+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 345, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/345/", "description" ""}
         "INQ0077ME01 SW  2257-112"
         {"externalId" "", "name" "INQ0077ME01_SW__2257-112", "displayedName" "INQ0077ME01 SW  2257-112",
          "date" "2014-03-27T01:19:13.000209+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 346, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/346/", "description" ""}
         "INQ0067TT01 35  2257-112"
         {"externalId" "", "name" "INQ0067TT01_35__2257-112", "displayedName" "INQ0067TT01 35  2257-112",
          "date" "2014-03-27T01:19:13.000211+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 347, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/347/", "description" ""}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (:sample-map  (experiment ts 97))))

(expect (more->
         {"IonXpressRNA_002" "INQ0067TT01 35  2257-112",
          "IonXpressRNA_005" "INQ0077ME01 SW  2257-112",
          "IonXpressRNA_004" "INQ0082TT01 F2  2257-112",
          "IonXpressRNA_013" "INQ0159TT01 PS  2257-112"} :barcode-sample-map
          #{"IonXpressRNA_013" "IonXpressRNA_002" "IonXpressRNA_004" "IonXpressRNA_005"}
          barcode-set
          "run" :status
          "Complete" :ftp-status
          ["run" "Complete"] ((juxt :status :ftp-status))
          true complete?)
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 97)))

;;; Experimnt 97   #inst "2014-03-27T11:24:17.000-00:00"
;;; Result   155   #inst "2014-03-27T11:24:17.000-00:00"
;;; Result   156   #inst "2014-03-27T06:21:05.000-00:00"
;;; Result 155 timestamp is *after* result 156

(expect-let [e97 (with-fake-routes-in-isolation
                   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                            {:status 200 :headers {"Content-Type" "application/json"}
                                             :body (slurp (uri-to-file uri :json))})}
                   (experiment ts 97))
             r155 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result ts 155))]
            (:latest-result-date e97)
            (:timestamp r155))

(expect-let [r155 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result ts 155))
             r156 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result ts 156))]
            true
            (> (.getTime ^java.util.Date (:timestamp r155))
               (.getTime ^java.util.Date (:timestamp r156))))

;;;

(expect (more-of coll
                 ion_torrent_api.schema.Experiment
                 (:e coll)
                 [ion_torrent_api.schema.Result ion_torrent_api.schema.Result]
                 (map type (:r coll))
                 #inst "2014-03-27T11:24:17.000-00:00"
                 (:latest-result-date (:e coll))
                 1395919457000
                 (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                 [[1395919457000 155] [1395901265000 156]]
                 (sort-by first > (map (juxt #(.getTime ^java.util.Date (:timestamp %)) :id) (:r coll)))
                 ["Completed" "Completed"]
                 (map :status (:r coll))
                 [false true]
                 (map :thumbnail? (:r coll))
                 1395919457000
                 (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                 [1395919457000 1395901265000]
                 (map  #(.getTime ^java.util.Date (:timestamp %)) (:r coll))
                 [true false]
                 (map #(<= (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                           (.getTime ^java.util.Date (:timestamp %)))
                      (:r coll))
                 155
                 (:id (filter-latest-result (:e coll) (:r coll))))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          {:e (experiment ts 97) :r [(result ts 155) (result ts 156)]}))

(expect (more-of coll
                 ion_torrent_api.schema.Experiment
                 (:e coll)
                 [ion_torrent_api.schema.Result ion_torrent_api.schema.Result]
                 (map type (:r coll))
                 #inst "2013-08-12T03:33:30.000-00:00"
                 (:latest-result-date (:e coll))
                 1376278410000
                 (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                 [[1376310430000 80] [1376288311000 81]]
                 (sort-by first > (map (juxt #(.getTime ^java.util.Date (:timestamp %)) :id) (:r coll)))
                 ["Completed" "Completed"]
                 (map :status (:r coll))
                 [false true]
                 (map :thumbnail? (:r coll))
                 1376278410000
                 (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                 [1376310430000 1376288311000]
                 (map  #(.getTime ^java.util.Date (:timestamp %)) (:r coll))
                 [true true]
                 (map #(<= (.getTime ^java.util.Date (:latest-result-date (:e coll)))
                           (.getTime ^java.util.Date (:timestamp %)))
                      (:r coll))
                 80
                 (:id (filter-latest-result (:e coll) (:r coll))))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          {:e (experiment ts 58) :r [(result ts 80) (result ts 81)]}))

(expect (more-of x
                 (str "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/" "Bar-code-name" "_rawlib.bam")
                 (bam-uri x "Bar-code-name")
                 (str "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/" "Bar-code-name" "_rawlib.bam.bai")
                 (bai-uri x "Bar-code-name")
                 (str "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/" "Bar-code-name" "_rawlib.bam.header.sam")
                 (bam-header-uri x "Bar-code-name"))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result ts 155)))

(expect (more-of x
                 97 (:id x))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 97)))

(expect (more-of x
                 155 (:id x)
                 true (complete? x)
                 false (:thumbnail? x)
                 "/report/latex/155.pdf" (pdf-uri x)
                 "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/IonXpressRNA_001_rawlib.bam"
                 (bam-uri x :IonXpressRNA_001)
                 "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/IonXpressRNA_001_rawlib.bam.bai"
                 (bai-uri x :IonXpressRNA_001)
                 "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/IonXpressRNA_001_rawlib.bam.header.sam"
                 (bam-header-uri x :IonXpressRNA_001))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result (experiment ts 97))))

;;; plugin-result: coverage
;;; plugin-result: variant-caller

(expect (more-of x
                 nil (:type x)
                 nil (tsvc-target-bed-uri x)
                 #{} (barcode-set x))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result ts 251)))

;;; recursively get experiment, latest result, and pluginresults
(expect
 #ion_torrent_api.schema.Experiment{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                    :id 50,
                                    :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                    :pgm-name "XXXNPROTON",
                                    :display-name "user XXX-24-AmpliSeq CCP 24",
                                    :uri "/rundb/api/v1/experiment/50/",
                                    :run-type "AMPS",
                                    :chip-type "900",
                                    :result-uri-set ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"],
                                    :dir "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                    :status "run",
                                    :ftp-status "Complete",
                                    :sample-map {"inq-037-me" {"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil}, "inq-052-tt" {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil}, "inq-024-me" {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil}, "inq-022-me" {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil}, "inq-025-tt" {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}},
                                    :barcode-sample-map {"IonXpressRNA_003" "inq-022-me", "IonXpressRNA_004" "inq-024-me", "IonXpressRNA_005" "inq-025-tt", "IonXpressRNA_002" "inq-037-me", "IonXpressRNA_001" "inq-052-tt"},
                                    :date #inst "2013-06-03T13:31:54.000-00:00",
                                    :latest-result-date #inst "2013-07-23T00:32:14.000-00:00",
                                    :latest-result
                                    #ion_torrent_api.schema.Result{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                                                   :id 77,
                                                                   :name "24_reanalyze",
                                                                   :uri "/rundb/api/v1/results/77/",
                                                                   :experiment-uri "/rundb/api/v1/experiment/50/",
                                                                   :status "Completed",
                                                                   :plugin-result-uri-set ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"],
                                                                   :plugin-state-map {"IonReporterUploader" "Completed", "variantCaller" "Completed"},
                                                                   :analysis-version "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,",
                                                                   :report-status "Nothing",
                                                                   :plugin-store-map {"IonReporterUploader" {}, "variantCaller" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}},
                                                                   :bam-link "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam", :fastq-link "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq", :report-link "/output/Home/24_reanalyze_077/", :filesystem-path "/results/analysis/output/Home/24_reanalyze_077", :reference "hg19",
                                                                   :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/68/"],
                                                                   :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/68/"],
                                                                   :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/74/"],
                                                                   :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/74/"],
                                                                   :timestamp #inst "2013-07-23T05:18:31.000-00:00",
                                                                   :thumbnail? false,
                                                                   :plugin-result-set
                                                                   #{#ion_torrent_api.schema.PluginResult{:type nil, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}, :id 89, :uri "/rundb/api/v1/pluginresult/89/", :result-uri "/rundb/api/v1/results/77/", :result-name "24_reanalyze", :state "Completed", :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/IonReporterUploader_out", :report-link "/output/Home/24_reanalyze_077/", :name "IonReporterUploader", :version "3.6.2-r62833", :versioned-name "IonReporterUploader--v3.6.2-r62833", :library-type nil, :config-desc nil, :barcode-result-map nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcoded? false, :start-time #inst "2013-07-30T13:50:08.000-00:00", :end-time #inst "2013-07-30T13:50:55.000-00:00",
                                                                                                          :raw-map {"size" "173732", "store" {}, "config" {}, "endtime" "2013-07-30T13:50:55.000586+00:00", "inodes" "119", "starttime" "2013-07-30T13:50:08.000084+00:00", "owner" {"last_login" "2014-04-01T05:48:53.000369+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "IonReporterUploader--v3.6.2-r62833", "config" {"port" "443", "protocol" "https", "server" "dataloader.ionreporter.iontorrent.com", "token" "njmr5ajT9xg85qdT3g4WLZH04FJEj89j7G2CeMmesrABrJirGBWYc2qgzEcsMK5gieZU9OgSgSIA5JGWsxQJaw==", "version" "IR16"}, "path" "", "active" false, "autorunMutable" true, "script" "IonReporterUploader.py", "name" "IonReporterUploader", "isConfig" false, "date" "2013-07-03T03:30:34+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" false, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" ["export"], "runlevel" ["pre" "block" "post"], "runtype" ["thumbnail" "wholechip" "composite"]}, "version" "3.6.2-r62833", "userinputfields" {}, "id" 35, "resource_uri" "/rundb/api/v1/plugin/35/", "selected" true, "autorun" false, "description" "Ion Torrent Plugin - 'IonReporterUploader' v3.6.2-r62833\n\n[ Please update python class documentation to provide a short description and documentation for your plugin. ]", "isInstance" false}, "duration" "0:00:47.501694", "jobid" nil}}
                                                                     #ion_torrent_api.schema.PluginResult{:type :tsvc, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}, :id 209, :uri "/rundb/api/v1/pluginresult/209/", :result-uri "/rundb/api/v1/results/77/", :result-name "24_reanalyze", :state "Completed", :path "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out", :report-link "/output/Home/24_reanalyze_077/", :name "variantCaller", :version "4.0-r76860", :versioned-name "variantCaller--v4.0-r76860", :library-type "AmpliSeq", :config-desc "Somatic - Proton - Low Stringency", :barcode-result-map {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, :target-name "4477685_Comprehensive_CCP_bedfile_20120517", :target-bed "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", :experiment-name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", :trimmed-reads? true, :barcoded? true, :start-time #inst "2014-02-17T05:50:42.000-00:00", :end-time #inst "2014-02-17T09:37:51.000-00:00",
                                                                                                          :raw-map {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}}}
                                                                   :lib-metrics-set #{{"sampled_q47_alignments" 0, "extrapolated_mapped_bases_in_q10_alignments" "0", "sampled_200q20_reads" 0, "i600Q20_reads" 0, "q20_alignments" 53385851, "sampled_300q10_reads" 0, "extrapolated_q47_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q20_alignments" "0", "sampled_100q20_reads" 0, "s50Q20" 0, "i500Q20_reads" 0, "sampled_200q10_reads" 0, "i600Q10_reads" 0, "extrapolated_from_number_of_sampled_reads" 0, "sampled_q47_coverage_percentage" 0, "q47_mapped_bases" "3781427950", "sampled_q17_longest_alignment" 0, "q20_coverage_percentage" 0, "sampled_100q10_reads" 0, "i500Q10_reads" 0, "r50Q20" 0, "q10_coverage_percentage" 0, "s50Q10" 0, "extrapolated_50q20_reads" 0, "i400Q20_reads" 0, "extrapolated_50q10_reads" 0, "cf" 0.27535108383745, "q7_mean_alignment_length" 91, "r50Q10" 0, "q47_longest_alignment" 211, "sNumAlignments" 0, "sampled_400q47_reads" 0, "i300Q20_reads" 0, "i400Q10_reads" 0, "align_sample" 0, "sampled_300q47_reads" 0, "i200Q20_reads" 46, "i300Q10_reads" 0, "sampled_q17_coverage_percentage" 0, "sampled_50q7_reads" 0, "sampled_q10_alignments" 0, "sampled_q7_alignments" 0, "extrapolated_q17_mean_alignment_length" 0, "rNumAlignments" 0, "i200Q10_reads" 179, "q10_mapped_bases" "5752686797", "extrapolated_100q7_reads" 0, "total_mapped_target_bases" "5830701139", "sampled_q20_alignments" 0, "sampled_200q47_reads" 0, "i600Q47_reads" 0, "i100Q20_reads" 17975937, "extrapolated_q7_longest_alignment" 0, "extrapolated_q20_mean_coverage_depth" 0, "q17_longest_alignment" 224, "sampled_100q47_reads" 0, "i500Q47_reads" 0, "q20_mapped_bases" "4235251115", "i100Q10_reads" 25792090, "i550Q20_reads" 0, "sampled_400q17_reads" 0, "extrapolated_200q7_reads" 0, "q47_coverage_percentage" 0, "sampled_q20_mean_alignment_length" 0, "i400Q47_reads" 0, "i150Q7_reads" 39620, "rMeanAlignLen" 0, "i450Q20_reads" 0, "extrapolated_50q47_reads" 0, "sampled_300q17_reads" 0, "extrapolated_300q7_reads" 0, "i550Q10_reads" 0, "extrapolated_q10_mean_coverage_depth" 0, "i350Q20_reads" 0, "sampled_200q17_reads" 0, "i600Q17_reads" 0, "i450Q10_reads" 0, "extrapolated_400q7_reads" 0, "sampled_q7_mean_alignment_length" 0, "extrapolated_mapped_bases_in_q7_alignments" "0", "sampled_q10_mean_alignment_length" 0, "extrapolated_q7_coverage_percentage" 0, "extrapolated_q17_alignments" 0, "i50Q7_reads" 57344941, "i300Q47_reads" 0, "i250Q7_reads" 0, "sMeanAlignLen" 0, "s50Q17" 0, "duplicate_reads" nil, "extrapolated_400q20_reads" 0, "i200Q47_reads" 9, "i350Q7_reads" 0, "q7_alignments" 63110718, "i250Q20_reads" 0, "sampled_100q17_reads" 0, "i500Q17_reads" 0, "q17_coverage_percentage" 0, "i50Q20_reads" 40379815, "i350Q10_reads" 0, "extrapolated_300q20_reads" 0, "i100Q47_reads" 10672707, "r50Q17" 0, "extrapolated_400q10_reads" 0, "i450Q7_reads" 0, "i150Q20_reads" 6631, "i400Q17_reads" 0, "i250Q10_reads" 0, "i50Q10_reads" 56924835, "extrapolated_50q17_reads" 0, "sysSNR" 13.7530774782199, "i300Q17_reads" 0, "i150Q10_reads" 29664, "extrapolated_q47_mean_coverage_depth" 0, "extrapolated_q47_alignments" 0, "Index_Version" "tmap-f3", "extrapolated_200q20_reads" 0, "total_number_of_sampled_reads" 0, "extrapolated_300q10_reads" 0, "i550Q47_reads" 0, "i550Q7_reads" 0, "q20_mean_alignment_length" 79, "extrapolated_100q20_reads" 0, "total_mapped_reads" "65565617", "extrapolated_200q10_reads" 0, "i450Q47_reads" 0, "sampled_q47_mean_alignment_length" 0, "i100Q7_reads" 25953991, "i200Q17_reads" 90, "q17_qscore_bases" "0", "extrapolated_100q10_reads" 0, "i350Q47_reads" 0, "rLongestAlign" 0, "q10_mean_alignment_length" 91, "i100Q17_reads" 21597651, "i200Q7_reads" 187, "sampled_50q20_reads" 0, "dr" 0.167434278409928, "i300Q7_reads" 0, "i550Q17_reads" 0, "extrapolated_q17_mean_coverage_depth" 0, "extrapolated_q20_longest_alignment" 0, "sampled_50q10_reads" 0, "extrapolated_400q47_reads" 0, "i250Q47_reads" 0, "sLongestAlign" 0, "sampled_mapped_bases_in_q17_alignments" "0", "i50Q47_reads" 37409631, "q7_mapped_bases" "5794278906", "extrapolated_q10_alignments" 0, "extrapolated_300q47_reads" 0, "extrapolated_50q7_reads" 0, "sampled_q17_mean_alignment_length" 0, "i150Q47_reads" 2525, "r200Q20" 0, "q47_qscore_bases" "0", "extrapolated_q10_longest_alignment" 0, "i400Q7_reads" 0, "i450Q17_reads" 0, "Genome_Version" "hg19", "s200Q20" 0, "r100Q20" 0, "i500Q7_reads" 0, "i350Q17_reads" 0, "r200Q10" 0, "sampled_100q7_reads" 0, "extrapolated_q20_alignments" 0, "raw_accuracy" 98.7, "extrapolated_200q47_reads" 0, "i600Q7_reads" 0, "i250Q17_reads" 0, "s100Q20" 0, "sampled_200q7_reads" 0, "extrapolated_q20_coverage_percentage" 0, "i50Q17_reads" 50537490, "s200Q10" 0, "r100Q10" 0, "extrapolated_100q47_reads" 0, "sampled_q20_mean_coverage_depth" 0, "q47_mean_alignment_length" 72, "sampled_mapped_bases_in_q47_alignments" "0", "extrapolated_400q17_reads" 0, "s100Q10" 0, "extrapolated_300q17_reads" 0, "extrapolated_q10_coverage_percentage" 0, "sampled_50q47_reads" 0, "i150Q17_reads" 12004, "sampled_q7_mean_coverage_depth" 0, "sampled_q10_mean_coverage_depth" 0, "sampled_300q7_reads" 0, "q10_qscore_bases" "0", "extrapolated_200q17_reads" 0, "extrapolated_q7_mean_alignment_length" 0, "extrapolated_q47_longest_alignment" 0, "sampled_mapped_bases_in_q7_alignments" "0", "sampled_400q7_reads" 0, "aveKeyCounts" 89, "report" "/rundb/api/v1/results/77/", "q17_mean_alignment_length" 87, "q20_qscore_bases" "0", "extrapolated_100q17_reads" 0, "sampled_50q17_reads" 0, "sampled_mapped_bases_in_q10_alignments" "0", "q7_longest_alignment" 224, "sampled_mapped_bases_in_q20_alignments" "0", "extrapolated_q47_coverage_percentage" 0, "sampled_q47_mean_coverage_depth" 0, "extrapolated_q17_longest_alignment" 0, "genome" "hg19", "r200Q17" 0, "genomesize" "3095693981", "q7_coverage_percentage" 0, "s200Q17" 0, "r100Q17" 0, "q17_alignments" 57135955, "extrapolated_q7_alignments" 0, "s100Q17" 0, "extrapolated_q17_coverage_percentage" 0, "sampled_q20_longest_alignment" 0, "sampled_q17_mean_coverage_depth" 0, "extrapolated_mapped_bases_in_q17_alignments" "0", "rCoverage" 0, "sampled_q7_longest_alignment" 0, "sampled_q10_longest_alignment" 0, "id" 68, "totalNumReads" 67420355, "q47_alignments" 52254752, "resource_uri" "/rundb/api/v1/libmetrics/68/", "sCoverage" 0, "ie" 0.617759954184294, "extrapolated_q20_mean_alignment_length" 0, "sampled_q20_coverage_percentage" 0, "extrapolated_mapped_bases_in_q47_alignments" "0", "q7_qscore_bases" "0", "sampled_q17_alignments" 0, "extrapolated_q7_mean_coverage_depth" 0, "genomelength" 0, "extrapolated_q10_mean_alignment_length" 0, "sampled_q7_coverage_percentage" 0, "sampled_q10_coverage_percentage" 0, "sampled_400q20_reads" 0, "sampled_q47_longest_alignment" 0, "q20_longest_alignment" 224, "q17_mapped_bases" "5004408711", "q10_alignments" 62602410, "sampled_400q10_reads" 0, "q10_longest_alignment" 224, "sampled_300q20_reads" 0}},
                                                                   :tf-metrics-set #{{"aveKeyCount" 92, "HPAccuracy" "0 : 331532643/333171476, 1 : 237654520/240430336, 2 : 10582544/10810316, 3 : 0/0, 4 : 2478849/2694841, 5 : 0/0, 6 : 0/0, 7 : 0/0", "sequence" "TACGAGCGTGTAGACGTGTCGTACGTGCGACGTAGTGAGTATACATGCTCTGACACTATGTACGATCTGAGACTGCCAAGGCACACAGGGGATAGG", "Q17ReadCount" 2422568, "corrHPSNR" "", "Q10ReadCount" 2623331, "number" 2755545, "SysSNR" 9.80844083888461, "name" "TF_C", "keypass" 2755545, "report" "/rundb/api/v1/results/77/", "Q10Mean" 91, "Q17Histo" "8359 38630 27283 25575 7839 3555 11554 9211 10021 14301 10666 27098 10045 13259 5789 7613 3527 7326 5427 6931 3776 2241 2257 2837 2658 1421 7936 10362 880 4733 1934 3657 3597 3144 884 431 4430 4016 2006 1674 686 2476 2764 2151 1669 577 826 1100 1144 701 6365 6499 5230 7266 3399 12241 10272 6734 6330 2250 2632 2916 2791 1261 2211 2420 1303 1448 1064 3411 1589 8234 1543 1308 2131 2348 5087 1125 2528 904 5728 1214 601 774 1092 1782 3355 2952 3359 13335 16913 70845 13506 943 1371 36139 1947587 186232 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0", "id" 68, "resource_uri" "/rundb/api/v1/tfmetrics/68/", "Q17Mean" 84, "Q10Histo" "4258 8094 6541 2829 1904 567 861 714 3153 2903 4833 6334 4421 4839 2068 2419 2117 2593 2753 1278 2723 3995 4440 2808 1325 1364 4212 5678 1709 1323 1271 2879 3721 2476 1296 871 1142 2277 1894 1573 1795 1876 1834 2106 1986 1618 1561 1681 1765 1536 2215 2777 2083 1540 2377 2144 2248 1580 952 970 953 866 658 541 986 551 520 389 414 319 385 565 559 934 595 854 2773 1037 1434 983 5210 1581 1030 834 1362 1345 3702 2440 4928 15418 19648 74742 18929 10620 10554 45045 1965728 202921 69360 39695 28229 20974 16135 12327 8942 5048 1382 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"}},
                                                                   :analysis-metrics-set #{{"keypass_all_beads" 0, "washout_dud" 0, "tfFinal" 2831301, "lib" 133931487, "bead" 137867956, "sysIE" 0.617759954184294, "excluded" 0, "tfMix" 0, "libKp" 0, "washout_library" 0, "live" 137119356, "empty" 6727891, "washout_test_fragment" 0, "washout" 0, "lib_pass_basecaller" 0, "lib_pass_cafie" 0, "sysCF" 0.27535108383745, "libLive" 0, "libFinal" 67443042, "ignored" 3782896, "tf" 3187869, "dud" 748600, "report" "/rundb/api/v1/results/77/", "amb" 0, "libMix" 0, "tfKp" 0, "pinned" 16320393, "sysDR" 0.167434278409928, "id" 74, "resource_uri" "/rundb/api/v1/analysismetrics/74/", "washout_ambiguous" 0, "washout_live" 0, "tfLive" 0}},
                                                                   :quality-metrics-set #{{"q0_bases" "6015091255", "q20_max_read_length" 297, "q20_mean_read_length" 59, "q0_50bp_reads" 58676073, "q20_150bp_reads" 26132, "q17_max_read_length" 299, "q20_100bp_reads" 13225065, "q20_reads" 67420355, "q17_50bp_reads" 52999900, "q17_150bp_reads" 122924, "q20_bases" "4849408686", "q0_max_read_length" 299, "report" "/rundb/api/v1/results/77/", "q17_100bp_reads" 23394392, "q17_reads" 67420355, "q0_mean_read_length" 89, "q20_50bp_reads" 38652987, "q0_150bp_reads" 208782, "q17_bases" "5267814486", "id" 74, "resource_uri" "/rundb/api/v1/qualitymetrics/74/", "q0_100bp_reads" 27101827, "q17_mean_read_length" 80, "q0_reads" 67420355}},
                                                                   :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "3XNXT", "metaData" {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}
                                    :raw-map {"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400, "baselineRun" false, "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/", "sample" "inq-037-me", "resultDate" "2013-07-23T00:32:14.000226+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-06-03T13:31:54+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "", "libraryKey" "TCAG", "thumbnailbasecallerargs" "", "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment", "userInput" "", "version" "3.6.56201"}}, "thumbnailanalysisargs" "", "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]}, "inq-024-me" {"barcodes" ["IonXpressRNA_004"]}, "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]}, "inq-037-me" {"barcodes" ["IonXpressRNA_002"]}, "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}}, "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-06-04T03:26:53.000155+00:00", "status" "run", "thumbnailalignmentargs" "", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47, "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "", "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}
 (with-fake-routes-in-isolation
   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                            {:status 200 :headers {"Content-Type" "application/json"}
                             :body (slurp (uri-to-file uri :json))})}
   (experiment ts 50 {:recurse? true})))

(expect [77 false #inst "2013-07-23T05:18:31.000-00:00"]
        ((juxt :id :thumbnail? :timestamp)
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (result ts "/rundb/api/v1/results/77/"))))

(expect [61 false #inst "2013-06-04T12:26:33.000-00:00"]
        ((juxt :id :thumbnail? :timestamp)
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (result ts "/rundb/api/v1/results/61/"))))

(expect [62 true #inst "2013-06-04T06:12:35.000-00:00"]
        ((juxt :id :thumbnail? :timestamp)
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (result ts "/rundb/api/v1/results/62/"))))

(expect (more-of x
                 50 (:id x)
                 ion_torrent_api.schema.Result (:latest-result x)
                 77 (:id (:latest-result x))
                 #inst "2013-07-23T00:32:14.000-00:00" (:latest-result-date x)
                 #{[89 nil "IonReporterUploader"] [209 :tsvc "variantCaller"]}
                 (set (map (juxt :id :type :name) (:plugin-result-set (:latest-result x)))))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 50 {:recurse? true})))

(expect
 (more-of x
          ion_torrent_api.schema.Result x
          77 (:id x)
          #{[89 nil "IonReporterUploader"] [209 :tsvc "variantCaller"]}
          (set (map (juxt :id :type :name) (:plugin-result-set x)))
          ion_torrent_api.schema.PluginResult
          (first (:plugin-result-set x))
          ion_torrent_api.schema.PluginResult
          (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x)))

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpress_001/TSVC_variants.vcf.gz"
          (tsvc-vcf-uri (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))) :IonXpress_001)
          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpress_001/TSVC_variants.vcf.gz.tbi"
          (tsvc-vcf-tbi-uri (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))) :IonXpress_001)
          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/4477685_Comprehensive_CCP_bedfile_20120517.bed"
          (tsvc-target-bed-uri (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))))

          true
          (:trimmed-reads? (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))) :IonXpressRNA_001)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/IonXpressRNA_001_rawlib_PTRIM.bam"
          (bam-uri (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))) :IonXpressRNA_001)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/IonXpressRNA_001_rawlib.bam"
          (bam-uri (assoc (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x)))
                     :trimmed-reads? false) :IonXpressRNA_001)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/IonXpressRNA_001_rawlib_PTRIM.bam.bai"
          (bai-uri (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x))) :IonXpressRNA_001)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/IonXpressRNA_001_rawlib.bam.bai"
          (bai-uri (assoc (first (filter (comp (partial = :tsvc) :type) (:plugin-result-set x)))
                     :trimmed-reads? false) :IonXpressRNA_001)

          "/output/Home/24_reanalyze_077/IonXpressRNA_001_rawlib.bam.header.sam"
          (bam-header-uri x :IonXpressRNA_001))
 (:latest-result (with-fake-routes-in-isolation
                   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                            {:status 200 :headers {"Content-Type" "application/json"}
                                             :body (slurp (uri-to-file uri :json))})}
                   (experiment ts 50 {:recurse? true}))))

(expect (more-of exp
                 50(:id exp)
                 2 (count (-> exp :latest-result :plugin-result-set))
                 [:tsvc nil]
                 (map :type (-> exp :latest-result :plugin-result-set))
                 1 (count (variant-caller exp))
                 0 (count (sample-id exp))
                 0 (count (coverage exp))
                 )
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 50 {:recurse? true})))

(expect (more-of exp
                 71 (:id exp)
                 3 (count (-> exp :latest-result :plugin-result-set))
                 [:coverage :sample-id :tsvc]
                 (map :type (-> exp :latest-result :plugin-result-set))
                 1 (count (variant-caller exp))
                 1 (count (sample-id exp))
                 1 (count (coverage exp))
                 )
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 71 {:recurse? true})))

(expect
 (more-of x
          97 (:id x)
          ion_torrent_api.schema.Result (:latest-result x)
          155 (:id (:latest-result x))
          [251] (map :id (:plugin-result-set (:latest-result x)))
          ion_torrent_api.schema.PluginResult (type (first (:plugin-result-set (:latest-result x))))
          "/results/analysis/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/plugin_out/FastqCreator_jb_out"
          (:path (first (:plugin-result-set (:latest-result x))))
          "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/"
          (:report-link (first (:plugin-result-set (:latest-result x))))
          "/output/Home/Auto_user_XXX-65-RNASeq_1-73_97_155/plugin_out/FastqCreator_jb_out"
          (#'ion/plugin-result-api-path-prefix (first (:plugin-result-set (:latest-result x))))
          )
 (with-fake-routes-in-isolation
   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                            {:status 200 :headers {"Content-Type" "application/json"}
                             :body (slurp (uri-to-file uri :json))})}
   (experiment ts 97 {:recurse? true})))

;;; experiment with multiple metrics uris
(expect
 #ion_torrent_api.schema.Experiment{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                    :id 95,
                                    :name "R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71",
                                    :pgm-name "XXXNPROTON",
                                    :display-name "user XXX-63-RNASeq 1-71",
                                    :uri "/rundb/api/v1/experiment/95/",
                                    :run-type "RNA",
                                    :chip-type "P1.1.17",
                                    :result-uri-set ["/rundb/api/v1/results/152/" "/rundb/api/v1/results/151/"],
                                    :dir "/rawdata/XXXNPROTON/R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71",
                                    :status "run",
                                    :ftp-status "Complete",
                                    :sample-map {"INQ0131TT01 7C  2257-111" {"externalId" "", "name" "INQ0131TT01_7C__2257-111", "displayedName" "INQ0131TT01 7C  2257-111", "date" "2014-03-21T04:33:21.000840+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 336, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/336/", "description" ""}, "INQ0073TT01 ON  2257-111" {"externalId" "", "name" "INQ0073TT01_ON__2257-111", "displayedName" "INQ0073TT01 ON  2257-111", "date" "2014-03-21T04:33:21.000844+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 337, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/337/", "description" ""}, "INQ0097TT01 DX  2257-111" {"externalId" "", "name" "INQ0097TT01_DX__2257-111", "displayedName" "INQ0097TT01 DX  2257-111", "date" "2014-03-21T04:33:21.000847+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 338, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/338/", "description" ""}, "INQ0087TT01 XV  2257-111" {"externalId" "", "name" "INQ0087TT01_XV__2257-111", "displayedName" "INQ0087TT01 XV  2257-111", "date" "2014-03-21T04:33:21.000849+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 339, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/339/", "description" ""}},
                                    :barcode-sample-map {"IonXpressRNA_015" "INQ0073TT01 ON  2257-111", "IonXpressRNA_014" "INQ0087TT01 XV  2257-111", "IonXpressRNA_002" "INQ0097TT01 DX  2257-111", "IonXpressRNA_016" "INQ0131TT01 7C  2257-111"},
                                    :date #inst "2014-03-20T15:28:15.000-00:00",
                                    :latest-result-date #inst "2014-03-21T12:57:41.000-00:00",
                                    :latest-result
                                    #ion_torrent_api.schema.Result{:torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"},
                                                                   :id 151,
                                                                   :name "Auto_user_XXX-63-RNASeq_1-71_95",
                                                                   :uri "/rundb/api/v1/results/151/",
                                                                   :experiment-uri "/rundb/api/v1/experiment/95/",
                                                                   :status "Completed",
                                                                   :plugin-result-uri-set ["/rundb/api/v1/pluginresult/247/"],
                                                                   :plugin-state-map {"FastqCreator_jb" "Error"},
                                                                   :analysis-version "db:4.0.39-1,an:4.0.6-1,",
                                                                   :report-status "Nothing",
                                                                   :plugin-store-map {"FastqCreator_jb" {}},
                                                                   :bam-link "/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71_Auto_user_XXX-63-RNASeq_1-71_95.bam", :fastq-link "/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/basecaller_results/R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71_Auto_user_XXX-63-RNASeq_1-71_95.fastq", :report-link "/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/", :filesystem-path "/results/analysis/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151", :reference "hg19",
                                                                   :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/142/"],
                                                                   :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/140/"],
                                                                   :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/148/"],
                                                                   :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/148/"],
                                                                   :timestamp #inst "2014-03-21T12:57:41.000-00:00",
                                                                   :thumbnail? false,
                                                                   :plugin-result-set #{#ion_torrent_api.schema.PluginResult{:type nil, :torrent-server #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}, :id 247, :uri "/rundb/api/v1/pluginresult/247/", :result-uri "/rundb/api/v1/results/151/", :result-name "Auto_user_XXX-63-RNASeq_1-71_95", :state "Error", :path "/results/analysis/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/plugin_out/FastqCreator_jb_out", :report-link "/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/", :name "FastqCreator_jb", :version "3.6.2-r57238", :versioned-name "FastqCreator_jb--v3.6.2-r57238", :library-type nil, :config-desc nil, :barcode-result-map nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcoded? false, :start-time #inst "2014-03-21T12:57:43.000-00:00", :end-time #inst "2014-03-21T12:57:45.000-00:00", :raw-map {"size" "19891", "store" {}, "config" {}, "endtime" "2014-03-21T12:57:45.000160+00:00", "inodes" "4", "starttime" "2014-03-21T12:57:43.000317+00:00", "owner" {"last_login" "2014-04-10T12:02:17.000680+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "FastqCreator_jb--v3.6.2-r57238", "config" {}, "path" "/results/plugins/implementations/sha1new=78fd455cb36501827f84158d4fa80ec1e6615a9c", "active" true, "autorunMutable" true, "script" "FastqCreator_jb.py", "name" "FastqCreator_jb", "isConfig" false, "date" "2013-11-22T06:17:11.000667+00:00", "url" "http://torrentcircuit.iontorrent.com/warehouse/download/feedfile/FastqCreator_jb.xml", "status" {"installStatus" "installed", "result" "0install"}, "hasAbout" true, "majorBlock" false, "isPlanConfig" false, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "thumbnail" "wholechip"]}, "version" "3.6.2-r57238", "userinputfields" {}, "id" 48, "resource_uri" "/rundb/api/v1/plugin/48/", "selected" true, "autorun" false, "description" "Convert BAM file to Fastq", "isInstance" true}, "duration" "0:00:01.842236", "jobid" nil}}},
                                                                   :lib-metrics-set
                                                                   #{{"sampled_q47_alignments" 0,
                                                                      "extrapolated_mapped_bases_in_q10_alignments" "0",
                                                                      "sampled_200q20_reads" 0,
                                                                      "i600Q20_reads" 0,
                                                                      "q20_alignments" 20574075,
                                                                      "sampled_300q10_reads" 0,
                                                                      "extrapolated_q47_mean_alignment_length" 0,
                                                                      "extrapolated_mapped_bases_in_q20_alignments" "0",
                                                                      "sampled_100q20_reads" 0,
                                                                      "s50Q20" 0,
                                                                      "i500Q20_reads" 0,
                                                                      "sampled_200q10_reads" 0,
                                                                      "i600Q10_reads" 0,
                                                                      "extrapolated_from_number_of_sampled_reads" 0,
                                                                      "sampled_q47_coverage_percentage" 0,
                                                                      "q47_mapped_bases" "1181392459",
                                                                      "sampled_q17_longest_alignment" 0,
                                                                      "q20_coverage_percentage" 0,
                                                                      "sampled_100q10_reads" 0,
                                                                      "i500Q10_reads" 0,
                                                                      "r50Q20" 0,
                                                                      "q10_coverage_percentage" 0,
                                                                      "s50Q10" 0,
                                                                      "extrapolated_50q20_reads" 0,
                                                                      "i400Q20_reads" 0,
                                                                      "extrapolated_50q10_reads" 0,
                                                                      "cf" 0.00707558210706338,
                                                                      "q7_mean_alignment_length" 81,
                                                                      "r50Q10" 0,
                                                                      "q47_longest_alignment" 256,
                                                                      "sNumAlignments" 0,
                                                                      "sampled_400q47_reads" 0,
                                                                      "i300Q20_reads" 0,
                                                                      "i400Q10_reads" 0,
                                                                      "align_sample" 0,
                                                                      "sampled_300q47_reads" 0,
                                                                      "i200Q20_reads" 27573,
                                                                      "i300Q10_reads" 0,
                                                                      "sampled_q17_coverage_percentage" 0,
                                                                      "sampled_50q7_reads" 0,
                                                                      "sampled_q10_alignments" 0,
                                                                      "sampled_q7_alignments" 0,
                                                                      "extrapolated_q17_mean_alignment_length" 0,
                                                                      "rNumAlignments" 0,
                                                                      "i200Q10_reads" 104121,
                                                                      "q10_mapped_bases" "2387451212",
                                                                      "extrapolated_100q7_reads" 0,
                                                                      "total_mapped_target_bases" "2612122580",
                                                                      "sampled_q20_alignments" 0,
                                                                      "sampled_200q47_reads" 0,
                                                                      "i600Q47_reads" 0,
                                                                      "i100Q20_reads" 3818095,
                                                                      "extrapolated_q7_longest_alignment" 0,
                                                                      "extrapolated_q20_mean_coverage_depth" 0,
                                                                      "q17_longest_alignment" 274,
                                                                      "sampled_100q47_reads" 0,
                                                                      "i500Q47_reads" 0,
                                                                      "q20_mapped_bases" "1325375594",
                                                                      "i100Q10_reads" 7962126,
                                                                      "i550Q20_reads" 0,
                                                                      "sampled_400q17_reads" 0,
                                                                      "extrapolated_200q7_reads" 0,
                                                                      "q47_coverage_percentage" 0,
                                                                      "sampled_q20_mean_alignment_length" 0,
                                                                      "i400Q47_reads" 0,
                                                                      "i150Q7_reads" 2108533,
                                                                      "rMeanAlignLen" 0,
                                                                      "i450Q20_reads" 0,
                                                                      "extrapolated_50q47_reads" 0,
                                                                      "sampled_300q17_reads" 0,
                                                                      "extrapolated_300q7_reads" 0,
                                                                      "i550Q10_reads" 0,
                                                                      "extrapolated_q10_mean_coverage_depth" 0,
                                                                      "i350Q20_reads" 0,
                                                                      "sampled_200q17_reads" 0,
                                                                      "i600Q17_reads" 0,
                                                                      "i450Q10_reads" 0,
                                                                      "extrapolated_400q7_reads" 0,
                                                                      "sampled_q7_mean_alignment_length" 0,
                                                                      "extrapolated_mapped_bases_in_q7_alignments" "0",
                                                                      "sampled_q10_mean_alignment_length" 0,
                                                                      "extrapolated_q7_coverage_percentage" 0,
                                                                      "extrapolated_q17_alignments" 0,
                                                                      "i50Q7_reads" 24477032,
                                                                      "i300Q47_reads" 0,
                                                                      "i250Q7_reads" 626,
                                                                      "sMeanAlignLen" 0,
                                                                      "s50Q17" 0,
                                                                      "duplicate_reads" nil,
                                                                      "extrapolated_400q20_reads" 0,
                                                                      "i200Q47_reads" 5344,
                                                                      "i350Q7_reads" 0,
                                                                      "q7_alignments" 31416854,
                                                                      "i250Q20_reads" 50,
                                                                      "sampled_100q17_reads" 0,
                                                                      "i500Q17_reads" 0,
                                                                      "q17_coverage_percentage" 0,
                                                                      "i50Q20_reads" 11711944,
                                                                      "i350Q10_reads" 0,
                                                                      "extrapolated_300q20_reads" 0,
                                                                      "i100Q47_reads" 2048198,
                                                                      "r50Q17" 0,
                                                                      "extrapolated_400q10_reads" 0,
                                                                      "i450Q7_reads" 0,
                                                                      "i150Q20_reads" 580226,
                                                                      "i400Q17_reads" 0,
                                                                      "i250Q10_reads" 500,
                                                                      "i50Q10_reads" 23008814,
                                                                      "extrapolated_50q17_reads" 0,
                                                                      "sysSNR" 14.6657219369107,
                                                                      "i300Q17_reads" 0,
                                                                      "i150Q10_reads" 1896566,
                                                                      "extrapolated_q47_mean_coverage_depth" 0,
                                                                      "extrapolated_q47_alignments" 0,
                                                                      "Index_Version" "tmap-f3",
                                                                      "extrapolated_200q20_reads" 0,
                                                                      "total_number_of_sampled_reads" 0,
                                                                      "extrapolated_300q10_reads" 0,
                                                                      "i550Q47_reads" 0,
                                                                      "i550Q7_reads" 0,
                                                                      "q20_mean_alignment_length" 64,
                                                                      "extrapolated_100q20_reads" 0,
                                                                      "total_mapped_reads" "35656651",
                                                                      "extrapolated_200q10_reads" 0,
                                                                      "i450Q47_reads" 0,
                                                                      "sampled_q47_mean_alignment_length" 0,
                                                                      "i100Q7_reads" 8719653,
                                                                      "i200Q17_reads" 48439,
                                                                      "q17_qscore_bases" "0",
                                                                      "extrapolated_100q10_reads" 0,
                                                                      "i350Q47_reads" 0,
                                                                      "rLongestAlign" 0,
                                                                      "q10_mean_alignment_length" 80,
                                                                      "i100Q17_reads" 5049050,
                                                                      "i200Q7_reads" 119268,
                                                                      "sampled_50q20_reads" 0,
                                                                      "dr" 0.684216804802418,
                                                                      "i300Q7_reads" 0,
                                                                      "i550Q17_reads" 0,
                                                                      "extrapolated_q17_mean_coverage_depth" 0,
                                                                      "extrapolated_q20_longest_alignment" 0,
                                                                      "sampled_50q10_reads" 0,
                                                                      "extrapolated_400q47_reads" 0,
                                                                      "i250Q47_reads" 7,
                                                                      "sLongestAlign" 0,
                                                                      "sampled_mapped_bases_in_q17_alignments" "0",
                                                                      "i50Q47_reads" 10870642,
                                                                      "q7_mapped_bases" "2551394418",
                                                                      "extrapolated_q10_alignments" 0,
                                                                      "extrapolated_300q47_reads" 0,
                                                                      "extrapolated_50q7_reads" 0,
                                                                      "sampled_q17_mean_alignment_length" 0,
                                                                      "i150Q47_reads" 253374,
                                                                      "r200Q20" 0,
                                                                      "q47_qscore_bases" "0",
                                                                      "extrapolated_q10_longest_alignment" 0,
                                                                      "i400Q7_reads" 0,
                                                                      "i450Q17_reads" 0,
                                                                      "Genome_Version" "hg19",
                                                                      "s200Q20" 0,
                                                                      "r100Q20" 0,
                                                                      "i500Q7_reads" 0,
                                                                      "i350Q17_reads" 0,
                                                                      "r200Q10" 0,
                                                                      "sampled_100q7_reads" 0,
                                                                      "extrapolated_q20_alignments" 0,
                                                                      "raw_accuracy" 97,
                                                                      "extrapolated_200q47_reads" 0,
                                                                      "i600Q7_reads" 0,
                                                                      "i250Q17_reads" 179,
                                                                      "s100Q20" 0,
                                                                      "sampled_200q7_reads" 0,
                                                                      "extrapolated_q20_coverage_percentage" 0,
                                                                      "i50Q17_reads" 16876552,
                                                                      "s200Q10" 0,
                                                                      "r100Q10" 0,
                                                                      "extrapolated_100q47_reads" 0,
                                                                      "sampled_q20_mean_coverage_depth" 0,
                                                                      "q47_mean_alignment_length" 58,
                                                                      "sampled_mapped_bases_in_q47_alignments" "0",
                                                                      "extrapolated_400q17_reads" 0,
                                                                      "s100Q10" 0,
                                                                      "extrapolated_300q17_reads" 0,
                                                                      "extrapolated_q10_coverage_percentage" 0,
                                                                      "sampled_50q47_reads" 0,
                                                                      "i150Q17_reads" 1065452,
                                                                      "sampled_q7_mean_coverage_depth" 0,
                                                                      "sampled_q10_mean_coverage_depth" 0,
                                                                      "sampled_300q7_reads" 0,
                                                                      "q10_qscore_bases" "0",
                                                                      "extrapolated_200q17_reads" 0,
                                                                      "extrapolated_q7_mean_alignment_length" 0,
                                                                      "extrapolated_q47_longest_alignment" 0,
                                                                      "sampled_mapped_bases_in_q7_alignments" "0",
                                                                      "sampled_400q7_reads" 0,
                                                                      "aveKeyCounts" 108,
                                                                      "report" "/rundb/api/v1/results/151/",
                                                                      "q17_mean_alignment_length" 73,
                                                                      "q20_qscore_bases" "0",
                                                                      "extrapolated_100q17_reads" 0,
                                                                      "sampled_50q17_reads" 0,
                                                                      "sampled_mapped_bases_in_q10_alignments" "0",
                                                                      "q7_longest_alignment" 278,
                                                                      "sampled_mapped_bases_in_q20_alignments" "0",
                                                                      "extrapolated_q47_coverage_percentage" 0,
                                                                      "sampled_q47_mean_coverage_depth" 0,
                                                                      "extrapolated_q17_longest_alignment" 0,
                                                                      "genome" "hg19",
                                                                      "r200Q17" 0,
                                                                      "genomesize" "3095693981",
                                                                      "q7_coverage_percentage" 0,
                                                                      "s200Q17" 0,
                                                                      "r100Q17" 0,
                                                                      "q17_alignments" 22905036,
                                                                      "extrapolated_q7_alignments" 0,
                                                                      "s100Q17" 0,
                                                                      "extrapolated_q17_coverage_percentage" 0,
                                                                      "sampled_q20_longest_alignment" 0,
                                                                      "sampled_q17_mean_coverage_depth" 0,
                                                                      "extrapolated_mapped_bases_in_q17_alignments" "0",
                                                                      "rCoverage" 0,
                                                                      "sampled_q7_longest_alignment" 0,
                                                                      "sampled_q10_longest_alignment" 0,
                                                                      "id" 142,
                                                                      "totalNumReads" 39118777,
                                                                      "q47_alignments" 20193055,
                                                                      "resource_uri" "/rundb/api/v1/libmetrics/142/",
                                                                      "sCoverage" 0,
                                                                      "ie" 0.656696408987045,
                                                                      "extrapolated_q20_mean_alignment_length" 0,
                                                                      "sampled_q20_coverage_percentage" 0,
                                                                      "extrapolated_mapped_bases_in_q47_alignments" "0",
                                                                      "q7_qscore_bases" "0",
                                                                      "sampled_q17_alignments" 0,
                                                                      "extrapolated_q7_mean_coverage_depth" 0,
                                                                      "genomelength" 0,
                                                                      "extrapolated_q10_mean_alignment_length" 0,
                                                                      "sampled_q7_coverage_percentage" 0,
                                                                      "sampled_q10_coverage_percentage" 0,
                                                                      "sampled_400q20_reads" 0,
                                                                      "sampled_q47_longest_alignment" 0,
                                                                      "q20_longest_alignment" 268,
                                                                      "q17_mapped_bases" "1678602161",
                                                                      "q10_alignments" 29774934,
                                                                      "sampled_400q10_reads" 0,
                                                                      "q10_longest_alignment" 276,
                                                                      "sampled_300q20_reads" 0}},
                                                                   :tf-metrics-set
                                                                   #{{"aveKeyCount" 73,
                                                                      "HPAccuracy" "0 : 10157675/10204853, 1 : 7195271/7362591, 2 : 323615/333050, 3 : 0/0, 4 : 77535/83222, 5 : 0/0, 6 : 0/0, 7 : 0/0",
                                                                      "sequence" "TACGAGCGTGTAGACGTGTCGTACGTGCGACGTAGTGAGTATACATGCTCTGACACTATGTACGATCTGAGACTGCCAAGGCACACAGGGGATAGG",
                                                                      "Q17ReadCount" 48619,
                                                                      "corrHPSNR" "",
                                                                      "Q10ReadCount" 79443,
                                                                      "number" 83966,
                                                                      "SysSNR" 7.61787130374887,
                                                                      "name" "TF_C",
                                                                      "keypass" 83966,
                                                                      "report" "/rundb/api/v1/results/151/",
                                                                      "Q10Mean" 91,
                                                                      "Q17Histo" "4951 16842 1319 349 105 2149 1957 5247 21 47 55 192 85 211 116 113 56 123 30 453 50 97 56 20 32 13 70 18 22 56 32 74 16 28 5 4 35 18 52 56 12 40 16 12 4 17 10 7 42 12 743 374 189 217 163 287 291 238 160 106 100 53 55 112 73 59 44 98 23 183 70 86 63 74 77 87 166 6 108 5 47 7 6 23 7 35 32 55 2 236 610 645 251 26 165 7419 24771 9972 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
                                                                      "id" 140,
                                                                      "resource_uri" "/rundb/api/v1/tfmetrics/140/",
                                                                      "Q17Mean" 55,
                                                                      "Q10Histo" "1124 1294 274 26 20 81 2 18 6 5 51 43 56 80 41 69 55 39 26 40 61 46 32 13 18 27 22 26 24 12 29 82 30 41 31 36 32 23 25 66 62 64 52 46 48 37 23 37 69 59 86 61 61 71 87 65 44 32 38 34 37 37 32 31 43 17 27 14 13 16 13 24 29 38 15 16 16 15 22 13 10 14 9 13 18 15 35 40 95 372 969 967 686 755 1926 8989 29920 13165 7938 4687 2950 2024 1301 777 462 218 41 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"}},
                                                                   :analysis-metrics-set
                                                                   #{{"keypass_all_beads" 0,
                                                                      "washout_dud" 0,
                                                                      "tfFinal" 109334,
                                                                      "lib" 139056500,
                                                                      "bead" 139530063,
                                                                      "sysIE" 0.656696408987045,
                                                                      "excluded" 16095180,
                                                                      "tfMix" 0,
                                                                      "libKp" 0,
                                                                      "washout_library" 0,
                                                                      "live" 139457653,
                                                                      "empty" 4610489,
                                                                      "washout_test_fragment" 0,
                                                                      "washout" 0,
                                                                      "lib_pass_basecaller" 0,
                                                                      "lib_pass_cafie" 0,
                                                                      "sysCF" 0.00707558210706338,
                                                                      "libLive" 0,
                                                                      "libFinal" 39121464,
                                                                      "ignored" 4002625,
                                                                      "tf" 401153,
                                                                      "dud" 72410,
                                                                      "report" "/rundb/api/v1/results/151/",
                                                                      "amb" 0, "libMix" 0, "tfKp" 0,
                                                                      "pinned" 460779,
                                                                      "sysDR" 0.684216804802418,
                                                                      "id" 148,
                                                                      "resource_uri" "/rundb/api/v1/analysismetrics/148/",
                                                                      "washout_ambiguous" 0,
                                                                      "washout_live" 0,
                                                                      "tfLive" 0}},
                                                                   :quality-metrics-set
                                                                   #{{"q0_bases" "3073960030",
                                                                      "q20_max_read_length" 352,
                                                                      "q20_mean_read_length" 37,
                                                                      "q0_50bp_reads" 27622675,
                                                                      "q20_150bp_reads" 981706,
                                                                      "q17_max_read_length" 352,
                                                                      "q20_100bp_reads" 4441618,
                                                                      "q20_reads" 39118777,
                                                                      "q17_50bp_reads" 22169575,
                                                                      "q17_150bp_reads" 2696722,
                                                                      "q20_bases" "2283093085",
                                                                      "q0_max_read_length" 371,
                                                                      "report" "/rundb/api/v1/results/151/",
                                                                      "q17_100bp_reads" 9357080,
                                                                      "q17_reads" 39118777,
                                                                      "q0_mean_read_length" 78,
                                                                      "q20_50bp_reads" 13014529,
                                                                      "q0_150bp_reads" 3440687,
                                                                      "q17_bases" "2524644192",
                                                                      "id" 148,
                                                                      "resource_uri" "/rundb/api/v1/qualitymetrics/148/",
                                                                      "q0_100bp_reads" 11606923,
                                                                      "q17_mean_read_length" 63,
                                                                      "q0_reads" 39118777}},
                                                                   :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 90050, "log" "/output/Home/Auto_user_XXX-63-RNASeq_1-71_95_151/log.html", "runid" "BNVR1", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "V4M3F", "metaData" {}, "resultsType" "", "timeStamp" "2014-03-21T12:57:41.000124+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/101/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}
                                    :raw-map {"sequencekitname" "ProtonI200Kit-v3", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 281915, "flows" 500, "baselineRun" false, "samples" [{"externalId" "", "name" "INQ0131TT01_7C__2257-111", "displayedName" "INQ0131TT01 7C  2257-111", "date" "2014-03-21T04:33:21.000840+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 336, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/336/", "description" ""} {"externalId" "", "name" "INQ0073TT01_ON__2257-111", "displayedName" "INQ0073TT01 ON  2257-111", "date" "2014-03-21T04:33:21.000844+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 337, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/337/", "description" ""} {"externalId" "", "name" "INQ0097TT01_DX__2257-111", "displayedName" "INQ0097TT01 DX  2257-111", "date" "2014-03-21T04:33:21.000847+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 338, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/338/", "description" ""} {"externalId" "", "name" "INQ0087TT01_XV__2257-111", "displayedName" "INQ0087TT01 XV  2257-111", "date" "2014-03-21T04:33:21.000849+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/95/"], "id" 339, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/339/", "description" ""}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/98/", "sample" "INQ0131TT01_7C__2257-111", "resultDate" "2014-03-21T12:57:41.000124+00:00", "sequencekitbarcode" "", "cycles" 15, "runMode" "single", "reagentBarcode" "", "date" "2014-03-20T15:28:15+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2014_03_21_01_26_33_user_XXX-63-RNASeq_1-71", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "stage1 map4", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 100000 --calibration-training=100000 --flow-signals-type scaled-residual", "libraryKey" "TCAG", "thumbnailbasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 100000", "selectedPlugins" {"FastqCreator_jb" {"features" [], "id" 48, "name" "FastqCreator_jb", "userInput" "", "version" "3.6.2-r57238"}}, "thumbnailanalysisargs" "Analysis --from-beadfind --clonal-filter-bkgmodel on --region-size=100x100 --bkg-bfmask-update off --gpuWorkLoad 1 --bkg-debug-param 1 --beadfind-thumbnail 1 --gopt /opt/ion/config/gopt_p1.1.17_ampliseq_exome.param", "barcodedSamples" {"INQ0073TT01 ON  2257-111" {"barcodeSampleInfo" {"IonXpressRNA_015" {"description" "", "externalId" ""}}, "barcodes" ["IonXpressRNA_015"]}, "INQ0087TT01 XV  2257-111" {"barcodeSampleInfo" {"IonXpressRNA_014" {"description" "", "externalId" ""}}, "barcodes" ["IonXpressRNA_014"]}, "INQ0097TT01 DX  2257-111" {"barcodeSampleInfo" {"IonXpressRNA_002" {"description" "", "externalId" ""}}, "barcodes" ["IonXpressRNA_002"]}, "INQ0131TT01 7C  2257-111" {"barcodeSampleInfo" {"IonXpressRNA_016" {"description" "", "externalId" ""}}, "barcodes" ["IonXpressRNA_016"]}}, "libraryKitBarcode" nil, "libraryKitName" "Ion Total RNA Seq Kit v2", "thumbnailbeadfindargs" "justBeadFind --beadfind-minlivesnr 3 --region-size=100x100 --beadfind-thumbnail 1", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2014-03-21T04:33:21.000836+00:00", "status" "run", "thumbnailalignmentargs" "stage1 map4", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/152/" "/rundb/api/v1/results/151/"], "targetRegionBedFile" "", "basecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 1000", "analysisargs" "Analysis --from-beadfind --clonal-filter-bkgmodel on --region-size=216x224 --bkg-bfmask-update off --gpuWorkLoad 1 --total-timeout 600 --gopt /opt/ion/config/gopt_p1.1.17_ampliseq_exome.param", "hotSpotRegionBedFile" "", "id" 101, "resource_uri" "/rundb/api/v1/experimentanalysissettings/101/", "prebasecallerargs" "BaseCaller --keypass-filter on --phasing-residual-filter=2.0 --trim-qual-cutoff 15 --trim-qual-window-size 30 --trim-adapter-cutoff 16 --num-unfiltered 1000 --calibration-training=100000 --flow-signals-type scaled-residual --max-phasing-levels 2", "isDuplicateReads" false, "beadfindargs" "justBeadFind --beadfind-minlivesnr 3 --region-size=216x224 --total-timeout 600", "experiment" "/rundb/api/v1/experiment/95/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}
 (with-fake-routes-in-isolation
   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                            {:status 200 :headers {"Content-Type" "application/json"}
                             :body (slurp (uri-to-file uri :json))})}
   (experiment ts 95 {:recurse? true})))

(expect (more-of x
                 nil? (:thumbnail? x)
                 false (:thumbnail? (:latest-result x)))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (experiment ts 95 {:recurse? true})))

;; no barcodes assigned but there are results for barcodes
(expect (more-of x
                 "R_2014_04_30_02_26_25_user_XXX-70"
                 (:name x)
                 #{}
                 (barcode-set x)
                 #{"IonXpress_001" "IonXpress_004" "IonXpress_006" "IonXpress_007" "IonXpress_008"
                   "IonXpress_010" "IonXpress_012" "IonXpress_013" "IonXpress_014" "IonXpress_016"
                   "IonXpress_025" "IonXpress_034" "IonXpress_061" "IonXpress_095" "IonXpress_096"}
                 (barcode-set (:latest-result x)))
        (read-string (slurp "test/data/e70-no-bc.edn")))
