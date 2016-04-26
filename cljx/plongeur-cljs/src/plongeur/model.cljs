(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.model :refer [scan-to-states]]
            [com.rpl.specter :as s :refer [select transform ALL FIRST]]))

; state map
;      {:seq
;       :graphs {}
;       }

;; Nothing else but the functions in this namespace touch (or query) the app state directly.

;; queries

(defn graphs [state] (:graphs state))

(defn graph-ids [state] (select [:graphs ALL FIRST] state))

(defn seq-val [state] (:seq state))

;; updates

(defn add-graph [_ state]
  "Add a graph"
  (let [graph-id    (seq-val state)
        graph-props {}]
    (prn "adding graph " graph-id)
    (->> state
         (transform [:seq] inc)
         (transform [:graphs] #(assoc % graph-id graph-props)))))

(defn drop-graph [id state]
  "Drop a graph to the app state."
  (transform [:graphs] #(dissoc % id) state))

;; model machinery

(def intent-handlers
  {:network-calculated (fn [_ state] state)
   :nodes-selected     (fn [_ state] state)
   :add-graph          add-graph
   :drop-graph         drop-graph})

(def default-state
  "Returns a new initial application state."
  {:seq    2
   :graphs {1 {}}})

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))