(ns plongeur.view
  (:require [cljs.core.async :as a :refer [>! chan pipe]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :as s :refer-macros [html]]
            [foreign.sigma])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def node-1 {:id "n1"
             :label "hello"
             :x 10
             :y 10
             :size 1
             :color "#FF0"})

(defn graph-id [graph-state] (str "graph-" (:id graph-state)))

(defcomponent Sigma
  :keyfn (fn [graph-state] (:id graph-state))

  :on-mount (fn [node graph-state intent-chans]
              (let [s  (js/sigma. (graph-id graph-state))
                    g  (.-graph s)]
                (.log js/console s)
                (.addNode g (clj->js node-1))
                (.refresh s)))

  :on-unmount (fn [node graph-state intent-chans]
                (.log js/console node))

  [graph-state {:keys [drop-graph] :as intent-chans}]
  (html [:section {:class "graph-section"}
         [:button {:on-click #(go (>! drop-graph (:id graph-state)))} "delete " (:id graph-state)]
         [:div {:id    (graph-id graph-state)
                :class "graph"}]]))

(defcomponent Root
  [state {:keys [add-graph] :as intent-chans}]
  (html [:div {:id "plongeur-main"}
         [:h1 {} "Bonjour, ici Plongeur"]
         (for [graph-state (:graphs state)]
           (Sigma graph-state intent-chans))
         [:button {:on-click (fn [_] (go (>! add-graph :click)))} "add graph"]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))

#_(def Sigma-2
    (let [local-state (atom nil)] ;; this is wrong
      (q/component
        (fn [graph-state {:keys [drop-graph] :as intent-chans}]
          (let [[s id] @local-state]
            (html [:section {:class "graph-section"}
                   [:button {:on-click #(go (>! drop-graph (:id graph-state)))} "delete " (:id graph-state)]
                   [:div {:id    (graph-id graph-state)
                          :class "graph"}]])))

        {:keyfn (fn [graph-state] (:id graph-state))

         :on-mount (fn [node graph-state intent-chans]
                     (let [s  (js/sigma. (graph-id graph-state))
                           g  (.-graph s)
                           id (:id graph-state)]
                       (reset! local-state [s id])
                       (.addNode g (clj->js node-1))
                       (.refresh s)
                       ))

         :on-unmount (fn [node graph-state intent-chans]
                       (let [[s id] @local-state]
                         (println "killing: " id)
                         (.kill s))
                       )})))