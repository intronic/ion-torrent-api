(ns ion-torrent-api.expectations.experiment
  (:require [expectations :refer :all]
            [ion-torrent-api.expectations.util :refer :all]
            [clojure.string :as str]))
(comment

  (expect
   (more-of e
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
   (dissoc (read-string (slurp (uri-to-file "/rundb/api/v1/experiment/name-XXX-24" :edn))) "log")))
