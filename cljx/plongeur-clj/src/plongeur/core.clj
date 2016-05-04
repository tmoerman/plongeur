(ns plongeur.core
  (:require [clojure.core.async :as a :refer [<! >! chan]]
            [kierros.core :as cycle]
            [kierros.sente-server-driver :as ws])
  )

(defn plongeur-server-main
  "Main function cfr. Cycle.js architecture."
  [{client-request-chan :WEB}]
  (let [client-push (chan 10)

        ]
    {:WEB client-push}))

(defn launch-server []
  (cycle/run plongeur-server-main
             {:WEB (ws/make-sente-server-driver {})}))