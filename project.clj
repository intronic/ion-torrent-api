;; Copyright 2013 Insilico Informatics Pty Ltd
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
;; which can be found in the file epl-v10.html at the root of this distribution.

;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.

;; You must not remove this notice, or any other, from this software.


(defproject intronic/ion-torrent-api "0.1.6"
  :description "Ion Torrent Server API: Convenience functions for accessing data."
  :url "https://github.com/intronic/ion-torrent-api"
  :autodoc {:name "ion-torrent-api", :page-title "Ion Torrent Server API Convenience Functions Documentation."
            :author "Michael Pheasant <mike@insilico.io>"
            :copyright "2013 © Insilico Informatics Pty Ltd, AU."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.generic "0.1.2"]
                 ;;[clj-http "0.9.0"]
                 [clj-http "0.7.9"]
                 [clj-http-fake "0.7.8"]]

  :source-paths ["src"]
  :test-paths ["test"]

;;  :jvm-opts [~(str "-Xmx" (* (.getTotalPhysicalMemorySize (java.lang.management.ManagementFactory/getOperatingSystemMXBean)) 1/2))]
  :jvm-opts [ "-Xmx2g" ]

  :profiles {:dev {:dependencies [[expectations "1.4.52"]]}}

  :scm {:name "git"
        :url "https://github.com/intronic/ion-torrent-api"
        :tag "3e7ef41804a7012d72462a3247b9ba97286dfc24"}
  :global-vars {*warn-on-reflection* true})
