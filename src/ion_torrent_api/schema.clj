(ns ion-torrent-api.schema
  (:require [clojure.core :as core]
            [clojure.java.io :as io]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.instant :as inst]
            [schema.core :as s]
            [schema.macros :as sm]
            [slingshot.slingshot :refer (try+ throw+)]))

(sm/defrecord TorrentServer
    [server-url :- s/Str
     version :- s/Keyword
     api-path :- s/Str]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord PluginResult
    [type :- (s/maybe s/Keyword)
     torrent-server :- TorrentServer
     id :- s/Int
     uri :- s/Str
     result-uri :- s/Str
     result-name :- s/Str
     state :- s/Str
     path :- s/Str
     report-link :- s/Str
     name :- s/Str
     version :- s/Str
     versioned-name :- s/Str
     library-type :- (s/maybe s/Str)
     config-desc :- (s/maybe s/Str)
     target-name :- (s/maybe s/Str)
     target-bed :- (s/maybe s/Str)
     experiment-name :- (s/maybe s/Str)
     trimmed-reads? :- (s/maybe s/Bool)
     barcode-result-map :- (s/maybe {s/Str {s/Str s/Any}})
     barcoded? :- s/Bool
     start-time :- s/Inst
     end-time :- s/Inst
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord Result
    [torrent-server :- TorrentServer
     id :- s/Int
     name :- s/Str
     uri :- s/Str
     experiment-uri :- s/Str
     status :- s/Str
     plugin-result-uri-set :- [s/Str]
     plugin-state-map :- {s/Str s/Str}
     analysis-version :- s/Str
     report-status :- s/Str
     plugin-store-map :- {s/Str {s/Str s/Any}}
     bam-link :- s/Str
     fastq-link :- s/Str
     report-link :- s/Str
     filesystem-path :- s/Str
     reference :- s/Str
     lib-metrics-uri-set :- [s/Str]
     tf-metrics-uri-set :- [s/Str]
     analysis-metrics-uri-set :- [s/Str]
     quality-metrics-uri-set :- [s/Str]
     timestamp :- s/Inst
     thumbnail? :- s/Bool
     plugin-result-set :- (s/maybe #{PluginResult})
     lib-metrics-set :- (s/maybe #{{s/Any s/Any}})
     tf-metrics-set :- (s/maybe #{{s/Any s/Any}})
     analysis-metrics-set :- (s/maybe #{{s/Any s/Any}})
     quality-metrics-set :- (s/maybe #{{s/Any s/Any}})
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))

(sm/defrecord Experiment
    [torrent-server :- TorrentServer
     id :- s/Int
     name :- s/Str
     pgm-name :- s/Str
     display-name :- s/Str
     uri :- s/Str
     run-type :- s/Str
     chip-type :- s/Str
     result-uri-set :- [s/Str]
     dir :- s/Str
     status :- s/Str
     ftp-status :- s/Str
     sample-map :- {s/Str {s/Any s/Any}}
     barcode-sample-map :- {s/Str s/Str}
     date :- s/Inst
     latest-result-date :- s/Inst
     latest-result :- (s/maybe Result)
     raw-map :- {s/Any s/Any}]
  Object
  (toString [this] (pr-str this)))


(def data-readers
  {'ion_torrent_api.schema.TorrentServer ion-torrent-api.schema/map->TorrentServer
   'ion_torrent_api.schema.Experiment    ion-torrent-api.schema/map->Experiment
   'ion_torrent_api.schema.Result        ion-torrent-api.schema/map->Result
   'ion_torrent_api.schema.PluginResult  ion-torrent-api.schema/map->PluginResult
   'ion_torrent_api.core.TorrentServer   ion-torrent-api.schema/map->TorrentServer
   'ion_torrent_api.core.Experiment      ion-torrent-api.schema/map->Experiment
   'ion_torrent_api.core.Result          ion-torrent-api.schema/map->Result
   'ion_torrent_api.core.PluginResult    ion-torrent-api.schema/map->PluginResult})
