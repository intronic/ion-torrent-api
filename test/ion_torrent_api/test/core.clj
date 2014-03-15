(ns ion-torrent-api.test.core
  (:require [expectations :refer :all]
            [ion-torrent-api.core :refer :all :as ion]
            [clj-http.client :as client]
            [clj-http.fake :refer :all]
            [clojure.string :as str]))

(def creds ["dummy" "dummy"])
(def host "http://my-intranet-torrent-server.com")

(defn uri-to-file
  [uri & [ext]]
  (str "test/data" (str/replace uri #"/$" "") (if ext (str "." (name ext)))))

(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file/" :edn))
(expect "test/data/my/path/to/file.edn" (uri-to-file "/my/path/to/file" :edn))
(expect "test/data/my/path/to/file.json" (uri-to-file "/my/path/to/file/" :json))
(expect "test/data/my/path/to/file" (uri-to-file "/my/path/to/file/"))

;;; test private functions by accessing value of symbols directly
(expect "abcdef" (#'ion/ensure-starts-with "abc" "def"))
(expect "abcxabcdef" (#'ion/ensure-starts-with "abc" "xabcdef"))
(expect "abcdef" (#'ion/ensure-starts-with "abc" "abcdef"))

;;; utilities
(expect [{"id" 3} {"id" 2} {"id" 1}] (sort-by-id-desc [{"id" 2} {"id" 1} {"id" 3}]))

;;; Reading from dummy torrent server
(expect {:status 200}
        (in (with-fake-routes-in-isolation
              {#".*/rundb/api/v1/.*" (fn [{uri :uri :as req}]
                                       {:status 200 :headers {} :body (slurp (uri-to-file uri :json))})}

              (client/get (str host  "/rundb/api/v1/" "experiment/schema/")))))

;;; Torrent Objects
(expect "bob" (experiment-name {"expName" "bob"}))

(given (read-string (slurp "test/data/rundb/api/v1/experiment/name-XXX-24.edn"))
       (expect experiment-name "R_2013_06_03_23_30_18_user_XXX-24-AmpliSeq_CCP_24"
               experiment-results ["/rundb/api/v1/results/77/" "/rundb/api/v1/results/61/" "/rundb/api/v1/results/62/"]))
