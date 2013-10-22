(ns ion-torrent-api.core-test
  (:require [clojure.test :refer :all]
            [ion-torrent-api.core :refer :all :as ion]))

(deftest string-test
  (testing "ensure-starts-with"
    (is (= "abcdef" (#'ion/ensure-starts-with "abc" "def")))
    (is (= "abcxabcdef" (#'ion/ensure-starts-with "abc" "xabcdef")))
    (is (= "abcdef" (#'ion/ensure-starts-with "abc" "abcdef")))))
