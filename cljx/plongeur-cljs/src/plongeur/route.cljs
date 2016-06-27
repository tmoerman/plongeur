(ns plongeur.route
  (:require [cljs.core.async :as a :refer [<! >! timeout alts! tap chan pipe close!]]
            [clojure.set :refer [map-invert]]
            [secretary.core :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [taoensso.timbre :refer [log debug info warn error fatal]]
                   [secretary.core  :refer [defroute]]))

(def view->token {:view/none          "/"
                  :view/login-user    "/login"
                  :view/browse-scenes "/browse"
                  :view/create-scene  "/create"
                  :view/edit-scene    "/scene"
                  :view/edit-config   "/config"
                  :view/route-404     "/route-404"})

(defroute none          "/"          [] (fn [evt cmd-chans] :view/none))

(defroute login-user    "/login"     [] (fn [evt cmd-chans] :view/login-user))

(defroute browse-scenes "/browse"    [] (fn [evt cmd-chans] :view/browse-scenes))

(defroute create-scene  "/create"    [] (fn [evt cmd-chans] :view/create-scene))

(defroute edit-scene    "/scene"     [] (fn [evt cmd-chans] :view/edit-scene))

(defroute edit-config   "/config"    [] (fn [evt cmd-chans] :view/edit-config))

(defroute route-404     "*"          [] (fn [evt cmd-chans] :view/route-404))

(defn handle-url-change
  [{:keys [token] :as evt} {:keys [set-view] :as cmd-chans}]

  (debug "routing:" token)
  (if-let [route-fn! (s/dispatch! token)]
    (let [view (route-fn! evt cmd-chans)]
      (debug "calculated view ->" view)
      (go (>! set-view view)))
    (do
      (error "no route for" token)
      (go (>! set-view :view/route-404))))

  evt)