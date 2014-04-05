# ion-torrent-api

A Clojure library using the Ion Torrent Server API (http://updates.iontorrent.com/ts-sdk-docs/index.html).

## Usage

### TorrentServerAPI Protocol ###

Methods to get Experiment, Result, and PluginResult records, as well as file
object URIs and resources such as BAM, VCF, and coverage files.

TorrentServer record maintains connection details, used to query Experiment, Result, PluginResult
records and other resources.

```clj
(require '[ion-torrent-api.core :as ion])

(def ts (ion/torrent-server "http://my-torent-server.com" ["user" "pass"]))
ts
; #ion_torrent_api.core.TorrentServer{:server-url "http://my-torent-server.com", :creds nil, :api-path "/rundb/api/v1/"}

(def exp (ion/experiment ts 50))
exp
; #ion_torrent_api.core.Experiment{...}

(ion/experiment-name ts "R_2014_03_01_..._015")
; #ion_torrent_api.core.Experiment{...}

(ion/barcode-set exp)
; #{"IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"}

(def res (ion/result ts 109))
res
; #ion_torrent_api.core.Result{...}

(ion/result exp)
; #ion_torrent_api.core.Result{...}

(def pr (ion/plugin-result ts 209))
pr
; #ion_torrent_api.core.PluginResult{...}

(ion/plugin-result res)
; [#ion_torrent_api.core.PluginResult{...}, ...]

(ion/barcode-set pr)
; #{"IonXpressRNA_001" "IonXpressRNA_002" "IonXpressRNA_003" "IonXpressRNA_004" "IonXpressRNA_005"}
                
```

## License

Copyright © 2013 Insilico Informatics Pty Ltd.

Distributed under the Eclipse Public License, the same as Clojure.
