(ns kierros.sente-client-driver
  "Sente websocket client driver.
  See https://github.com/ptaoussanis/sente"
  (:require [cljs.core.async :as a :refer [<! >! put! close! chan pipe]]
            [taoensso.sente :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn make-sente-client-driver
  "Accepts a path and options.
  Returns a websocket client driver powered by Sente."
  [{:keys [path host]} & options]
  (if (-> options set :disable)
    (fn [_]
      (prn "Sente client driver :disabled flag was set, creating dummy channel...")
      (chan 10))
    (fn [request-chan]
      (prn "Sente client driver initializing...")
      (let [{:keys [chsk ch-recv send-fn]} (s/make-channel-socket-client! path {:host host})
            server-response-chan (chan 10)
            router-shutdown-fn (s/start-chsk-router! ch-recv #(go (>! server-response-chan %)))]
        (go-loop []
                 (if-let [request (<! request-chan)]
                   (do (send-fn request) ;; send-fn signature: (fn [event & [?timeout-ms ?cb-fn]])
                       (recur))
                   (do (some-> chsk s/chsk-disconnect!)
                       (router-shutdown-fn)
                       (prn "sente client driver stopped"))))
        server-response-chan))))