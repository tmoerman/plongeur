(ns plongeur.system
  (:require [cljs.core.async :refer [close!]]
            [plongeur.core :as plongeur]))

(defonce app (-> (plongeur/launch-client) (atom)))

(defn on-js-reload []
  (prn "reloading app")
  (swap! app (fn [shutdown-fn] (shutdown-fn) (plongeur/launch-client))))