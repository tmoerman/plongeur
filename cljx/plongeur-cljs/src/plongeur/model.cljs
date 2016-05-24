(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [com.rpl.specter :as sp :refer [select select-one transform keypath ALL VAL FIRST]]
            [kierros.model :refer [scan-to-states]]
            [plongeur.config :as c]
            ;[plongeur.sigma  :as s]

            ))

;; State queries
;; The developer should never navigate the state map in another namespace.
;; All navigation should be done by means of the state queries specified here.

(defn graphs [state] (select-one [:graphs] state))

(defn graph-ids [state] (select [:graphs ALL FIRST] state))

(defn graph-count [state] (-> state graphs count))

(defn seq-val [state] (select-one [:seq] state))

(defn screen-defaults [type state] (select-one [:config :defaults type] state))

(defn sigma-settings [state] (select-one [:config :sigma :settings] state))

(defn sigma-instance [id state] (get-in state [:transient id]))


;; Event handlers

(defn handle-response
  "Handle a websocket response."
  [response state]
  #_(prn (str "received websocket response: " response))

  state)

(defn handle-dom-event
  "Handle a DOM event."
  [event state]
  #_(prn (str "received DOM event: " event))

  state)


;; State update

(defn add-screen [type] ;TODO generalization of graphs
  )

(defn add-graph [_ state]
  "Add a graph visualization."
  (let [graph-id    (seq-val state)
        graph-props (screen-defaults :tda state)]
    (->> state
         (transform [:seq] inc)
         (transform [:graphs] #(assoc % graph-id graph-props)))))

(defn drop-graph [id state]
  "Drop a graph to the app state."
  (transform [:graphs] #(dissoc % id) state))


;; Other intents

(defn prn-state [_ state] (prn state) state)


;; Model machinery

(def intent-handlers
  {:handle-response  handle-response
   :handle-dom-event handle-dom-event

   :add-graph        add-graph  ;; TODO generalize to add-viz
   :drop-graph       drop-graph

   :debug            prn-state})

(def default-state
  "Returns a new initial application state."
  {:seq       2                 ;; database sequence-like
   :graphs    {1 {}}            ;; contains the visualization properties TODO generalise
   :config    c/default-config  ;; the default config
   })

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))