(ns kierros.local-storage-driver
  (:require [cljs.core.async :refer [<! chan]]))

(defn storage-driver [pickled-chan & args] (chan))
