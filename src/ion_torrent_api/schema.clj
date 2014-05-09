(ns ion-torrent-api.schema
  (:require [clojure.core :as core]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [slingshot.slingshot :refer (try+ throw+)]))


(defrecord Experiment [torrent-server id name pgm-name display-name uri run-type chip-type
                       result-uri-set dir status ftp-status sample-map barcode-sample-map date
                       latest-result-date latest-result raw-map]

  Object
  (toString [this] (pr-str this)))


(defrecord Result [torrent-server id name uri experiment-uri status
                   plugin-result-uri-set plugin-state-map analysis-version report-status plugin-store-map
                   bam-link fastq-link report-link filesystem-path reference
                   lib-metrics-uri-set tf-metrics-uri-set analysis-metrics-uri-set quality-metrics-uri-set
                   timestamp thumbnail? plugin-result-set
                   lib-metrics-set tf-metrics-set analysis-metrics-set quality-metrics-set
                   raw-map]

  Object
  (toString [this] (pr-str this)))


(defrecord PluginResult [type torrent-server id uri result-uri result-name state path report-link
                         name version versioned-name
                         library-type config-desc target-name target-bed experiment-name
                         trimmed-reads? barcode-result-map barcoded? start-time end-time raw-map]

  Object
  (toString [this] (pr-str this)))


(defrecord TorrentServer [server-url version api-path]

  Object
  (toString [this] (pr-str this)))


(defn torrent-server [server-url & {:keys [creds version api-path] :or {version :v1}}]
  ;; creds are attached to record as metadata
  (TorrentServer. server-url version (or api-path ({:v1 "/rundb/api/v1/"} version))
                  {:creds creds} nil))


(def data-readers
  {'ion_torrent_api.schema.TorrentServer ion-torrent-api.schema/map->TorrentServer
   'ion_torrent_api.schema.Experiment    ion-torrent-api.schema/map->Experiment
   'ion_torrent_api.schema.Result        ion-torrent-api.schema/map->Result
   'ion_torrent_api.schema.PluginResult  ion-torrent-api.schema/map->PluginResult
   'ion_torrent_api.core.TorrentServer   ion-torrent-api.schema/map->TorrentServer
   'ion_torrent_api.core.Experiment      ion-torrent-api.schema/map->Experiment
   'ion_torrent_api.core.Result          ion-torrent-api.schema/map->Result
   'ion_torrent_api.core.PluginResult    ion-torrent-api.schema/map->PluginResult})
