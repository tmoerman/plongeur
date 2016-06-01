(ns plongeur.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [com.rpl.specter :as sp :refer [select select-one transform keypath ALL VAL FIRST]]
            [kierros.model :refer [scan-to-states]]
            [plongeur.config :as c]
            [plongeur.sigma :as s]))

;; State queries
;; The developer should never navigate the state map in another namespace.
;; All navigation should be done by means of the state queries specified here.

(defn seq-val [state] (select-one [:seq] state))

(defn plot       [state id] (select-one [:plots (keypath id)] state))
(defn plots      [state] (select-one [:plots] state))
(defn plot-ids   [state] (select [:plots ALL FIRST] state))
(defn plot-count [state] (-> state plots count))

(defn init-sigma-settings [state] (select-one [:config :sigma :settings] state))
(defn init-sigma-props    [state] (select-one [:config :sigma :props] state))

(defn force-layout-active? [plot-state] (select-one [:props :force-layout-active] plot-state))
(defn sync-interval-ms     [plot-state] (select-one [:props :sync-interval-ms] plot-state))
(defn sigma-settings       [plot-state] (select-one [:settings] plot-state))
(defn sigma-data           [plot-state] (select-one [:data] plot-state))

(defn lasso-tool-active?   [plot-state] (select-one [:props :lasso-tool-active] plot-state))


;; Intent handler functions have signature [param state], where param is a data structure that captures
;; all necessary data for handling the intent, and state is the entire application state.

(defn handle-web-response
  "Handle a websocket response."
  [response state]
  #_(prn (str "received websocket response: " response))

  ;; TODO merge the received data into the state's plots map.
  ;; OR: perhaps allow the Sigma graphs to periodically (while running the force algorithm)

  state)

(defn handle-dom-event
  "Handle a DOM event."
  [event state]
  #_(prn (str "received DOM event: " event))
  state)

(defn make-sigma-plot
  [state]
  {:type     :sigma
   :props    (init-sigma-props state)
   :settings (init-sigma-settings state)
   :data     (s/make-shape)})

(defn make-scatter-plot ;; TODO implement this
  [state]
  {:type     :scatter
   :props    {}
   :settings {}
   :data     {}})

(defn add-plot
  "Add a new plot."
  [plot-type state]
  (let [plot-id    (seq-val state)
        plot-entry (condp = plot-type
                     :sigma   (make-sigma-plot state)
                     :scatter (make-scatter-plot state))]
    (->> state
         (transform [:seq] inc)
         (transform [:plots]
                    (fn [plots] (assoc plots plot-id plot-entry))))))

(defn drop-plot
  "Remove a plot."
  [id state]
  (transform [:plots] #(dissoc % id) state))

(defn fill-plots [_ state] state)

(defn toggle-prop
  [id state prop-key]
  (->> state
       (transform [:plots (keypath id) :props prop-key]
                  not)))

(defn set-prop
  [id state prop-key prop-val]
  (->> state
       (transform [:plots (keypath id) :props prop-key]
                  (constantly prop-val))))

(defn set-force [[id active?] state] (set-prop id state :force-layout-active active?))

(defn toggle-force
  "Toggles the force layout for the plot with specified id."
  [id state]
  (toggle-prop id state :force-layout-active))

(defn set-lasso [[id active?] state] (set-prop id state :lasso-tool-active active?))

(defn toggle-lasso
  "Toggles the lasso tool for the plot with specified id. If by means of this function, the lasso will
  be activated, the force layout is deactivated."
  [id state]
  (->> state
       (transform [:plots (keypath id) :props]
                  (fn [plot-props]
                    (prn (str "lasso? "(lasso-tool-active? plot-props)))
                    (if (lasso-tool-active? plot-props)
                      (-> plot-props ; deactivating lasso.
                          (update :lasso-tool-active not))
                      (-> plot-props ; activating lasso stops the force layout.
                          (update :lasso-tool-active not)
                          (update :force-layout-active (constantly false))))))))

(defn merge-plot-data
  "Handles updates to the plot, effected by user interaction (dragging nodes) or by a
  force layout algorithm"
  [[id data] state]
  (->> state
       (transform [:plots (keypath id) :data]
                  (constantly data))))

(defn prn-state
  "Print the current application state to the console."
  [_ state] (prn state) state)

;; Model machinery

(def intent-handlers
  {:handle-web-response handle-web-response
   :handle-dom-event    handle-dom-event
   :add-plot            add-plot
   :drop-plot           drop-plot
   :fill-plots          fill-plots

   :toggle-force        toggle-force
   :set-force           set-force
   :toggle-lasso        toggle-lasso
   :set-lasso           set-lasso

   :merge-plot-data     merge-plot-data

   :debug               prn-state})

;; The model is a channel of application states.

(def default-state
  "Returns a new initial application state."
  {:seq    1                  ;; database sequence-like
   :plots  {}                 ;; contains the visualization properties
   :config c/default-config}) ;; the default config

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))