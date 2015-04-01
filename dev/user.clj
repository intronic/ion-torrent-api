(ns user
  (:require [clojure
             [set :as set]
             [string :as str]
             [zip :as zip]
             [pprint :as p]
             [edn :as edn]]
            [clojure.core.reducers :as r]
            [clojure.java.io :as io]
            [clojure.tools.trace :as trace]
            [clojure.algo.generic.functor :refer (fmap)]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [slingshot.slingshot :refer (try+ throw+)]
            [clj-time [core :as tm] [coerce :as tmc]]
            [ion-torrent-api.core :as ionc]
            [expectations :refer (run-tests run-all-tests)])
  (:import [java.io BufferedReader StringReader InputStreamReader BufferedInputStream FilterInputStream]))

(defn set-repl-print-bindings []
  (alter-var-root #'*print-length* (constantly 30))
  (alter-var-root #'*print-level* (constantly 10)))
