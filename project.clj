;; Copyright 2013 Insilico Informatics Pty Ltd
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
;; which can be found in the file epl-v10.html at the root of this distribution.

;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.

;; You must not remove this notice, or any other, from this software.


(defproject intronic/ion-torrent-api "0.3.7"
  :description "Ion Torrent Server API: Convenience functions for accessing data."
  :url "https://github.com/intronic/ion-torrent-api"
  :autodoc {:name "ion-torrent-api", :page-title "Ion Torrent Server API Convenience Functions Documentation."
            :author "Michael Pheasant <mike@insilico.io>"
            :copyright "2013 © Insilico Informatics Pty Ltd, AU."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/algo.generic "0.1.2"]
                 [org.clojure/tools.cli "0.3.1"]
                 [clj-time "0.9.0"]
                 [clj-http "1.1.0"]
                 [clj-http-fake "1.0.1"]
                 [slingshot "0.12.2"]
                 [prismatic/schema "0.4.0"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/tools.trace "0.7.8"]
                                  [expectations "2.1.0"]
                                  [difftest "1.3.8"]]}}
  :plugins [[lein-expectations "0.0.8"]]

  :lein-release {:deploy-via :clojars
                 :scm :git}
  :scm {:name "git"
        :url "https://github.com/intronic/ion-torrent-api"}
  :global-vars {*warn-on-reflection* true})
