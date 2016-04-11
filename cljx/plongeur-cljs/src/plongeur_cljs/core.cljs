(ns plongeur-cljs.core
  (:require [kierros.core :as cycle]
            [plongeur-cljs.intent :as i]
            [plongeur-cljs.model  :as m]
            [plongeur-cljs.view   :as v]
            [kierros.quiescent-dom-driver   :as dom]
            [kierros.sente-websocket-driver :as ws]
            [kierros.local-storage-driver   :as st]))

(enable-console-print!)

(defn on-js-reload [] (print "on-js-reload"))

(defn plongeur-main
  "Cycle main."
  [& args] ;; TODO destructure to obtain source channels
  (let [init-state   (m/init-state)
        intent-chans (i/intents)
        states-chan  (m/model init-state intent-chans)
        views-chan   (v/view intent-chans states-chan)
        request-chan (:server-requests intent-chans) ;; TODO temporary
        pickled-chan nil] ;; TODO serialize the states to be submitted to the storage driver
    {:DOM     views-chan
     :WEB     request-chan
     :STORAGE pickled-chan}))

(cycle/run
  plongeur-main
  {:DOM     (dom/make-dom-driver "plongeur-app")
   :WEB     (ws/make-websocket-driver "/chsk")
   :STORAGE st/storage-driver})