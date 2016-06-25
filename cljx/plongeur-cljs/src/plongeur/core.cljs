(ns plongeur.core
  (:require [cljs.core.async :as a :refer [<! >! timeout mult tap chan pipe sliding-buffer]]
            [kierros.core :as cycle]
            [kierros.async :refer [drain!]]
            [kierros.history :as h]
            [plongeur.intent :as i]
            [plongeur.model  :as m]
            [plongeur.view   :as v]
            [plongeur.route  :as r]
            [kierros.quiescent-dom-driver :as dom]
            [kierros.sente-client-driver :as ws]
            [kierros.local-storage-driver :as st]
            [cljsjs.material])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(v/upgrade-mdl-components)

(defonce history-multiples (h/init-history))

(defn plongeur-client-main
  "Main function inspired by the Cycle.js architecture."
  [{dom-event-chan    :DOM
    saved-state-chan  :STORAGE
    web-response-chan :WEB}]

  (let [intent-chans           (i/intents)

        navigate-chan          (->> (map r/view->token)
                                    (chan 10))

        history-event-chan     (->> (map #(r/handle-url-change % intent-chans))
                                    (chan 10)
                                    (drain!))

        _ (h/connect-chans! history-multiples navigate-chan history-event-chan)
        _ (pipe web-response-chan (:handle-web-response intent-chans))
        _ (pipe dom-event-chan    (:handle-dom-event    intent-chans))

        states-chan            (m/model saved-state-chan intent-chans)
        states-mult            (mult states-chan)

        pickle-states-chan     (->> (comp
                                      (map #(dissoc % :transient)))
                                    (chan 10)
                                    (tap states-mult))

        view-states-chan       (->> (chan 10)
                                    (tap states-mult))

        post-request-chan      (chan 10)

        cmd-chans              (assoc intent-chans
                                 :navigate     navigate-chan
                                 :post-request post-request-chan)

        views-chan             (v/view view-states-chan cmd-chans)

        ;; _ (go (>! navigate-chan :view/browse-scenes))

        ;; TODO periodically post a req to the server to verify whether the session is still available.
        ]

    {:DOM      views-chan
     ;:STORAGE  (chan (a/sliding-buffer 10))
     :STORAGE  pickle-states-chan
     :WEB      post-request-chan}))


(defn launch-client []
  (cycle/run plongeur-client-main
             {:DOM     (dom/make-dom-driver "plongeur-app")
              :WEB     (ws/make-sente-client-driver {:path "/chsk"
                                                     :host "localhost:3000"}
                                                    :disable
                                                    )
              :STORAGE (st/make-storage-driver "plongeur" m/default-state)}))