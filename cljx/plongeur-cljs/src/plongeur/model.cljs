(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.model :refer [scan-to-states]]))

(defn add-graph
  [_ state]
  (let [next-id (:seq state)]
    (-> state
        (update-in [:seq] inc)
        (update-in [:graphs] (fn [graphs] (conj graphs {:id next-id}))))))

(defn drop-graph
  [id state]
  (update-in state [:graphs] (fn [graphs] (->> graphs
                                               (remove #(= id (:id %)))))))

(def intent-handlers
  {:network-calculated (fn [_ state] state)
   :nodes-selected     (fn [_ state] state)
   :add-graph          add-graph
   :drop-graph         drop-graph})

(def default-state
  "Returns a new initial application state."
  {:seq    2
   :graphs [{:id 1}]})

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))