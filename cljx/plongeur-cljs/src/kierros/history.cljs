(ns kierros.history
  (:require [cljs.core.async :as a :refer [>! chan sliding-buffer]]
            [goog.events :as e]
            [goog.History])
  (:import [goog.history EventType])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn init-history
  "Set up Google Closure history management.
  Returns a channel on which history tokens are put."
  []
  (let [history    (goog.History.)
        token-chan (chan (sliding-buffer 1))
        callback   (fn [evt]
                     (let [token (.-token evt)]
                       (.log js/console "history event: " evt)
                       (.setToken history token)
                       (go (>! token-chan token))))]
    (doto history
      (.setEnabled     true)
      (e/listen        EventType.NAVIGATE callback))

    token-chan))