(ns plongeur.core
  (:require [cljs.core.async :as a :refer [mult tap chan pipe sliding-buffer]]
            [kierros.core :as cycle]
            [plongeur.intent :as i]
            [plongeur.model  :as m]
            [plongeur.view   :as v]
            [plongeur.sigma-driver :as sig]
            [kierros.quiescent-dom-driver :as dom]
            [kierros.sente-client-driver :as ws]
            [kierros.local-storage-driver :as st]
            [cljsjs.material]))

(enable-console-print!)

(v/upgrade-mdl-components)

(defn plongeur-client-main
  "Main function cfr. Cycle.js architecture."
  [{dom-event-chan       :DOM
    sigma-event-chan     :SIGMA
    saved-state-chan     :STORAGE
    server-response-chan :WEB}]
  (let [sigma-chan         (chan 10)

        intent-chans       (-> (i/intents)
                               (assoc :sigma-ctrl sigma-chan)) ;; a sign that sigma operation are intents as well...

        states-chan        (m/model saved-state-chan intent-chans)
        states-mult        (mult states-chan)

        pickle-states-chan (->> (map #(dissoc % :transient))
                                (chan 10)
                                (tap states-mult))

        view-states-chan   (->> (chan 10)
                                (tap states-mult))

        request-chan       (chan 10)

        views-chan         (v/view view-states-chan intent-chans)]
    {:DOM     views-chan
     :SIGMA   sigma-chan
     :STORAGE pickle-states-chan
     :WEB     request-chan}))

(defn launch-client []
  (cycle/run plongeur-client-main
             {:DOM     (dom/make-dom-driver "plongeur-app")
              ;; :WEB     (ws/make-sente-client-driver "/chsk" {:host "localhost:8090"})
              :SIGMA   (sig/make-sigma-driver)
              :STORAGE (st/make-storage-driver "plongeur" m/default-state)}))