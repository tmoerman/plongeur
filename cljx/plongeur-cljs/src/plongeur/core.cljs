(ns plongeur.core
  (:require [cljs.core.async :as a :refer [mult tap chan pipe sliding-buffer]]
            [kierros.core :as cycle]
            [plongeur.intent :as i]
            [plongeur.model  :as m]
            [plongeur.view   :as v]
            [kierros.quiescent-dom-driver   :as dom]
            [kierros.sente-websocket-driver :as ws]
            [kierros.local-storage-driver   :as st]))

(enable-console-print!)

(defn on-js-reload [] (print "on-js-reload"))

(defn plongeur-main
  "Cycle main."
  [{dom-event-chan       :DOM
    ;websocket-event-chan :WEB
    saved-state-chan     :STORAGE}]
  (let [intent-chans (i/intents)
        states-chan  (m/model saved-state-chan intent-chans)
        states-mult  (mult states-chan)
        view-states-chan   (->> (chan) (tap states-mult))
        pickle-states-chan (->> (sliding-buffer 1) (chan) (tap states-mult))
        views-chan   (v/view view-states-chan intent-chans)
        request-chan (chan)]
    {:DOM     views-chan
     ;:WEB     request-chan
     :STORAGE pickle-states-chan}))

(cycle/run
  plongeur-main
  {:DOM     (dom/make-dom-driver "plongeur-app")
   ;:WEB     (ws/make-websocket-driver "/chsk")
   :STORAGE (st/make-storage-driver "plongeur" m/default-state)})