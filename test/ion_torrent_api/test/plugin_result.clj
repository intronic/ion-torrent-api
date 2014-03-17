(ns ion-torrent-api.test.plugin-result
  (:require [expectations :refer :all]
            [ion-torrent-api.test.util :refer :all]
            [ion-torrent-api.plugin-result :refer :all]
            [clojure.string :as str]))
(expect
 (more-of pr
          #{"id"
            "resource_uri"
            "result"
            "size"
            "store" "config" "path" "resultName" "endtime" "inodes" "reportLink" "starttime" "state" "owner" "plugin" "duration" "jobid" }
          (plugin-result-keys pr)

          209 (plugin-result-id pr)
          "/rundb/api/v1/pluginresult/209/" (plugin-result-uri pr)
          "/rundb/api/v1/results/77/" (plugin-result-result-uri pr)

          "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out"
          (plugin-result-path pr)

          "/output/Home/24_reanalyze_077/" (plugin-result-report-link pr)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out"
          (plugin-result-api-path-prefix pr)

          ;; note: this is note a coverage plugin so amplicon path returns nil,
          ;; need to test with different plugin-result record
          nil?
          (plugin-result-api-path-coverage-amplicon-file pr "IonXpressRNA_001")

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/"
          (plugin-result-api-path-tsvc-variant-prefix pr)

          "Completed" (plugin-result-status pr)
          true (plugin-result-complete? pr)

          #inst "2014-02-17T09:37:51.000879+00:00" (plugin-result-end-time pr)
          #inst "2014-02-17T05:50:42.000089+00:00" (plugin-result-start-time pr)

          "3:47:09.789983" (plugin-result-duration pr)
          "25242564174" (plugin-result-size pr)
          "24_reanalyze" (plugin-result-result-name pr)

          {"versionedName" "variantCaller--v4.0-r76860"
           "config" {}
           "path" "/results/plugins/variantCaller"
           "active" true,
           "autorunMutable" true,
           "script" "launch.sh"
           "name" "variantCaller"
           "isConfig" false,
           "date" "2013-11-22T08:38:55.000219+00:00"
           "url" ""
           "status" {} "hasAbout" false, "majorBlock" true, "isPlanConfig" true,
           "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}
           "version" "4.0-r76860",
           "userinputfields" {},
           "id" 54,
           "resource_uri" "/rundb/api/v1/plugin/54/",
           "selected" true, "autorun" false, "description" "", "isInstance" true}
          (plugin-result-plugin pr)

          "variantCaller" (plugin-result-plugin-name pr)
          true (plugin-result-variant-caller? pr)
          false (plugin-result-coverage? pr)

          {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
           "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
           "barcoded" "true",
           "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517",
           "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants"
                                           {"het_indels" 104, "het_snps" 1046, "homo_indels" 21,
                                            "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}
                       "IonXpressRNA_002" {"hotspots" {}, "variants"
                                           {"het_indels" 126, "het_snps" 850, "homo_indels" 24,
                                            "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}
                       "IonXpressRNA_003" {"hotspots" {}, "variants"
                                           {"het_indels" 113, "het_snps" 799, "homo_indels" 22,
                                            "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}
                       "IonXpressRNA_004" {"hotspots" {}, "variants"
                                           {"het_indels" 127, "het_snps" 937, "homo_indels" 26,
                                            "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}
                       "IonXpressRNA_005" {"hotspots" {}, "variants"
                                           {"het_indels" 120, "het_snps" 841, "homo_indels" 21,
                                            "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
           "Configuration" "Somatic - Proton - Low Stringency",
           "Target Loci" "Not using",
           "Trim Reads" true,
           "Library Type" "AmpliSeq"}
          (plugin-result-store pr)

          ["IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"]
          (plugin-result-barcodes pr)

          {"IonXpressRNA_001" {"hotspots" {}, "variants"
                               {"het_indels" 104, "het_snps" 1046, "homo_indels" 21,
                                "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}
           "IonXpressRNA_002" {"hotspots" {}, "variants"
                               {"het_indels" 126, "het_snps" 850, "homo_indels" 24,
                                "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}
           "IonXpressRNA_003" {"hotspots" {}, "variants"
                               {"het_indels" 113, "het_snps" 799, "homo_indels" 22,
                                "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}
           "IonXpressRNA_004" {"hotspots" {}, "variants"
                               {"het_indels" 127, "het_snps" 937, "homo_indels" 26,
                                "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}
           "IonXpressRNA_005" {"hotspots" {}, "variants"
                               {"het_indels" 120, "het_snps" 841, "homo_indels" 21,
                                "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
          (plugin-result-barcode-counts pr)

          "variantCaller--v4.0-r76860" (plugin-result-versioned-name pr)

          "Somatic - Proton - Low Stringency" (plugin-result-configuration pr)

          "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed"
          (plugin-result-target-bed-file pr)

          "4477685_Comprehensive_CCP_bedfile_20120517.bed"
          (plugin-result-target-bed-file-name pr)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/4477685_Comprehensive_CCP_bedfile_20120517.bed"
          (plugin-result-api-path-tsvc-variant-target-region pr)

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/TSVC_variants.vcf.gz"
          (plugin-result-api-path-tsvc-variant-file pr "IonXpressRNA_001")

          "/output/Home/24_reanalyze_077/plugin_out/variantCaller_out/IonXpressRNA_001/TSVC_variants.vcf.gz.tbi"
          (plugin-result-api-path-tsvc-variant-tbi-file pr "IonXpressRNA_001")
          )
 (read-string (slurp (uri-to-file "/rundb/api/v1/pluginresult/209/" :edn))))

;;; ;;;;;;;;;;;;;;;;;
;;; Example Raw Plugin Result data

#_{
 "id" 209,
 "resource_uri" "/rundb/api/v1/pluginresult/209/",
 "result" "/rundb/api/v1/results/77/"
 "size" "25242564174",
 "config" {},
 "path" "/results/analysis/output/Home/24_reanalyze_077/plugin_out/variantCaller_out",
 "resultName" "24_reanalyze",
 "endtime" "2014-02-17T09:37:51.000879+00:00",
 "inodes" "391",
 "reportLink" "/output/Home/24_reanalyze_077/",
 "starttime" "2014-02-17T05:50:42.000089+00:00",
 "state" "Completed",
 "duration" "3:47:09.789983",
 "jobid" nil,

 "plugin" {"versionedName" "variantCaller--v4.0-r76860"
           "config" {}
           "path" "/results/plugins/variantCaller"
           "active" true,
           "autorunMutable" true,
           "script" "launch.sh"
           "name" "variantCaller"
           "isConfig" false,
           "date" "2013-11-22T08:38:55.000219+00:00"
           "url" ""
           "status" {} "hasAbout" false, "majorBlock" true, "isPlanConfig" true,
           "pluginsettings" {"depends" [], "features" [], "runlevel" [], "runtype" ["composite" "wholechip" "thumbnail"]}
           "version" "4.0-r76860",
           "userinputfields" {},
           "id" 54,
           "resource_uri" "/rundb/api/v1/plugin/54/",
           "selected" true, "autorun" false, "description" "", "isInstance" true},

 "store" {"Aligned Reads" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24",
          "targets_bed" "/results/uploads/BED/1/hg19/unmerged/detail/4477685_Comprehensive_CCP_bedfile_20120517.bed",
          "barcoded" "true",
          "Target Regions" "4477685_Comprehensive_CCP_bedfile_20120517",
          "barcodes" {"IonXpressRNA_001" {"hotspots" {}, "variants"
                                          {"het_indels" 104, "het_snps" 1046, "homo_indels" 21,
                                           "homo_snps" 267, "no_call" 0, "other" 9, "variants" 1447}}
                      "IonXpressRNA_002" {"hotspots" {}, "variants"
                                          {"het_indels" 126, "het_snps" 850, "homo_indels" 24,
                                           "homo_snps" 306, "no_call" 0, "other" 6, "variants" 1312}}
                      "IonXpressRNA_003" {"hotspots" {}, "variants"
                                          {"het_indels" 113, "het_snps" 799, "homo_indels" 22,
                                           "homo_snps" 303, "no_call" 0, "other" 11, "variants" 1248}}
                      "IonXpressRNA_004" {"hotspots" {}, "variants"
                                          {"het_indels" 127, "het_snps" 937, "homo_indels" 26,
                                           "homo_snps" 292, "no_call" 0, "other" 6, "variants" 1388}}
                      "IonXpressRNA_005" {"hotspots" {}, "variants"
                                          {"het_indels" 120, "het_snps" 841, "homo_indels" 21,
                                           "homo_snps" 316, "no_call" 0, "other" 6, "variants" 1304}}}
          "Configuration" "Somatic - Proton - Low Stringency",
          "Target Loci" "Not using",
          "Trim Reads" true,
          "Library Type" "AmpliSeq"},

 "owner" {"last_login" "2014-03-16T06:12:16.000409+00:00",
          "profile" {"id" 1, "last_read_news_post" "2013-11-02T02:33:07.000710+00:00", "name" "", "note" "",
                     "phone_number" "", "resource_uri" "", "title" "user"},
          "last_name" "",
          "username" "ionadmin",
          "date_joined" "2011-05-03T18:37:38+00:00",
          "first_name" "",
          "id" 1,
          "resource_uri" "/rundb/api/v1/user/1/",
          "full_name" "",
          "is_active" true,
          "email" "ionadmin@iontorrent.com"},
}

