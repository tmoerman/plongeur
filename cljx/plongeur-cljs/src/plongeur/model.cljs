(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [com.rpl.specter :as sp :refer [select select-one transform keypath ALL VAL FIRST]]
            [kierros.model :refer [scan-to-states]]
            [plongeur.config :as c]))

;; State queries
;; The developer should never navigate the state map in another namespace.
;; All navigation should be done by means of the state queries specified here.

(defn plots [state] (select-one [:plots] state))

(defn plot-ids [state] (select [:plots ALL FIRST] state))

(defn plot-count [state] (-> state plots count))

(defn seq-val [state] (select-one [:seq] state))

(defn defaults [key state] (select-one [:config :defaults key] state))

(defn sigma-settings [state] (select-one [:config :sigma :settings] state))


;; Intent handler functions have signature [param state], where param is a data structure that captures
;; all necessary data for handling the intent, and state is the entire application state.

(defn handle-web-response
  "Handle a websocket response."
  [response state]
  #_(prn (str "received websocket response: " response))

  ;;
  ;; TODO merge the received data into the state's plots map.
  ;; OR: perhaps allow the Sigma graphs to periodically (while running the force algorithm)

  state)

(defn handle-dom-event
  "Handle a DOM event."
  [event state]
  #_(prn (str "received DOM event: " event))
  state)

(defn add-plot [plot-type state]
  "Add an empty graph visualization."

  (let [plot-id    (seq-val state)
        plot-entry {plot-type (defaults plot-type state)
                    :data     nil}]
    (->> state
         (transform [:seq] inc)
         (transform [:plots] #(assoc % plot-id plot-entry)))))

(defn drop-plot [id state]
  "Drop a graph to the app state."
  (transform [:plots] #(dissoc % id) state))


(defn prn-state [_ state]
  (prn state)

  state)

;; Model machinery

(def intent-handlers
  {:handle-web-response handle-web-response
   :handle-dom-event    handle-dom-event
   :add-plot            add-plot
   :drop-plot           drop-plot
   :debug               prn-state})

;; The model is a channel of application states.

(def default-state
  "Returns a new initial application state."
  {:seq    2                 ;; database sequence-like
   :plots  {1 {}}            ;; contains the visualization properties
   :config c/default-config  ;; the default config
   })

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))