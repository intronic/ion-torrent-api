(ns ion-torrent-api.test.core
  (:require [expectations :refer :all]
            [ion-torrent-api.core :refer :all :as ion]))

;;; test private functions by accessing value of symbols directly
(expect "abcdef" (#'ion/ensure-starts-with "abc" "def"))
(expect "abcxabcdef" (#'ion/ensure-starts-with "abc" "xabcdef"))
(expect "abcdef" (#'ion/ensure-starts-with "abc" "abcdef"))

