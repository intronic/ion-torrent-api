(ns ion-torrent-api.expectations.core
  (:require [expectations :refer :all]
            [ion-torrent-api.core :refer :all :as ion]
            [ion-torrent-api.expectations.util :refer :all]
            [clj-http.client :as client]
            [clj-http.fake :refer :all]
            [clojure [string :as str] [edn :as edn]]))

(def creds ["user" "pass"])
(def host "http://my-intranet-torrent-server.com")
(def ts (torrent-server host creds))
;;; utilities
(expect [{"id" 3} {"id" 2} {"id" 1}] (sort-by-id-desc [{"id" 2} {"id" 1} {"id" 3}]))

;;; Note: test private functions by accessing vars directly
(expect "abcdef" (#'ion/ensure-starts-with "abc" "def"))
(expect "abcxabcdef" (#'ion/ensure-starts-with "abc" "xabcdef"))
(expect "abcdef" (#'ion/ensure-starts-with "abc" "abcdef"))

(expect "/rundb/api/v1/results/99/" (#'ion/ensure-starts-with (str "/rundb/api/v1/" "results/") (str 99 "/")))

(expect #ion_torrent_api.core.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :api-path "/rundb/api/v1/", :creds ["user" "pass"]}
                ts)

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
              (#'ion/get-resource ts "experiment/name-XXX-24"))))

;;; test object
(expect {"expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (first (get (with-fake-routes-in-isolation
                          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                   {:status 200 :headers {"Content-Type" "application/json"}
                                                    :body (slurp (uri-to-file uri :json))})}
                          (#'ion/get-resource ts "experiment/name-XXX-24"))
                        "objects"))))

(expect {"expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (first (get (with-fake-routes-in-isolation
                          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                   {:status 200 :headers {"Content-Type" "application/json"}
                                                    :body (slurp (uri-to-file uri :json))})}
                          (#'ion/get-completed-resource ts "experiment/name-XXX-24"))
                        "objects"))))

;;; ;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TorrentServerAPI Protocol
;;; experiments

(expect {"limit" 20, "next" "/rundb/api/v1/experiment/?limit=20&offset=20", "offset" 0, "previous" nil, "total_count" 77}
        (get (with-fake-routes-in-isolation
                {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                         {:status 200 :headers {"Content-Type" "application/json"}
                                          :body (slurp (uri-to-file uri :json))})}
                (get-experiments ts))
             "meta"))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (get-experiments ts))
                    "objects")))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (get-experiments ts {"some" "opts"}))
                    "objects")))
(expect 20
        (count (get (with-fake-routes-in-isolation
                      {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                               {:status 200 :headers {"Content-Type" "application/json"}
                                                :body (slurp (uri-to-file uri :json))})}
                      (get-experiments ts 20 20))
                    "objects")))

;;; experiment-name

(expect '("sequencekitname" "pgmName" "notes" "pinnedRepResult" "storageHost" "flowsInOrder" "diskusage" "log" "runtype" "flows" "chipType" "baselineRun" "expName" "samples" "seqKitBarcode" "plan" "sample" "resultDate" "sequencekitbarcode" "cycles" "displayName" "runMode" "reagentBarcode" "date" "metaData" "expDir" "reverse_primer" "unique" "star" "status" "isReverseRun" "chipBarcode" "ftpStatus" "user_ack" "results" "storage_options" "expCompInfo" "eas_set" "id" "resource_uri" "usePreBeadfind" "autoAnalyze" "rawdatastyle")
        (keys 
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
           (get-experiment-name ts "name-XXX-24"))))

(expect {"pgmName" "XXXNPROTON" "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
              (get-experiment-name ts "name-XXX-24"))))

;;; experiment

(expect {"pgmName" "XXXNPROTON" "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (get-experiment ts 50))))

(expect {"pgmName" "XXXNPROTON" "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (get-experiment ts "/rundb/api/v1/experiment/50/"))))

(expect {"pgmName" "XXXNPROTON" "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp (uri-to-file uri :json))})}
              (get-experiment ts "/rundb/api/v1/experiment/50/" {}))))

;;; result

(expect "/rundb/api/v1/results/77"
        (let [id-or-uri 77] (#'ion/ensure-starts-with (str (:api-path ts) "results/")
                                                (str id-or-uri))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-result ts 77))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-result ts 77 {}))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-result ts 77 {}))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-result ts "/rundb/api/v1/results/77/"))))

;;; plugin-result

;;; barcode map of variantCaller results
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
          (get-plugin-result ts 209 {})))

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
          (get-plugin-result ts 66)))

(expect ["size" "store" "config" "path" "resultName" "endtime" "inodes" "reportLink" "starttime" "state"
         "owner" "plugin" "duration" "jobid" "id" "resource_uri" "result"]
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-plugin-result ts 209{}))))

(expect ["size" "store" "config" "path" "resultName" "endtime" "inodes" "reportLink" "starttime" "state"
         "owner" "plugin" "duration" "jobid" "id" "resource_uri" "result"]
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-plugin-result ts 209 {}))))

(expect ["size" "store" "config" "path" "resultName" "endtime" "inodes" "reportLink" "starttime" "state"
         "owner" "plugin" "duration" "jobid" "id" "resource_uri" "result"]
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (get-plugin-result ts "/rundb/api/v1/pluginresult/209/"))))

(expect true? (=
               (with-fake-routes-in-isolation
                 {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                          {:status 200 :headers {"Content-Type" "application/json"}
                                           :body (slurp (uri-to-file uri :json))})}
                 (get-plugin-result ts 209 {}))
               (with-fake-routes-in-isolation
                 {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                          {:status 200 :headers {"Content-Type" "application/json"}
                                           :body (slurp (uri-to-file uri :json))})}
                 (get-plugin-result ts "/rundb/api/v1/pluginresult/209/"))))

;;; Experiment record

(expect  #ion_torrent_api.core.Experiment{:id 50, :name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
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
                                          :sample-map [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}],
                                          :result-uri-set ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"],
                                          :dir "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
                                          :status "run", :ftp-status "Complete",
                                          :date #inst "2013-06-03T13:31:54.000-00:00",
                                          :latest-result-date #inst "2013-07-23T00:32:14.000-00:00",
                                          :raw-map {"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost", "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400, "baselineRun" false, "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/", "sample" "inq-037-me", "resultDate" "2013-07-23T00:32:14.000226+00:00", "sequencekitbarcode" "", "cycles" 12, "runMode" "single", "reagentBarcode" "", "date" "2013-06-03T13:31:54+00:00", "metaData" {}, "reverse_primer" "Ion Kit", "unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false, "isReverseRun" false, "chipBarcode" "", "user_ack" "U", "storage_options" "A", "expCompInfo" "", "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "", "libraryKey" "TCAG", "thumbnailbasecallerargs" "", "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment", "userInput" "", "version" "3.6.56201"}}, "thumbnailanalysisargs" "", "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]}, "inq-024-me" {"barcodes" ["IonXpressRNA_004"]}, "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]}, "inq-037-me" {"barcodes" ["IonXpressRNA_002"]}, "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}}, "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19", "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false, "date" "2013-06-04T03:26:53.000155+00:00", "status" "run", "thumbnailalignmentargs" "", "isOneTimeOverride" false, "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"], "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47, "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "", "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}], "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled"}}
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (experiment (get-experiment ts 50))))

;;; Result record

(expect #ion_torrent_api.core.Result{:id 77, :name "24_reanalyze", :uri "/rundb/api/v1/results/77/", :experiment-uri "/rundb/api/v1/experiment/50/", :status "Completed", :plugin-result-uri-set ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"], :plugin-state-map {"IonReporterUploader" "Completed", "variantCaller" "Completed"}, :analysis-version "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,", :report-status "Nothing", :plugin-store-map {"IonReporterUploader" {}, "variantCaller" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}},
                                     :bam-link "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam",
                                     :fastq-link "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq",
                                     :report-link "/output/Home/24_reanalyze_077/",
                                     :filesystem-path "/results/analysis/output/Home/24_reanalyze_077",
                                     :reference "hg19", :lib-metrics-uri-set ["/rundb/api/v1/libmetrics/68/"], :tf-metrics-uri-set ["/rundb/api/v1/tfmetrics/68/"], :analysis-metrics-uri-set ["/rundb/api/v1/analysismetrics/74/"], :quality-metrics-uri-set ["/rundb/api/v1/qualitymetrics/74/"]  :timestamp #inst "2013-07-23T05:18:31.000209000-00:00", :thumbnail? false :raw-map {"timeToComplete" "0", "tfFastq" "_", "diskusage" 154878, "log" "/output/Home/24_reanalyze_077/log.html", "runid" "ZTVA2", "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}, "framesProcessed" 0, "sffLink" nil, "parentIDs" "", "autoExempt" false, "planShortID" "3XNXT", "metaData" {}, "resultsType" "", "timeStamp" "2013-07-23T05:18:31.000209+00:00", "processedflows" 0, "eas" "/rundb/api/v1/experimentanalysissettings/47/", "projects" ["/rundb/api/v1/project/3/"], "tfSffLink" nil, "processedCycles" 0, "representative" false}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (result (get-result ts "/rundb/api/v1/results/77/"))))

;;; PluginResult record

(expect #ion_torrent_api.core.PluginResult{:name "variantCaller", :version "4.0-r76860",
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
                                           {"size" "25242564174", "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed", "barcoded" "true", "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517", "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}, "Configuration" "Somatic - Proton - Low Stringency", "Target Loci" "Not using", "Trim Reads" true, "Library Type" "AmpliSeq"}, "config" {}, "endtime" "2014-02-17T09:37:51.000879+00:00", "inodes" "391", "starttime" "2014-02-17T05:50:42.000089+00:00", "owner" {"last_login" "2014-04-01T05:48:44.000235+00:00", "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "", "phone_number" "", "resource_uri" "", "title" "user"}, "last_name" "", "username" "ionadmin", "date_joined" "2011-05-03T18:37:38+00:00", "first_name" "", "id" 1, "resource_uri" "/rundb/api/v1/user/1/", "full_name" "", "is_active" true, "email" "ionadmin@iontorrent.com"}, "plugin" {"versionedName" "variantCaller--v4.0-r76860", "config" {}, "path" "/results/plugins/variantCaller", "active" true, "autorunMutable" true, "script" "launch.sh", "name" "variantCaller", "isConfig" false, "date" "2013-11-22T08:38:55.000219+00:00", "url" "", "status" {}, "hasAbout" false, "majorBlock" true, "isPlanConfig" true, "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}, "version" "4.0-r76860", "userinputfields" {}, "id" 54, "resource_uri" "/rundb/api/v1/plugin/54/", "selected" true, "autorun" false, "description" "", "isInstance" true}, "duration" "3:47:09.789983", "jobid" nil}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result (get-plugin-result ts 209))))

(expect {"IonXpressRNA_001" {"hotspots" {}, "variants" {"het_indels" 104, "het_snps" 1046, "homo_indels" 21, "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}, "IonXpressRNA_002" {"hotspots" {}, "variants" {"het_indels" 126, "het_snps" 850, "homo_indels" 24, "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}, "IonXpressRNA_003" {"hotspots" {}, "variants" {"het_indels" 113, "het_snps" 799, "homo_indels" 22, "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}, "IonXpressRNA_004" {"hotspots" {}, "variants" {"het_indels" 127, "het_snps" 937, "homo_indels" 26, "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}, "IonXpressRNA_005" {"hotspots" {}, "variants" {"het_indels" 120, "het_snps" 841, "homo_indels" 21, "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (:barcode-result-map (plugin-result (get-plugin-result ts 209)))))

(expect true
        (=
         (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (plugin-result (get-plugin-result ts 209)))
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp (uri-to-file uri :json))})}
           (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/209/")))))

;;; Results for one experiment

(expect [#inst "2013-07-23T00:32:14.000226000+00:00"
                 ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"]]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          ((juxt :latest-result-date :result-uri-set) (experiment (get-experiment ts 50)))))

(expect [77 "/rundb/api/v1/experiment/50/" "Completed"
                 ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"]
                 {"IonReporterUploader" "Completed", "variantCaller" "Completed"}
                 #inst "2013-07-23T05:18:31.000209000-00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          ((juxt :id :experiment-uri :status :plugin-result-uri-set :plugin-state-map :timestamp)
           (result (get-result ts "/rundb/api/v1/results/77/")))))

(expect [62 "/rundb/api/v1/experiment/50/" "Completed"
                 ["/rundb/api/v1/pluginresult/60/"]
         {"Alignment" "Completed"}
         #inst "2013-06-04T06:12:35.000941000+00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          ((juxt :id :experiment-uri :status :plugin-result-uri-set :plugin-state-map :timestamp)
           (result (get-result ts "/rundb/api/v1/results/62/")))))

(expect [61 "/rundb/api/v1/experiment/50/" "Completed"
         ["/rundb/api/v1/pluginresult/71/", "/rundb/api/v1/pluginresult/66/", "/rundb/api/v1/pluginresult/61/"]
         {"IonReporterUploader" "Completed", "coverageAnalysis" "Completed", "variantCaller" "Completed"}
         #inst "2013-06-04T12:26:33.000330000+00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          ((juxt :id :experiment-uri :status :plugin-result-uri-set :plugin-state-map :timestamp)
           (result (get-result ts "/rundb/api/v1/results/61/")))))

;;; the result with the newest timestamp
(expect [[#inst "2013-07-23T00:32:14.000226000+00:00" #inst "2013-06-03T13:31:54.000000000-00:00"]
                 #inst "2013-07-23T05:18:31.000209000+00:00"
                 #inst "2013-06-04T06:12:35.000941000+00:00"
                 #inst "2013-06-04T12:26:33.000330000+00:00"]
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          [((juxt :latest-result-date :date) (experiment (get-experiment ts 50)))
           (:timestamp (result (get-result ts "/rundb/api/v1/results/77/")))
           (:timestamp (result (get-result ts "/rundb/api/v1/results/62/")))
           (:timestamp (result (get-result ts "/rundb/api/v1/results/61/")))]))

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
   [((juxt :latest-result-date :date) (experiment (get-experiment ts 50)))
    (:timestamp (result (get-result ts "/rundb/api/v1/results/77/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/209/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/89/")))
    (:timestamp (result (get-result ts "/rundb/api/v1/results/62/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/60/")))
    (:timestamp (result (get-result ts "/rundb/api/v1/results/61/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/71/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/66/")))
    ((juxt :start-time :end-time) (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/61/")))
    ]))

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
                         [(:start-time (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/209/")))
                          (:start-time (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/89/")))
                          (:timestamp (result (get-result ts "/rundb/api/v1/results/77/")))
                          ;; newest result is older than its
                          ;; plugin-results, but newer than the
                          ;; experiment latest-result-date
                          (:latest-result-date (experiment (get-experiment ts 50)))
                          ;; and all other plugin-results and results
                          ;; are older than this
                          ;; but pluginresults are newer than their
                          ;; results
                          ;; and I'm not sure how result 61 is newer
                          ;; than 62
                          (:start-time (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/71/"))) ; res 61
                          (:start-time (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/66/"))) ; res 61
                          (:start-time (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/61/"))) ; res 61
                          (:start-time  (plugin-result (get-plugin-result ts "/rundb/api/v1/pluginresult/60/"))) ;res 62
                          (:timestamp (result (get-result ts "/rundb/api/v1/results/61/")))
                          (:timestamp (result (get-result ts "/rundb/api/v1/results/62/")))
                          ;; experiment was first
                          (:date (experiment (get-experiment ts 50)))
                          ])))))

;;; check read and write are equivalent
(expect-let [e (with-fake-routes-in-isolation
                 {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                          {:status 200 :headers {"Content-Type" "application/json"}
                                           :body (slurp (uri-to-file uri :json))})}
                 (experiment (get-experiment ts 50)))]
            e
            (map->Experiment (read-string (pr-str e))))

(expect-let [r (with-fake-routes-in-isolation
                 {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                          {:status 200 :headers {"Content-Type" "application/json"}
                                           :body (slurp (uri-to-file uri :json))})}
                 (result (get-result ts 77)))]
            r
            (map->Result (read-string (pr-str r))))

(expect-let [x (with-fake-routes-in-isolation
                 {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                          {:status 200 :headers {"Content-Type" "application/json"}
                                           :body (slurp (uri-to-file uri :json))})}
                 (plugin-result (get-plugin-result ts 209)))]
            x
            (map->PluginResult (read-string (pr-str x))))

;;; now testing data-readers / toString round-trip

(expect #ion_torrent_api.core.Experiment{:id 9999}
        (edn/read-string {:readers data-readers}
                         (str (map->Experiment { :id 9999}))))

;;; with all fields specified
(expect #ion_torrent_api.core.Experiment{:id 9999, :name nil, :pgm-name nil, :display-name nil, :uri nil, :run-type nil, :chip-type nil, :sample-map nil, :result-uri-set nil, :dir nil, :status nil, :ftp-status nil, :date nil, :latest-result-date nil, :raw-map nil}
        (edn/read-string {:readers data-readers}
                         (str (map->Experiment { :id 9999}))))

(expect #ion_torrent_api.core.Result{:id 99999}
        (edn/read-string {:readers data-readers}
                         (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.core.Result{:id 99999, :name nil, :uri nil, :experiment-uri nil, :status nil, :plugin-result-uri-set nil, :plugin-state-map nil, :analysis-version nil, :report-status nil, :plugin-store-map nil, :bam-link nil, :fastq-link nil, :report-link nil, :filesystem-path nil, :reference nil, :lib-metrics-uri-set nil, :tf-metrics-uri-set nil, :analysis-metrics-uri-set nil, :quality-metrics-uri-set nil, :timestamp nil, :thumbnail? nil, :raw-map nil}
        (edn/read-string {:readers data-readers}
                         (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.core.PluginResult{:id 999, :uri nil, :result-uri nil, :result-name nil, :state nil, :path nil, :report-link nil, :name nil, :version nil, :versioned-name nil, :library-type nil, :config-desc nil, :barcode-result-map nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcoded? nil, :start-time nil, :end-time nil, :raw-map nil}
        (edn/read-string {:readers data-readers}
                         (str (map->PluginResult {:id 999}))))

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
           (experiment (get-experiment ts 97)))))

(expect [{"externalId" "", "name" "INQ0082TT01_F2__2257-112", "displayedName" "INQ0082TT01 F2  2257-112",
          "date" "2014-03-27T01:19:13.000203+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 344, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/344/", "description" ""}
         {"externalId" "", "name" "INQ0159TT01_PS__2257-112", "displayedName" "INQ0159TT01 PS  2257-112",
          "date" "2014-03-27T01:19:13.000206+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 345, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/345/", "description" ""}
         {"externalId" "", "name" "INQ0077ME01_SW__2257-112", "displayedName" "INQ0077ME01 SW  2257-112",
          "date" "2014-03-27T01:19:13.000209+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 346, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/346/", "description" ""}
         {"externalId" "", "name" "INQ0067TT01_35__2257-112", "displayedName" "INQ0067TT01 35  2257-112",
          "date" "2014-03-27T01:19:13.000211+00:00", "status" "run",
          "experiments" ["/rundb/api/v1/experiment/97/"], "id" 347, "sampleSets" [],
          "resource_uri" "/rundb/api/v1/sample/347/", "description" ""}]

        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (:sample-map (experiment (get-experiment ts 97)))))

(expect {"IonXpressRNA_002" "INQ0067TT01 35  2257-112",
         "IonXpressRNA_005" "INQ0077ME01 SW  2257-112",
         "IonXpressRNA_004" "INQ0082TT01 F2  2257-112",
         "IonXpressRNA_013" "INQ0159TT01 PS  2257-112"}
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (:barcode-sample-map (experiment (get-experiment ts 97)))))

;;; Experimnt 97   #inst "2014-03-27T11:24:17.000-00:00"
;;; Result   155   #inst "2014-03-27T11:24:17.000-00:00"
;;; Result   156   #inst "2014-03-27T06:21:05.000-00:00"
;;; Result 155 timestamp is *after* result 156

(expect-let [e97 (with-fake-routes-in-isolation
                   {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                            {:status 200 :headers {"Content-Type" "application/json"}
                                             :body (slurp (uri-to-file uri :json))})}
                   (experiment (get-experiment ts 97)))
             r155 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result (get-result ts 155)))]
            (:latest-result-date e97)
            (:timestamp r155))

(expect-let [r155 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result (get-result ts 155)))
             r156 (with-fake-routes-in-isolation
                    {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                             {:status 200 :headers {"Content-Type" "application/json"}
                                              :body (slurp (uri-to-file uri :json))})}
                    (result (get-result ts 156)))]
            true
            (> (.getTime ^java.util.Date (:timestamp r155))
               (.getTime ^java.util.Date (:timestamp r156))))

;;; borcode map is empty for plugin-result 251 which comes from result 155
(expect nil
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          (:barcode-map (plugin-result (get-plugin-result ts 251)))))
;;;
(expect (more-of coll
                 ion_torrent_api.core.Experiment
                 (first coll)
                 [ion_torrent_api.core.Result ion_torrent_api.core.Result]
                 (map type (rest coll))
                 #inst "2014-03-27T11:24:17.000-00:00"
                 (:latest-result-date (first coll))
                 1395919457000
                 (.getTime ^java.util.Date (:latest-result-date (first coll)))
                 [[1395919457000 155] [1395901265000 156]]
                 (sort-by first > (map (juxt #(.getTime ^java.util.Date (:timestamp %)) :id) (rest coll)))
                 155
                 (:id (latest-result (first coll) (rest coll))))
        (with-fake-routes-in-isolation
          {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                   {:status 200 :headers {"Content-Type" "application/json"}
                                    :body (slurp (uri-to-file uri :json))})}
          [(experiment (get-experiment ts 97))
           (result (get-result ts 155))
           (result (get-result ts 156))]))

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
          (result (get-result ts 155))))

