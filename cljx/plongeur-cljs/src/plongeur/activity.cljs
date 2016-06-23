(ns plongeur.activity
  "OMG, we're got an Android concept here, run for yer lives!"
  (:require [cljs.core.async :as a :refer [<! >! timeout alts! tap chan pipe close!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn nav! [url] (set! (.-location js/window) url))

(defn ->browse-scenes
  "Navigate to the scene browser page."
  [{:keys [post-request] :as cmd-chans}]
  ;; TODO request list of scenes

  (nav! "#/browse"))

(defn ->create-scene
  "Navigate to the scene creation page."
  [{:keys [post-request] :as cmd-chans}]
  ;; TODO request list of data files

  (nav! "#/create"))

(defn ->edit-scene
  "Navigate to the scene analysis page."
  [{:keys [post-request] :as cmd-chans} scene-id]
  ;; TODO request scene with id

  (nav! "#/scene"))

(defn ->edit-config
  "Navigate to the application configuration page."
  []
  (nav! "#/config"))