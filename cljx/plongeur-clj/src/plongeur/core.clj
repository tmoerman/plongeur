(ns plongeur.core
  (:require [clojure.core.async :as a :refer [<! >! chan]]
            [kierros.core :as cycle]
            [kierros.sente-server-driver :as web]
            [plongeur.spark-driver :as spark]
            [plongeur.util :as u]))

(defn plongeur-server-main
  "Main function cfr. Cycle.js architecture."
  [{client-request-chan  :WEB
    repl-msg-chan        :REPL
    spark-ctx-chan       :SPARK}]

  (let [push-response-chan (chan 10)
        spark-cfg-chan     (chan 10)

        ]

    (a/pipe repl-msg-chan (->> (map u/echo) (chan 10)))

    {:WEB   push-response-chan
     :REPL  repl-msg-chan ;;TODO is this correct?
     :SPARK spark-cfg-chan}))

(defn launch []
  (cycle/run plongeur-server-main
             {:SPARK (spark/make-spark-context-driver)
              :WEB   (web/make-sente-server-driver {:sente    {} ;; TODO add :user-id-fn (fn [req] user-id-str)
                                                    :http-kit {:port 3000}})
              :REPL  (fn [_] (chan 10)) ;; a REPL message channel
              }))

(defn shutdown [system]
  (when-let [stop-fn (some-> system :SHUTDOWN)] (stop-fn)))