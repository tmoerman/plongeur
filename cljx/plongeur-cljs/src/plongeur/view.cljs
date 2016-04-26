(ns plongeur.view
  (:require [cljs.core.async :as a :refer [>! chan pipe]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [plongeur.model :as m]
            [plongeur.sigma-driver :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def node-1 {:id "n1"
             :label "hello"
             :x 10
             :y 10
             :size 1
             :color "#FF0"})

(defn graph-id [id] (str "graph-" id))

(defcomponent Sigma
  :keyfn      (fn [[id _]] id)
  :on-mount   (fn [_ [id _] {:keys [sigma-ctrl]}] (go (>! sigma-ctrl (s/ctrl-update id))))
  :on-unmount (fn [_ [id _] {:keys [sigma-ctrl]}] (go (>! sigma-ctrl (s/ctrl-remove id))))
  [[id props] {:keys [drop-graph] :as intent-chans}]
  (html [:section {:class "graph-section"}
         [:button {:on-click #(go (>! drop-graph id))} "delete " id]
         [:div {:id    (graph-id id)
                :class "graph"}]]))

(defcomponent Root
  [state {:keys [add-graph debug] :as intent-chans}]
  (html [:div {:id "plongeur-main"}
         [:h1 {} "Bonjour, ici Plongeur"]
         [:button {:on-click (fn [_] (go (>! debug :click)))} "print state"]
         [:button {:on-click (fn [_] (go (>! add-graph :click)))} "add graph"]
         (for [graph-state (m/graphs state)]
           (Sigma graph-state intent-chans))]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))