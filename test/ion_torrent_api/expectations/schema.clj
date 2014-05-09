(ns ion-torrent-api.expectations.schema
  (:require [expectations :refer :all]
            [clojure [string :as str] [edn :as edn]]
            [schema.core :as s]
            [ion-torrent-api.expectations.util :refer :all]
            [ion-torrent-api.schema :refer :all])
  (:import [ion_torrent_api.schema Experiment Result PluginResult TorrentServer]))

(def creds ["user" "pass"])
(def host "http://my-intranet-torrent-server.com")
(def ts (torrent-server host :creds creds))
;;; utilities

;;; Note: test private functions by accessing vars directly
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
            (edn/read-string {:readers data-readers} (str ts)))
(expect-let [ts (torrent-server "h" :api-path "p")]
            ts
            (read-string (str ts)))


;;; now testing data-readers / toString round-trip

(expect #ion_torrent_api.schema.Experiment{:id 9999}
        (edn/read-string {:readers data-readers} (str (map->Experiment { :id 9999}))))

;;; with all fields specified
(expect #ion_torrent_api.schema.Experiment{:id 9999, :name nil, :pgm-name nil, :display-name nil, :uri nil, :run-type nil, :chip-type nil, :sample-map nil, :result-uri-set nil, :dir nil, :status nil, :ftp-status nil, :date nil, :latest-result-date nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->Experiment { :id 9999}))))

(expect #ion_torrent_api.schema.Result{:id 99999}
        (edn/read-string {:readers data-readers} (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.schema.Result{:id 99999, :name nil, :uri nil, :experiment-uri nil, :status nil, :plugin-result-uri-set nil, :plugin-state-map nil, :analysis-version nil, :report-status nil, :plugin-store-map nil, :bam-link nil, :fastq-link nil, :report-link nil, :filesystem-path nil, :reference nil, :lib-metrics-uri-set nil, :tf-metrics-uri-set nil, :analysis-metrics-uri-set nil, :quality-metrics-uri-set nil, :timestamp nil, :thumbnail? nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.schema.PluginResult{:type nil :id 999, :uri nil, :result-uri nil, :result-name nil, :state nil, :path nil, :report-link nil, :name nil, :version nil, :versioned-name nil, :library-type nil, :config-desc nil, :barcode-result-map nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcoded? nil, :start-time nil, :end-time nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->PluginResult {:id 999}))))

(expect nil? (s/check TorrentServer
                      #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com", :version :v1 :api-path "/rundb/api/v1/"}))

(expect nil? (s/check PluginResult
                      #ion_torrent_api.schema.PluginResult{:type :tsvc,
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
                                                           :raw-map {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}}))

(expect nil? (s/check Result
                      #ion_torrent_api.schema.Result{:torrent-server
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
                                                     :raw-map
                                                     {"timeToComplete" "0", "tfFastq" "_", "diskusage"
                                                      154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage"
                                                      {"default"
                                                       true, "dirPath" "/results/analysis/output", "id"
                                                       1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed"
                                                      0, "sffLink"
                                                      nil, "parentIDs" "", "autoExempt"
                                                      false, "planShortID" "3XNXT", "metaData"
                                                      {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows"
                                                      0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects"
                                                      ["/rundb/api/v1/project/3/"], "tfSffLink"
                                                      nil, "processedCycles"
                                                      0, "representative"
                                                      false}}))

(expect nil? (s/check Experiment
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
                                                    :raw-map {"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400, "baselineRun" false, "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}], "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/", "sample" "inq-037-me", "resultDate" "2013-07-23T00:32:14.000226+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-06-03T13:31:54+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "", "libraryKey" "TCAG", "thumbnailbasecallerargs" "", "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment", "userInput" "", "version" "3.6.56201"}}, "thumbnailanalysisargs" "", "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]}, "inq-024-me" {"barcodes" ["IonXpressRNA_004"]}, "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]}, "inq-037-me" {"barcodes" ["IonXpressRNA_002"]}, "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}}, "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-06-04T03:26:53.000155+00:00", "status" "run", "thumbnailalignmentargs" "", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47, "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "", "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}))
