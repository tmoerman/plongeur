(ns kierros.sente-client-driver
  "Sente websocket client driver. See https://github.com/ptaoussanis/sente"
  (:require [cljs.core.async :as a :refer [<! >! put! close! chan pipe]]
            [taoensso.sente  :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn make-sente-client-driver
  "Accepts a path and options.
  Returns a websocket client driver powered by Sente."
  [path & options]
  (fn [request-chan]
    (let [{:keys [chsk ch-recv send-fn]} (s/make-channel-socket-client! path options)]
      (go-loop []
               (if-let [request (<! request-chan)]
                 (do (send-fn request)
                     (recur))
                 (do (some-> chsk s/chsk-disconnect!)
                     (prn "sente client driver stopped"))))
      ch-recv)))