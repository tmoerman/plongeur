(ns kierros.sente-server-driver
  "Sente websocket server driver.
  See https://github.com/ptaoussanis/sente"
  (:require [clojure.core.async :refer [<! >! chan close! go go-loop]]
            [compojure.core :refer [routes GET POST]]
            [compojure.route :as route]
            [org.httpkit.server :as h]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as s]
            [hiccup.core :refer [html]]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.timbre :refer [warn]]))

;; TODO styling of these pages

(defn landing-page   [] (html [:h1 "Ici Plongeur CLJ!"]))

(defn page-not-found [] (html [:h1 "Parbleu! 404"]))

(defn connections-page
  [connected-uids]
  (html [:h1 "Connected UIDs:"
         (for [[key id-set] connected-uids]
           (list [:h2 key]
                 [:ul]
                 (for [id id-set]
                   [:li id])))]))

(defn make-sente-server-driver
  "Accepts an options map.
  Returns a websocket server driver powered by Sente and http-kit.
  The driver accepts a channel of client-bound response push messages.
  The driver returns a channel of client requests."
  [& [{sente-options    :sente
       http-kit-options :http-kit}]]
  (fn [response-push-chan]
    (let [{:keys [ch-recv
                  send-fn
                  connected-uids
                  ajax-post-fn
                  ajax-get-or-ws-handshake-fn]} (s/make-channel-socket-server! sente-web-server-adapter sente-options)
          sente-routes (routes (GET  "/"     _   (landing-page))
                               (GET  "/cnxn" _   (connections-page @connected-uids))
                               (GET  "/chsk" req (ajax-get-or-ws-handshake-fn req))
                               (POST "/chsk" req (ajax-post-fn req))
                               (route/not-found  (page-not-found)))
          ring-handler (wrap-defaults sente-routes site-defaults)
          http-shutdown-fn    (h/run-server ring-handler http-kit-options)
          client-request-chan (chan)
          router-shutdown-fn  (s/start-chsk-router! ch-recv (fn [{:keys [client-id id uid event connected-uids] :as msg}]
                                                              (prn (str uid " - " id " - " client-id " - " event))
                                                              (go (>! client-request-chan msg))))]
      (go-loop []
               (if-let [msg (<! response-push-chan)]
                 (do (send-fn msg)
                     (recur))
                 (do (http-shutdown-fn)
                     (router-shutdown-fn)
                     (warn "sente server driver stopped"))))
      client-request-chan)))