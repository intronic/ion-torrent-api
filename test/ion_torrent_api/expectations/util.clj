(ns ion-torrent-api.expectations.util
  (:require [expectations :refer :all]
            [clojure.string :as str]))

(defn uri-to-file
  [uri & [ext]]
  (str "test/data" (str/replace uri #"/$" "") (if ext (str "." (name ext)))))

(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file/" :edn))
(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file" :edn))
(expect "test/data/my/path/to/file.json" (uri-to-file "/my/path/to/file/" :json))
(expect "test/data/my/path/to/file" (uri-to-file "/my/path/to/file/"))

(comment
  ;; to get test data from torrent:
  (def creds ["ion user" "ion pass"])
  (def host2 "http://my-internal-torrent-server.com")

  (spit "test/data/rundb/api/v1/results/77.edn" (pr-str (ion/get-result-uri creds host2 "/rundb/api/v1/results/77/")))
  (spit "test/data/rundb/api/v1/results/62.edn" (pr-str (ion/get-result-uri creds host2 "/rundb/api/v1/results/62/")))
  (spit "test/data/rundb/api/v1/results/61.edn" (pr-str (ion/get-result-uri creds host2 "/rundb/api/v1/results/61/")))

  (spit "test/data/rundb/api/v1/pluginresult/209.edn" (pr-str (ion/get-plugin-result-uri creds host2 "/rundb/api/v1/pluginresult/209/")))
  (spit "test/data/rundb/api/v1/pluginresult/89.edn" (pr-str (ion/get-plugin-result-uri creds host2 "/rundb/api/v1/pluginresult/89/")))


  ;; get single experiment to file
  (#'ion/get-resource-file-to-file creds host2 "/rundb/api/v1/experiment/" "test/data/rundb/api/v1/experiment/name-XXX-24.json" {"status__exact" "run" "expName__exact" "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"})

  ;; get 20 experiments
  (#'ion/get-resource-file-to-file creds host2 "/rundb/api/v1/experiment/" "test/data/rundb/api/v1/experiment.json")
)
