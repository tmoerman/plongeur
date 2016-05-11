(ns kierros.sente-server-driver
  "Sente websocket server driver.
  See https://github.com/ptaoussanis/sente"
  (:require [clojure.core.async :refer [<! >! chan close! go go-loop]]
            [compojure.core :refer [routes GET POST]]
            [taoensso.sente :as s]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :refer [warn]]))

(defn make-sente-server-driver
  "Accepts an optional options map.
  Returns a websocket server driver powered by Sente."
  [options]
  (fn [push-chan]
    (let [{:keys [ch-recv send-fn]} (s/make-channel-socket-server! sente-web-server-adapter options)
          client-request-chan       (chan 10)
          router-shutdown-fn        (s/start-chsk-router! ch-recv #(go (>! client-request-chan %)))]
      (go-loop []
               (if-let [push->client (<! push-chan)]
                 (do (send-fn push->client)
                     (recur))
                 (do (router-shutdown-fn)
                     (warn "sente server driver stopped"))))
      client-request-chan)))