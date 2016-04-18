(ns kierros.history
  (:require [cljs.core.async :as a :refer [>! chan]]
            [goog.events :as e]
            [goog.History])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn init-history
  "Set up Google Closure history management.
  Returns a channel on which history tokens are put."
  []
  (let [history    (goog.History.)
        token-chan (chan)
        callback   (fn [evt]
                     (let [token (.-token evt)]
                       (.setToken history token)
                       (go (>! token-chan token))))]
    (.setEnabled history true)
    (e/listen history goog.History.EventType.NAVIGATE callback)
    token-chan))
