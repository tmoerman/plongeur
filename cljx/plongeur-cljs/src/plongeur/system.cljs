(ns plongeur.system
  (:require [cljs.core.async :refer [close!]]
            [plongeur.core :refer [launch-plongeur]]))

(defn run-app [] (launch-plongeur))

(defonce sys-atom (-> (run-app) (atom)))

(defn on-js-reload []
  (prn "reloading")
  (swap! sys-atom (fn [shutdown-fn] (shutdown-fn) (run-app))))