(ns kierros.async
  (:require [cljs.core.async :as a :refer [<! >! close! chan]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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
          (let [val (<! ch)]
            (if (nil? val) ;; current channel exhausted
              (recur rest)
              (do
                (>! out val)
                (recur chs))))
          (close! out))) ;; close out channel when all input channels are consumed
     out)))
