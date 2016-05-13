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
  "Arity 1: accepts a channel. For every value taken from the channel, resets an atom
            to the new value. Returns the atom.
   Arity 2: accepts an atom and a channel. Same behaviour. Returns the atom."
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
  "Accepts channels. Returns a channel that emits vectors of the latest value of
  the input channels. The output channel closes when any of the input channels closes.
  See http://rxmarbles.com/#combineLatest."
  ([chs] (combine-latest (chan) chs))
  ([out chs]
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
               (>! out new-state))
             (recur new-state))
           (close! out)))) ; close out when one of the input channels is closed
     out)))