(ns plongeur.util
  (:require [taoensso.timbre :refer [info]]))

(defn echo
  "Accepts an argument x and a function f that takes one argument.
  Applies f to x, returns x."
  [x]
  (prn (str "ECHO: " x))
  x)