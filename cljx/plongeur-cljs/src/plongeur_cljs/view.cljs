(ns plongeur-cljs.view
  (:require [cljs.core.async :as a :refer [>! chan pipe]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :as s :refer-macros [html]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defcomponent Root
  [state {:keys [toggle-all] :as intent-chans}]
  (html [:div {}
         [:section {:id "main"} "Bonjour, ici Plongeur."]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))