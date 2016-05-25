(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


;; Sigma instance creation

(defn bind-event-listeners
  "Bind event handlers to the sigma instance, dispatch events through the intent handlers.
  See: https://github.com/jacomyal/sigma.js/wiki/Events-API"
  [cmd-chans sigma-inst]
  (do
    ;; TODO implement event binding system
    )
  sigma-inst)

(defn make-sigma-instance
  "Sigma instance contructor function."

  ([dom-container-id sigma-settings]
   (try
     (let [sigma-inst (js/sigma. dom-container-id)]
       (some->> sigma-settings clj->js (.settings sigma-inst))
       (.refresh sigma-inst))
     (catch :default e
       (prn (str e " " dom-container-id)))))

  ([dom-container-id sigma-settings cmd-chans]
   (some->> (make-sigma-instance dom-container-id sigma-settings)
            (bind-event-listeners cmd-chans))))



(defn kill
  "Kill the specified sigma instance, if present. Returns nil"
  [sigma-inst]
  (some-> sigma-inst .kill)
  nil)