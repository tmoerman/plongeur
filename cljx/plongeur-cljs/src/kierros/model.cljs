(ns kierros.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.util :refer [scan]]
            [kierros.async :refer [chain]]))

(defn scan-to-states
  ; TODO generic enough to factored out to Kierros namespace.
  "Returns a stream of application states, represented as a core.async channel."
  [init-state intent-chans intent-handlers]
  (let [buf-or-n      10
        amend-fn-chan (->> intent-chans
                           (map (fn [[key ch]]
                                  (when-let [intent-handler (key intent-handlers)]
                                    (->> #(partial intent-handler %) ; fn
                                         (map)                       ; xf
                                         (chan buf-or-n)             ; ch
                                         (pipe ch)))))               ; piped
                           (remove nil?) ; only channel with handler
                           (a/merge))
        initial-chan  (to-chan [init-state])
        states-chan   (->> (fn [state f] (f state)) ; fn
                           (scan)                   ; xf
                           (chan buf-or-n)          ; ch
                           (pipe (chain [initial-chan amend-fn-chan])))]
    states-chan))
