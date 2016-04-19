(ns plongeur-cljs.intent
  (:require [cljs.core.async :as a :refer [<! chan dropping-buffer]]))

(def intent-keys
  [:network-calculated ; visualization calculated - server message
   :nodes-selected     ; nodes selected in network visualization
   :bus])

(defn intent-chan [] (chan (dropping-buffer 10)))

(defn intents
  "Returns a map of event channels."
  []
  (->> intent-keys
       (map (fn [key] [key (intent-chan)]))
       (into {})))