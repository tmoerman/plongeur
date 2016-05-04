(ns kierros.model
  (:require [cljs.core.async :as a :refer [chan pipe]]
            [kierros.util :refer [scan]]
            [kierros.async :refer [chain]]))

(defn scan-to-states
  "Accepts a channel with the initial state, possibly loaded from storage
   and a channel of intents. Returns a channel of application states."
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
