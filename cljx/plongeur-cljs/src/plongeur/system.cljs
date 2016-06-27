(ns plongeur.system
  (:require [cljs.core.async :refer [close!]]
            [plongeur.core :as plongeur])
  (:require-macros [taoensso.timbre :refer [log debug info warn error fatal]]))

(defonce app (-> (plongeur/launch-client) (atom)))

(defn on-js-reload []
  (info "reloading app")
  (swap! app (fn [shutdown-fn] (shutdown-fn) (plongeur/launch-client))))