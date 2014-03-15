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

(expect (more-> "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24" experiment-name
                ["/rundb/api/v1/results/77/"
                 "/rundb/api/v1/results/61/"
                 "/rundb/api/v1/results/62/"] experiment-results)
        (read-string (slurp (uri-to-file "/rundb/api/v1/experiment/name-XXX-24" :edn))))
