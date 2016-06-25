(ns plongeur.route
  (:require [cljs.core.async :as a :refer [<! >! timeout alts! tap chan pipe close!]]
            [clojure.set :refer [map-invert]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def view->token {:view/none          ""
                  :view/login-user    "/login"
                  :view/browse-scenes "/browse"
                  :view/create-scene  "/create"
                  :view/edit-scene    "/scene"
                  :view/edit-config   "/config"})

(def token->view (map-invert view->token))

(defn handle-url-change
  [{:keys [token nav?] :as evt} {:keys [set-view]}]

  (let [view (token->view token)]
    (go (>! set-view view))

    (prn (-> evt
             (assoc :view view))))
  evt)