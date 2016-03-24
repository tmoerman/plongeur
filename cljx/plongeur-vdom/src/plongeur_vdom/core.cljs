(ns plongeur-vdom.core
  (:require [dominator.core :as d :refer [render]]
            [stch.html :refer [div table tr td input canvas]]
            [cljs.core.async :as async :refer [<!]]
            [dominator.async :as as :refer-macros [forever]]
            [jamesmacaulay.zelkova.signal :as z]
            [cljs.core.match]
            [clojure.string :as str])
  (:require-macros [cljs.core.match.macros :refer [match]]))

(enable-console-print!)

(def people ["Billy" "Bobby" "Joey"])

(def action$ (z/write-port :no-op))

(defn view [model]
  (div
    (table
      (tr
        (for [person people]
          (td
            (input :type "button" :value person
                   :onclick (as/send action$ [:clicked person]))))) ;; put click event on actions channel
      (tr
        (for [person people]
          (td
            (input :type "text" :readonly true :value (get model person 0))))))
    (div :id "button-row"
         (input :type "button" :value "Reset"
                :onclick (as/send action$ :reset))))) ;; put reset event on actions channel

(defn update-model [model action] ;; scan function ~ reductions
  (match action
         :no-op model
         :reset {}
         [:clicked n] (update-in model [n] inc)))

(def model$ (z/reductions update-model {} action$))

;; do something with defonce to make this work
(d/render (z/map view model$) js/document.body)

