(ns kierros.quiescent-dom-driver
  "Cycle DOM driver, powered by quiescent."
  (:require [cljs.core.async :refer [<! chan]]
            [quiescent.core :as q :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn make-dom-driver
  "Accepts a DOM container as rendering root.
  Returns a DOM driver powered by Quiescent."
  [container & options]
  (fn [views-chan]
    (go-loop []
             (when-let [view (<! views-chan)]
               (.requestAnimationFrame
                 js/window
                 (fn [] (q/render view (.getElementById js/document container)))))
             (recur))
    (chan))) ; For now not much is going on here. Dom event should be put on this chan.
