(ns plongeur-cljs.view
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

(def Sigma-2
  (let [local-state (atom {:id (rand-int 1000)})]
    (q/component
      (fn [graph-state intent-chans]
        (html [:div {:id (str "graph-" (:id graph-state))
                     :class "graph"} "test"]))

      {:keyfn (fn [graph-state] (:id graph-state))
       
       :on-mount (fn [node graph-state intent-chans]
                   (let [s (js/sigma. (str "graph-" (:id graph-state)))
                         g (.-graph s)]

                     (.addNode g (clj->js node-1))
                     (.refresh s)))

       :on-unmount (fn [node graph-state intent-chans]

                     )})))

(defcomponent Root
  [state {:keys [bus] :as intent-chans}]
  (html [:div {:id "plongeur-main"}
         [:h1 {} "Bonjour, ici Plongeur"]
         (for [graph-state (:graphs state)]
           (Sigma-2 graph-state intent-chans))
         [:button {:on-click (fn [_] (go (>! bus :click)))} "add Click"]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))

;; obsolete code

#_(defcomponent Sigma
                :on-mount (fn [node graph-state intent-chans]
                            (let [s (js/sigma. "container")
                                  g (.-graph s)]
                              (println "on-mount")
                              ;; listen to graph events here? move this to render probably!, then we will have access
                              ;; clean up resources here? HOW?
                              (.addNode g (clj->js {:id "n1"
                                                    :label "hello"
                                                    :x 10
                                                    :y 10
                                                    :size 1
                                                    :color "#FF0"}))
                              (.refresh s)))
                [graph-state intent-chans]
                (prn "state: " graph-state)
                (html [:div {:id    "container"
                             :class "graph"} " test "]))

;)