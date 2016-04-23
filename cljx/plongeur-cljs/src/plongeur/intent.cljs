(ns plongeur.intent
  (:require [cljs.core.async :as a :refer [<! chan dropping-buffer]]
            [plongeur.model :refer [intent-handlers]]))

(defn intent-chan [] (chan (dropping-buffer 10)))

(defn intents
  "Returns a map of event channels."
  []
  (->> (keys intent-handlers)
       (map (fn [key] [key (intent-chan)]))
       (into {})))