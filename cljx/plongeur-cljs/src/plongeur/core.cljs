(ns plongeur.core
  (:require [cljs.core.async :as a :refer [mult tap chan pipe sliding-buffer]]
            [kierros.core :as cycle]
            [plongeur.intent :as i]
            [plongeur.model  :as m]
            [plongeur.view   :as v]
            [plongeur.sigma-driver :as sig]
            [kierros.quiescent-dom-driver   :as dom]
            [kierros.sente-websocket-driver :as ws]
            [kierros.local-storage-driver   :as st]))

(enable-console-print!)

(defn on-js-reload [] (print "on-js-reload"))

(defn plongeur-main
  "Cycle main."
  [{dom-event-chan       :DOM
    sigma-event-chan     :SIGMA
    saved-state-chan     :STORAGE
    websocket-event-chan :WEB
    }]
  (let [sigma-chan         (chan 10)
        intent-chans       (-> (i/intents)
                               (assoc :sigma-ctrl sigma-chan))

        states-chan        (m/model saved-state-chan intent-chans)
        states-mult        (mult states-chan)

        pickle-states-chan (->> (sliding-buffer 1) (chan) (tap states-mult))

        view-states-chan   (->> (chan 10) (tap states-mult))

        request-chan       (chan 10)

        views-chan         (v/view view-states-chan intent-chans)]
    {:DOM     views-chan
     :SIGMA   sigma-chan
     :STORAGE pickle-states-chan
     :WEB     request-chan}))

(cycle/run
  plongeur-main
  {:DOM     (dom/make-dom-driver "plongeur-app")
   :SIGMA   (sig/make-sigma-driver)
   :STORAGE (st/make-storage-driver "plongeur" m/default-state)
   ;:WEB    (ws/make-websocket-driver "/chsk")
   })