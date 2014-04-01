(ns ion-torrent-api.expectations.result
  (:require [expectations :refer :all]
            [ion-torrent-api.result :refer :all]
            [ion-torrent-api.expectations.util :refer :all]
            [clojure.string :as str]))

(comment
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
            "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam"
            (result-bam-link r)
            "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq"
            (result-fastq-link r)
            "/results/analysis/output/Home/24_reanalyze_077" (result-file-path r)

            "Completed" (result-status r)
            true (result-complete? r)

            #inst "2013-07-23T05:18:31.000209+00:00" (result-timestamp r)
            "db:3.6.52-1,al:3.6.3-1,an:3.6.39-1," (result-version r)
            "/rundb/api/v1/experiment/50/" (result-experiment r)
            {"IonReporterUploader" "Completed", "variantCaller" "Completed"}
            (result-plugin-status r)
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
            (get-in (result-plugin-store r) ["variantCaller" "targets_bed"])

            "/output/Home/24_reanalyze_077/IonXpressRNA_001_rawlib.bam"
            (result-api-path-bam r "IonXpressRNA_001")

            "/output/Home/24_reanalyze_077/IonXpressRNA_001_rawlib.bam.bai"
            (result-api-path-bai r "IonXpressRNA_001")

            "/report/latex/77.pdf"
            (result-api-path-pdf r)
            )

   ;; get rid of log slot as it is huge and messes up error output
   (dissoc (read-string (slurp (uri-to-file "/rundb/api/v1/results/77/" :edn))) "log"))

;;; ;;;;;;;;;;;;;;;;;
;;; Example Raw Result data

  #_{"resultsName" "24_reanalyze",
     "reportLink" "/output/Home/24_reanalyze_077/",
     "bamLink" "/output/Home/24_reanalyze_077/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.bam",
     "fastqLink" "/output/Home/24_reanalyze_077/basecaller_results/R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24_24_reanalyze.fastq",
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
  )
