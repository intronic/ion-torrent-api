;; Copyright 2013 Insilico Informatics Pty Ltd
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
;; which can be found in the file epl-v10.html at the root of this distribution.

;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.

;; You must not remove this notice, or any other, from this software.


(defproject intronic/ion-torrent-api "0.1.6"
  :description "Ion Torrent Server API"
  :url "https://github.com/intronic/ion-torrent-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.generic "0.1.1"]
                 [clj-http "0.7.7"]]
  :scm {:name "git"
        :url "https://github.com/intronic/ion-torrent-api"
        :tag "308b05b4a4339706baa3dd47e18d944c97ff2dbe"}
  :global-vars {*warn-on-reflection* true})
