(ns plongeur.core
  (:require [clojure.core.async :as a :refer [<! >! chan]]
            [kierros.core :as cycle]
            [kierros.sente-server-driver :as ws]
            [plongeur.util :as u])
  )

(defn plongeur-server-main
  "Main function cfr. Cycle.js architecture."
  [{client-request-chan :WEB
    repl-msg-chan       :REPL}]
  (let [client-push (chan 10)]

    (a/pipe repl-msg-chan (->> (map u/echo) (chan 10)))

    {:WEB  client-push
     :REPL repl-msg-chan}))

(defn launch-server []
  (cycle/run plongeur-server-main
             {:WEB  (ws/make-sente-server-driver {:sente    {}
                                                  :http-kit {:port 3000}})
              :REPL (fn [_] (chan 10)) ;; a REPL message channel
              }))