(ns plongeur-cljs.view
  (:require [cljs.core.async :as a :refer [>! chan pipe]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :as s :refer-macros [html]]
            [foreign.sigma])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(let [bla js/sigma]
  (println bla))

(defcomponent Sigma
  ;:on-mount (fn [node state intent-chans]
  ;            ;(let [s (js/)])
  ;            :bla
  ;            )
  [state intent-chans]
  (html [:div {:id "container"} "<<< sigma placeholder >>>"]))

(defcomponent Root
  [state {:keys [toggle-all] :as intent-chans}]
  (html [:div {:id "plongeur-main"}
         [:h1 {} "Bonjour, ici Plongeur"]
         (Sigma state intent-chans)]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))