(ns plongeur.core
  (:require [clojure.core.async :as a :refer [<! >! chan]]
            [kierros.core :as cycle]
            [kierros.sente-server-driver :as web]
            [plongeur.spark-driver :as spark]
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

(defn launch []
  (cycle/run plongeur-server-main
             {:SPARK (spark/make-spark-context-driver)
              :WEB   (web/make-sente-server-driver {:sente    {}
                                                    :http-kit {:port 3000}})
              :REPL  (fn [_] (chan 10)) ;; a REPL message channel
              }))

(defn shutdown [system]
  (when-let [stop-fn (some-> system :SHUTDOWN)] (stop-fn)))