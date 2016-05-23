(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.model :refer [scan-to-states]]
            [com.rpl.specter :as s :refer [select transform ALL FIRST]]))

;; Queries on the application state. Refrain from implementing queries in another namespace
;; or in inline functions elsewhere.

(defn graphs [state] (:graphs state))

(defn graph-ids [state] (select [:graphs ALL FIRST] state))

(defn seq-val [state] (:seq state))


;; Intent handler functions have signature [param state], where param is a data structure that captures
;; all necessary data for handling the intent, and state is the entire application state.

(defn add-graph [_ state]
  "Add a graph"
  (let [graph-id    (seq-val state)
        graph-props {}]
    (->> state
         (transform [:seq] inc)
         (transform [:graphs] #(assoc % graph-id graph-props)))))

(defn drop-graph [id state]
  "Drop a graph to the app state."
  (transform [:graphs] #(dissoc % id) state))

(def intent-handlers
  {;:network-calculated (fn [_ state] state)
   ;:nodes-selected     (fn [_ state] state)
   :add-graph          add-graph
   :drop-graph         drop-graph
   :debug              (fn [_ state] (prn state) state)})


;; The model is a channel of application states.

(def default-state
  "Returns a new initial application state."
  {:seq    2
   :graphs {1 {}}})

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))