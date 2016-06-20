(ns plongeur.machine
  (:require [clojure.data :refer [diff]]))

(defn diff-ops
  "Computes the difference between specified old and new sets."
  [old new]
  (let [[only-in-old only-in-new _] (diff (set old) (set new))]
    {:to-remove only-in-old
     :to-add    only-in-new}))