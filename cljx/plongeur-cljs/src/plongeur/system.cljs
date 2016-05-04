(ns plongeur.system
  (:require [cljs.core.async :refer [close!]]
            [plongeur.core :refer [launch-plongeur]]))

(defonce app (-> (launch-plongeur) (atom)))

(defn on-js-reload []
  (prn "reloading app")
  (swap! app (fn [shutdown-fn] (shutdown-fn) (launch-plongeur))))