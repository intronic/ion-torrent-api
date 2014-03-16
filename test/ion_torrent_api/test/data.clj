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


;;; ;;;;;;;;;;;;;;;;;
;;; Experiment

(expect "bob" (experiment-name {"expName" "bob"}))

(expect
 (more-of e
          #{"sequencekitname" "pinnedRepResult" "storageHost" "flowsInOrder" "diskusage" "flows" "baselineRun"
            "rawdatastyle" "usePreBeadfind" "autoAnalyze" "expCompInfo" "storage_options"
            "sequencekitbarcode" "cycles" "runMode" "reagentBarcode" "metaData" "notes"
            "reverse_primer" "unique" "star" "isReverseRun" "chipBarcode" "ftpStatus"
            "user_ack" "eas_set" "seqKitBarcode" "plan"
            ;; interesting keys:
            "id"
            "expName"
            "displayName"
            "resource_uri"
            "pgmName"
            "date"
            "runtype"
            "chipType"
            "expDir"
            "status"
            "samples"
            "sample"
            "resultDate"
            "results"}
          (experiment-keys e)

          "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24" (experiment-name e)
          "user XXX-24-AmpliSeq CCP 24" (experiment-display-name e)
          50 (experiment-id e)
          "/rundb/api/v1/experiment/50/" (experiment-uri e)
          "AMPS" (experiment-run-type e)
          "900" (experiment-chip-type e)
          "XXXNPROTON" (experiment-pgm-name e)
          #inst "2013-06-03T13:31:54+00:00" (experiment-date e)

          ["/rundb/api/v1/results/77/"
          "/rundb/api/v1/results/61/"
          "/rundb/api/v1/results/62/"]
          (experiment-result-uri e)

          #inst "2013-07-23T00:32:14.000226000-00:00" (experiment-result-date e)
          "/rawdata/XXXNPROTON/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24" (experiment-dir e)

          true (experiment-run? e)
          true (experiment-complete? e)

          [{"externalId" "", "name" "inq-037-me", "displayedName" "inq-037-me", "date" "2013-06-01T06:30:44.000910+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 76, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/76/", "description" nil} {"externalId" "", "name" "inq-052-tt", "displayedName" "inq-052-tt", "date" "2013-06-01T06:30:44.000906+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 75, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/75/", "description" nil} {"externalId" "", "name" "inq-024-me", "displayedName" "inq-024-me", "date" "2013-06-03T04:51:46.000218+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 83, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/83/", "description" nil} {"externalId" "", "name" "inq-022-me", "displayedName" "inq-022-me", "date" "2013-06-03T04:51:46.000222+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/49/"], "id" 84, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/84/", "description" nil} {"externalId" "", "name" "inq-025-tt", "displayedName" "inq-025-tt", "date" "2013-06-01T06:30:44.000903+00:00", "status" "run", "experiments" ["/rundb/api/v1/experiment/50/" "/rundb/api/v1/experiment/47/" "/rundb/api/v1/experiment/49/"], "id" 74, "sampleSets" [], "resource_uri" "/rundb/api/v1/sample/74/", "description" nil}]
          (experiment-sample-maps e)

          '("inq-022-me" "inq-024-me" "inq-025-tt" "inq-037-me" "inq-052-tt")
          (experiment-sample-names e)

          ["IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"]
          (experiment-barcodes e)

          ["inq-022-me" "inq-024-me" "inq-025-tt" "inq-037-me" "inq-052-tt"]
          (experiment-samples e)

          {"IonXpressRNA_003" "inq-022-me", "IonXpressRNA_004" "inq-024-me", "IonXpressRNA_005" "inq-025-tt", "IonXpressRNA_002" "inq-037-me", "IonXpressRNA_001" "inq-052-tt"}
          (experiment-barcode-sample-map e)

          {"IonXpressRNA_001" ["inq-052-tt"], "IonXpressRNA_002" ["inq-037-me"], "IonXpressRNA_005" ["inq-025-tt"], "IonXpressRNA_004" ["inq-024-me"], "IonXpressRNA_003" ["inq-022-me"]}
          (experiment-barcode-sample-map-with-dups e))

 ;; get rid of log slot as it is huge and messes up error output
 (dissoc (read-string (slurp (uri-to-file "/rundb/api/v1/experiment/name-XXX-24" :edn))) "log"))

;;; ;;;;;;;;;;;;;;;;;
;;; Result

(expect
 (more-of r
          #{"reportstorage" "diskusage"  "framesProcessed" "autoExempt" "parentIDs" "processedCycles" "planShortID"
            "metaData" "reportStatus" "processedflows" "eas" "projects" "representative" "tfSffLink" "sffLink"
            "id"
            "resource_uri"
            "analysisVersion"
            "resultsName"
            "timeToComplete"
            "bamLink"
            "tfFastq"
            "runid"
            "pluginresults"
            "qualitymetrics"
            "analysismetrics"
            "libmetrics"
            "tfmetrics"
            "reference"
            "pluginState"
            "pluginStore"
            "reportLink"
            "fastqLink"
            "resultsType"
            "filesystempath"
            "status"
            "timeStamp"
            "experiment"}
          (result-keys r)

          77 (result-id r)
          "/rundb/api/v1/results/77/" (result-uri r)
          "24_reanalyze" (result-name r)
          "/output/Home/24_reanalyze_077/" (result-report-link r)
          "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_AIB-24-AmpliSeq_CCP_24_24_reanalyze.bam"
          (result-bam-link r)
          "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_AIB-24-AmpliSeq_CCP_24_24_reanalyze.fastq"
          (result-fastq-link r)
          "/results/analysis/output/Home/24_reanalyze_077" (result-file-path r)
          "Completed" (result-status r)
          #inst "2013-07-23T05:18:31.000209+00:00" (result-timestamp r)
          "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1," (result-version r)
          "/rundb/api/v1/experiment/50/" (result-experiment r)
          {"IonReporterUploader" "Completed", "variantCaller" "Completed"}
          (result-plugin-state r)
          ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"]
          (result-plugin-results r)
          ["/rundb/api/v1/tfmetrics/68/"] (result-tf-metrics r)
          ["/rundb/api/v1/libmetrics/68/"] (result-lib-metrics r)
          ["/rundb/api/v1/qualitymetrics/74/"] (result-quality-metrics r)
          ["/rundb/api/v1/analysismetrics/74/"] (result-analysis-metrics r)
          "ZTVA2" (result-run-id r)
          "hg19" (result-reference r)
          ["/rundb/api/v1/project/3/"] (result-projects r)
          {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"}
          (result-report-storage r)
          #{"IonReporterUploader" "variantCaller"}
          (into #{} (keys (result-plugin-store r)))
          "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed"
          (get-in (result-plugin-store r) ["variantCaller" "targets_bed"]))

 ;; get rid of log slot as it is huge and messes up error output
 (dissoc (read-string (slurp (uri-to-file "/rundb/api/v1/results/77/" :edn))) "log"))

;;; ;;;;;;;;;;;;;;;;;
;;; Example Raw Experiment data (excluding massing "log" slot)


{"resultsName" "24_reanalyze",
 "reportLink" "/output/Home/24_reanalyze_077/",
 "bamLink" "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_AIB-24-AmpliSeq_CCP_24_24_reanalyze.bam",
 "fastqLink" "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_AIB-24-AmpliSeq_CCP_24_24_reanalyze.fastq",
 "filesystempath" "/results/analysis/output/Home/24_reanalyze_077",
 "status" "Completed",
 "timeStamp" "2013-07-23T05:18:31.000209+00:00",
 "id" 77,
 "resource_uri" "/rundb/api/v1/results/77/",
 "analysisVersion" "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1,",
 "experiment" "/rundb/api/v1/experiment/50/",
 "pluginState" {"IonReporterUploader" "Completed", "variantCaller" "Completed"},
 "pluginresults" ["/rundb/api/v1/pluginresult/209/" "/rundb/api/v1/pluginresult/89/"],
 "tfmetrics" ["/rundb/api/v1/tfmetrics/68/"],
 "libmetrics" ["/rundb/api/v1/libmetrics/68/"]
 "qualitymetrics" ["/rundb/api/v1/qualitymetrics/74/"],
 "analysismetrics" ["/rundb/api/v1/analysismetrics/74/"],
 "runid" "ZTVA2",
 "reference" "hg19",
 "reportStatus" "Nothing",
 "projects" ["/rundb/api/v1/project/3/"],
 "pluginStore" {"IonReporterUploader" {},
                "variantCaller"
                {"Aligned Reads" "R_2013_06_03_23_30_18_user_AIB-24-AmpliSeq_CCP_24",
                 "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
                 "barcoded" "true",
                 "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517",
                 "barcodes" {"IonXpressRNA_001"
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
                                          "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}},
                                 "Configuration" "Somatic - Proton - Low Stringency",
                                 "Target Loci" "Not using",
                                 "Trim Reads" true,
                                 "Library Type" "AmpliSeq"}},
 "reportstorage" {"default" true, "dirPath" "/results/analysis/output", "id" 1, "name" "Home", "resource_uri" "", "webServerPath" "/output"},
 "framesProcessed" 0,  "sffLink" nil,  "planShortID" "3XNXT",
 "log" "/output/Home/24_reanalyze_077/log.html",
 "eas" "/rundb/api/v1/experimentanalysissettings/47/",
 "timeToComplete" "0", "tfFastq" "_", "diskusage" 154878,
 "metaData" {}, "resultsType" "", "parentIDs" "", "autoExempt" false,
 "representative" false, "processedCycles" 0, "processedflows" 0, "tfSffLink" nil,
}

;;; ;;;;;;;;;;;;;;;;;
;;; Example Raw Experiment data (excluding massing "log" slot)

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
