(ns kierros.async
  (:require [cljs.core.async :as a :refer [<! >! chan close! timeout alts!]])
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
          (if-let [v (<! ch)]
            (do (>! out v)
                (recur chs))
            (recur rest))
          (close! out))) ;; close out channel when all input channels are consumed
     out)))

(defn drain!
  "Drain the specified channel until it is closed. Returns the channel."
  [ch]
  (go-loop [] (when (<! ch) (recur)))
  ch)

(defn val-timeout
  "Returns a channel that returns a value after the specified timeout ms."
  ([ms] (val-timeout ms :-D))
  ([ms v]
   (let [out (chan)]
     (go
       (<! (timeout ms))
       (>! out v))
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
