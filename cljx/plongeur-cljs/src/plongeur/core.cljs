(ns plongeur.core
  (:require [cljs.core.async :as a :refer [mult tap chan pipe sliding-buffer]]
            [kierros.core :as cycle]
            [plongeur.intent :as i]
            [plongeur.model  :as m]
            [plongeur.view   :as v]
            [kierros.quiescent-dom-driver :as dom]
            [kierros.sente-client-driver :as ws]
            [kierros.local-storage-driver :as st]
            [cljsjs.material]))

(enable-console-print!)

(v/upgrade-mdl-components)

(defn plongeur-client-main
  "Main function cfr. Cycle.js architecture."
  [{dom-event-chan    :DOM
    saved-state-chan  :STORAGE
    web-response-chan :WEB}]
  (let [intent-chans           (i/intents)

        _ (pipe web-response-chan (:handle-response intent-chans))
        _ (pipe dom-event-chan    (:handle-dom-event intent-chans))

        states-chan            (m/model saved-state-chan intent-chans)
        states-mult            (mult states-chan)

        pickle-states-chan     (->> (chan 10)
                                    (tap states-mult))

        view-states-chan       (->> (chan 10)
                                    (tap states-mult))

        post-request-chan      (chan 10)

        cmd-chans              (assoc intent-chans
                                 :post-request post-request-chan
                                 :update-graph )

        views-chan             (v/view view-states-chan cmd-chans)]
    {:DOM     views-chan
     :STORAGE pickle-states-chan
     :WEB     post-request-chan}))

(defn launch-client []
  (cycle/run plongeur-client-main
             {:DOM     (dom/make-dom-driver "plongeur-app")
              :WEB     (ws/make-sente-client-driver {:path "/chsk"
                                                     :host "localhost:8090"} :dummy)
              :STORAGE (st/make-storage-driver "plongeur" m/default-state)}))