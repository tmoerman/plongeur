(ns kierros.local-storage-driver
  (:require [cljs.core.async :as a :refer [<! chan]]
            [cljs.reader :as r])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn store!
  [storage-key state]
  (when (map? state)
    (.setItem js/localStorage storage-key (str state))))

(defn load
  [storage-key]
  (when-let [stored-state (.getItem js/localStorage storage-key)]
    (r/read-string stored-state)))

(defn make-storage-driver
  "Accepts a storage key and an initial state.
  Returns a storage driver that takes a state channel and returns
  a channel with the previously stored or specified initial state."
  [storage-key init-state]
  (fn [state-chan]
    (let [return-state (or (load storage-key) init-state)]
      (go-loop []
               (when-let [state (<! state-chan)]
                 (store! storage-key state))
               (recur))
      (a/to-chan [return-state]))))
