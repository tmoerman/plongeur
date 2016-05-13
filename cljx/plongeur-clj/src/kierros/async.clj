(ns kierros.async
  (:require [clojure.core.async :as a :refer [<! <!! >! >!! chan close! go-loop]]
            [taoensso.timbre :refer [debug info]]))

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

(defn pipe-to-atom
  "Accepts a channel and optionally the target atom. For every value taken from the
  channel, resets the atom to the new value. Returns the atom."
  ([ch] (pipe-to-atom (atom nil) ch))
  ([a ch]
   (go-loop []
     (if-let [v (<! ch)]
       (do
         (reset! a v)
         (recur))
       (reset! a nil)))
   a))

(defn combine-latest
  "Accepts a collection of channels, an optional selector function f and an optional output
  channel. Returns a channel with the latest values of the input values combined with the
  selector function. If no selector function is specified, a vector will be returned.
  The output channel only yields results when at least one value has been offered to each of
  the input channels. The output channel closes when any of the input channels closes.
  Inspired by http://rxmarbles.com/#combineLatest"
  ([chs]   (combine-latest (chan) vector chs))
  ([f chs] (combine-latest (chan) f chs))
  ([out f chs]
   (assert some? chs)
   (let [ch->idx (->> chs
                      (map-indexed (fn [i x] [x i]))
                      (into {}))
         nil-vec (-> (count chs) (repeat nil) (vec))]
     (go-loop [state nil-vec]
       (let [[v ch] (a/alts! chs)]
         (if (some? v)
           (let [idx       (ch->idx ch)
                 new-state (assoc state idx v)]
             (when (every? some? new-state) ; emit when a value is present for every index
               (>! out (apply f new-state)))
             (recur new-state))
           (close! out)))) ; close out when one of the input channels is closed
     out)))