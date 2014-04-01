(ns ion-torrent-api.expectations.core
  (:require [expectations :refer :all]
            [ion-torrent-api.core :refer :all :as ion]
            [ion-torrent-api.expectations.util :refer :all]
            [clj-http.client :as client]
            [clj-http.fake :refer :all]
            [clojure.string :as str])
  #_(:import [ion-torrent-api.core TorrentServer]))

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
                      (experiments ts 20 20))
                    "objects")))

(expect '("sequencekitname" "pgmName" "notes" "pinnedRepResult" "storageHost" "flowsInOrder" "diskusage" "log" "runtype" "flows" "chipType" "baselineRun" "expName" "samples" "seqKitBarcode" "plan" "sample" "resultDate" "sequencekitbarcode" "cycles" "displayName" "runMode" "reagentBarcode" "date" "metaData" "expDir" "reverse_primer" "unique" "star" "status" "isReverseRun" "chipBarcode" "ftpStatus" "user_ack" "results" "storage_options" "expCompInfo" "eas_set" "id" "resource_uri" "usePreBeadfind" "autoAnalyze" "rawdatastyle")
        (keys 
         (with-fake-routes-in-isolation
           {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                    {:status 200 :headers {"Content-Type" "application/json"}
                                     :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
           (experiment-name ts "name-XXX-24"))))

(expect {"pgmName" "XXXNPROTON" "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {"Content-Type" "application/json"}
                                        :body (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.json")})}
              (experiment-name ts "name-XXX-24"))))

(expect "/rundb/api/v1/" (:api-path ts))

(expect "/rundb/api/v1/results/77"
        (let [id-or-uri 77] (#'ion/ensure-starts-with (str (:api-path ts) "results/")
                                                (str id-or-uri))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (result ts 77))))

(expect '("timeToComplete" "resultsName" "tfFastq" "pluginresults" "diskusage" "qualitymetrics" "log" "runid" "reportStatus" "analysismetrics" "reportstorage" "bamLink" "framesProcessed" "reference" "sffLink" "pluginStore" "parentIDs" "autoExempt" "planShortID" "reportLink" "fastqLink" "metaData" "resultsType" "filesystempath" "tfmetrics" "status" "timeStamp" "processedflows" "eas" "projects" "tfSffLink" "id" "processedCycles" "resource_uri" "analysisVersion" "representative" "experiment" "pluginState" "libmetrics")
                (keys (with-fake-routes-in-isolation
                        {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                                 {:status 200 :headers {"Content-Type" "application/json"}
                                                  :body (slurp (uri-to-file uri :json))})}
                        (result ts {} 77))))


(comment "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}])

