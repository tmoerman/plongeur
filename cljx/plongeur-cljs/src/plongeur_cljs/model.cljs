(ns plongeur-cljs.model
  (:require [cljs.core.async :as a :refer [<! chan to-chan pipe]]
            [kierros.util :refer [scan]]
            [kierros.async :refer [chain]]))

(defn init-state
  "Returns a new initial application state."
  []
  {})

(defn model
  [init-state intent-chans]
  ;TODO complete me
  )