(ns ion-torrent-api.test.data
  (:require [expectations :refer :all]
            [ion-torrent-api.data :refer :all]
            [clojure.string :as str]))

(defn uri-to-file
  [uri & [ext]]
  (str "test/data" (str/replace uri #"/$" "") (if ext (str "." (name ext)))))

(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file/" :edn))
(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file" :edn))
(expect "test/data/my/path/to/file.json" (uri-to-file "/my/path/to/file/" :json))
(expect "test/data/my/path/to/file" (uri-to-file "/my/path/to/file/"))

;;; experiment
(expect "bob" (experiment-name {"expName" "bob"}))

(expect
 (more-of e
          #{"sequencekitname" "pinnedRepResult" "storageHost" "flowsInOrder" "diskusage" "flows" "baselineRun" 
            "rawdatastyle" "usePreBeadfind" "autoAnalyze" "expCompInfo" "storage_options"
            "pgmName"
            "notes"
            "runtype"
            "chipType"
            "expName"
            "samples"
            "seqKitBarcode"
            "plan"
            "sample"
            "resultDate"
            "sequencekitbarcode" "cycles" "displayName" "runMode" "reagentBarcode" "date" "metaData" "expDir" "reverse_primer" "unique" "star" "status" "isReverseRun" "chipBarcode" "ftpStatus" "user_ack"
            "results"
            "eas_set"
            "id"
            "resource_uri"}
          (experiment-keys e)
          "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24" (experiment-name e)
          50 (experiment-id e)
          "/rundb/api/v1/experiment/50/" (experiment-uri e)
          "AMPS" (experiment-run-type e)
          "900" (experiment-chip-type e)
          "XXXNPROTON" (experiment-pgm-name e)
          ["/rundb/api/v1/results/77/"
          "/rundb/api/v1/results/61/"
          "/rundb/api/v1/results/62/"] (experiment-result-uri e)
          #inst "2013-07-23T00:32:14.000226000-00:00" (experiment-result-date e)
          "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24" (experiment-dir e)
          true (experiment-status-run? e)
          )
 ;; get rid of log slot as it is huge and messes up error output
 (dissoc (read-string (slurp (uri-to-file "/rundb/api/v1/experiment/name-XXX-24" :edn)))
         "log"))

#_{"sequencekitname" "", "notes" "", "pinnedRepResult" false, "storageHost" "localhost",
   "flowsInOrder" "TACGTACGTCTGAGCATCGATCGATGTACAGC", "diskusage" 224837, "flows" 400,
   "baselineRun" false, "seqKitBarcode" "", "plan" "/rundb/api/v1/plannedexperiment/53/",
   "sample" "inq-037-me","sequencekitbarcode" "", "cycles" 12,
   "runMode" "single", "reagentBarcode" "","storage_options" "A", "expCompInfo" "",
   "reverse_primer" "Ion Kit","unique" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24", "star" false,
   "isReverseRun" false, "chipBarcode" "", "ftpStatus" "Complete", "user_ack" "U",
   "usePreBeadfind" false, "autoAnalyze" true, "rawdatastyle" "tiled",

   "id" 50,
   "resource_uri" "/rundb/api/v1/experiment/50/",
   "pgmName" "XXXNPROTON",
   "runtype" "AMPS",
   "chipType" "900",
   "expName" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
   "resultDate" "2013-07-23T00:32:14.000226+00:00", 
   "displayName" "user XXX-24-AmpliSeq CCP 24",
   "date" "2013-06-03T13:31:54+00:00", "metaData" {},
   "expDir" "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
   "status" "run",
   "results" ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"],
   "samples" [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me",
               "date" "2013-06-01T06:30:44.000910+00:00",
               "status" "run",
               "experiments" ["/rundb/api/v1/experiment/50/"
                              "/rundb/api/v1/experiment/47/"
                              "/rundb/api/v1/experiment/49/"],
               "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil}
              {"externalId" "",
               "name" "inq-052-tt",
               "displayedName" "inq-052-tt",
               "date" "2013-06-01T06:30:44.000906+00:00",
               "status" "run",
               "experiments" ["/rundb/api/v1/experiment/50/"
                              "/rundb/api/v1/experiment/47/"
                              "/rundb/api/v1/experiment/49/"],
               "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil}
              {"externalId" "",
               "name" "inq-024-me",
               "displayedName" "inq-024-me",
               "date" "2013-06-03T04:51:46.000218+00:00",
               "status" "run",
               "experiments" ["/rundb/api/v1/experiment/50/"
                              "/rundb/api/v1/experiment/49/"],
               "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil}
              {"externalId" "",
               "name" "inq-022-me",
               "displayedName" "inq-022-me",
               "date" "2013-06-03T04:51:46.000222+00:00",
               "status" "run",
               "experiments" ["/rundb/api/v1/experiment/50/"
                              "/rundb/api/v1/experiment/49/"],
               "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil}
              {"externalId" "",
               "name" "inq-025-tt",
               "displayedName" "inq-025-tt",
               "date" "2013-06-01T06:30:44.000903+00:00",
               "status" "run",
               "experiments" ["/rundb/api/v1/experiment/50/"
                              "/rundb/api/v1/experiment/47/"
                              "/rundb/api/v1/experiment/49/"],
               "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}],
   "eas_set" [{"alignmentargs" "", "barcodeKitName" "IonXpressRNA", "prethumbnailbasecallerargs" "",
               "libraryKey" "TCAG", "thumbnailbasecallerargs" "",
               "selectedPlugins" {"Alignment" {"features" [], "id" "27", "name" "Alignment",
                                               "userInput" "", "version" "3.6.56201"}},
               "thumbnailanalysisargs" "",
               "barcodedSamples" {"inq-022-me" {"barcodes" ["IonXpressRNA_003"]},
                                  "inq-024-me" {"barcodes" ["IonXpressRNA_004"]},
                                  "inq-025-tt" {"barcodes" ["IonXpressRNA_005"]},
                                  "inq-037-me" {"barcodes" ["IonXpressRNA_002"]},
                                  "inq-052-tt" {"barcodes" ["IonXpressRNA_001"]}},
               "libraryKitBarcode" "", "libraryKitName" "", "thumbnailbeadfindargs" "", "reference" "hg19",
               "threePrimeAdapter" "ATCACCGACTGCCCATAGAGAGGCTGAGAC", "isEditable" false,
               "date" "2013-06-04T03:26:53.000155+00:00",
               "status" "run",
               "thumbnailalignmentargs" "", "isOneTimeOverride" false,
               "results" ["/rundb/api/v1/results/77/"
                          "/rundb/api/v1/results/61/"
                          "/rundb/api/v1/results/62/"],
               "targetRegionBedFile" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
               "basecallerargs" "", "analysisargs" "", "hotSpotRegionBedFile" "", "id" 47,
               "resource_uri" "/rundb/api/v1/experimentanalysissettings/47/", "prebasecallerargs" "",
               "isDuplicateReads" false, "beadfindargs" "", "experiment" "/rundb/api/v1/experiment/50/"}],}
