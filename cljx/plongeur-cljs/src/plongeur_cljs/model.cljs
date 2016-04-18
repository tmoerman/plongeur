(ns plongeur-cljs.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.model :refer [scan-to-states]]))

(def intent-handlers
  {:network-calculated identity ; visualization calculated - server message
   :nodes-selected     identity})

(def default-state
  "Returns a new initial application state."
  {})

(defn model
  [init-state-chan intent-chans]
  (scan-to-states init-state-chan intent-chans intent-handlers))