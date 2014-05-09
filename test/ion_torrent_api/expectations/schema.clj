(ns ion-torrent-api.expectations.schema
  (:require [expectations :refer :all]
            [ion-torrent-api.expectations.util :refer :all]
            [ion-torrent-api.schema :refer :all]
            [clojure [string :as str] [edn :as edn]])
  (:import [ion_torrent_api.schema Experiment Result PluginResult TorrentServer]))

(def creds ["user" "pass"])
(def host "http://my-intranet-torrent-server.com")
(def ts (torrent-server host :creds creds))
;;; utilities

;;; Note: test private functions by accessing vars directly
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :version :v1 :api-path "/rundb/api/v1/"}
        ts)

(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :version :v1 :api-path "/rundb/api/v1/"}
        (torrent-server host :version :v1))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :version :v1 :api-path "/some/other/path"}
        (torrent-server host :version :v1 :api-path "/some/other/path"))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :version :v2 :api-path nil}
        (torrent-server host :version :v2))
(expect #ion_torrent_api.schema.TorrentServer{:server-url "http://my-intranet-torrent-server.com"
                                            :version :v2 :api-path "/some/other/path"}
        (torrent-server host :version :v2 :api-path "/some/other/path"))

(expect creds (:creds (meta ts)))
(expect "/rundb/api/v1/" (:api-path ts))
(expect (torrent-server "h")
        (torrent-server "h"))
(expect (torrent-server "h")
        (read-string (str (torrent-server "h"))))
(expect-let [ts (torrent-server "h" :api-path "p")]
            ts
            (edn/read-string {:readers data-readers} (str ts)))
(expect-let [ts (torrent-server "h" :api-path "p")]
            ts
            (read-string (str ts)))


;;; now testing data-readers / toString round-trip

(expect #ion_torrent_api.schema.Experiment{:id 9999}
        (edn/read-string {:readers data-readers} (str (map->Experiment { :id 9999}))))

;;; with all fields specified
(expect #ion_torrent_api.schema.Experiment{:id 9999, :name nil, :pgm-name nil, :display-name nil, :uri nil, :run-type nil, :chip-type nil, :sample-map nil, :result-uri-set nil, :dir nil, :status nil, :ftp-status nil, :date nil, :latest-result-date nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->Experiment { :id 9999}))))

(expect #ion_torrent_api.schema.Result{:id 99999}
        (edn/read-string {:readers data-readers} (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.schema.Result{:id 99999, :name nil, :uri nil, :experiment-uri nil, :status nil, :plugin-result-uri-set nil, :plugin-state-map nil, :analysis-version nil, :report-status nil, :plugin-store-map nil, :bam-link nil, :fastq-link nil, :report-link nil, :filesystem-path nil, :reference nil, :lib-metrics-uri-set nil, :tf-metrics-uri-set nil, :analysis-metrics-uri-set nil, :quality-metrics-uri-set nil, :timestamp nil, :thumbnail? nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->Result { :id 99999}))))

(expect #ion_torrent_api.schema.PluginResult{:type nil :id 999, :uri nil, :result-uri nil, :result-name nil, :state nil, :path nil, :report-link nil, :name nil, :version nil, :versioned-name nil, :library-type nil, :config-desc nil, :barcode-result-map nil, :target-name nil, :target-bed nil, :experiment-name nil, :trimmed-reads? nil, :barcoded? nil, :start-time nil, :end-time nil, :raw-map nil}
        (edn/read-string {:readers data-readers} (str (map->PluginResult {:id 999}))))
