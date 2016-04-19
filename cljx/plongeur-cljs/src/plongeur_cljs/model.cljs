(ns plongeur-cljs.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.model :refer [scan-to-states]]))

(def intent-handlers
  {:network-calculated (fn [_ state] state)
   :nodes-selected     (fn [_ state] state)
   :bus                (fn [msg state]
                         (let [nr-clicks (:clicks state)]
                           (-> state
                               (update-in [:clicks] inc)
                               (update-in [:graphs] (fn [graphs] (conj graphs {:id nr-clicks}))))))})

(def default-state
  "Returns a new initial application state."
  {:clicks 2
   :graphs [{:id 1}]})

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))