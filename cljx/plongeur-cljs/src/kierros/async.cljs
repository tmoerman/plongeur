(ns kierros.async
  (:require [cljs.core.async :as a :refer [<! >! chan close! timeout alts!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn chain
  "Core.async equivalent of Rx.concat(observables).
  Takes a collection of source channels and returns a channel which contains all
  values taken from them, respecting the order of channels. The returned channel
  will be unbuffered by default, or a buf-or-n can be supplied. The channel will
  close after all the source channels have closed."
  ([chs] (chain chs nil))
  ([chs buf-or-n]
   (let [out (chan buf-or-n)]
     (go-loop [[ch & rest] chs]
        (if ch
          (if-let [v (<! ch)]
            (do (>! out v)
                (recur chs))
            (recur rest))
          (close! out))) ;; close out channel when all input channels are consumed
     out)))

(defn debounce
  "!!! UNTESTED !!!
  See: https://gist.github.com/scttnlsn/9744501"
  [in ms]
  (let [out (chan)]
    (go-loop [last-val nil]
      (let [val          (if (nil? last-val) (<! in) last-val)
            timer        (timeout ms)
            [new-val ch] (alts! [in timer])]
        (condp = ch
          timer (do (>! out val) (recur nil))
          in    (recur new-val))))
    out))
