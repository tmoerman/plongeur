(ns kierros.sente-websocket-driver
  "Sente websocket driver. See https://github.com/ptaoussanis/sente"
  (:require [cljs.core.async :as a :refer [<! >! put! close! chan pipe]]
            [taoensso.sente  :as s :refer [make-channel-socket! cb-success?]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn make-websocket-driver
  "Accepts a path and options.
  Returns a websocket driver powered by Sente."
  [path & options]
  (fn [request-chan]
    (let [default-options   {:type :auto}
          {:keys [ch-recv
                  send-fn]} (make-channel-socket! path (or options default-options))
          websocket-send!   send-fn
          response-chan     ch-recv]
      (go-loop []
               (when-let [request (<! request-chan)]
                 (websocket-send! request))
               (recur))
      response-chan)))