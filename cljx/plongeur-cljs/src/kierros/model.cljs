(ns kierros.model
  (:require [cljs.core.async :as a :refer [chan pipe]]
            [kierros.util :refer [scan]]
            [kierros.async :refer [chain]]))

(defn scan-to-states
  "Accepts a channel with the initial state, a map of intent channels and a map
  of intent handler functions. Intent channels contain a value that contains the
  intent handler parameter. Intent channels are piped through new channels with
  a transducer that transforms the intent parameters into a functions of shape

  `(param -> state) -> state`.

  These function-channels are merged and scanned over, starting from an initial state,
  to produce a channel of application states. Returns the channel of application states."
  [init-state-chan intent-chans intent-handlers]
  (let [amend-fn-chan (->> intent-chans
                           (map (fn [[key ch]]
                                  (when-let [intent-handler (key intent-handlers)]
                                    (->> #(partial intent-handler %) ; fn
                                         (map)                       ; xf
                                         (chan 10)                   ; ch
                                         (pipe ch)))))               ; piped
                           (remove nil?) ; only channel with handler
                           (a/merge))
        states-chan   (->> (fn [state f] (f state)) ; fn
                           (scan)                   ; xf
                           (chan 10)                ; ch
                           (pipe (chain [init-state-chan amend-fn-chan])))]
    states-chan))