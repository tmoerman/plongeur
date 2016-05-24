(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


;; Sigma instance creation

(defn graph-id [id] (str "graph-" id))

(defn make-sigma-instance
  "Accepts an id and sigma settings in JSON format.
  Returns a new sigma instance or nil if the container div does not exist."
  [id sigma-settings]
  (try
    (let [sigma-inst (js/sigma. (graph-id id))]
      (some->> sigma-settings clj->js (.settings sigma-inst))
      (.refresh sigma-inst))
    (catch :default e
      (prn (str e " " (graph-id id))))))

(defn bind-event-listeners
  "Bind event handlers to the sigma instance, dispatch events through the intent handlers.
  See: https://github.com/jacomyal/sigma.js/wiki/Events-API"
  [cmd-chans sigma-inst]
  (do
    ;; TODO implement event binding system
    )
  sigma-inst)

(defn make-sigma-instance-with-events
  "Accepts a graph id, the out channel and options.
  Return a map containing the sigma instance and a listener async go-loop channel."
  [id sigma-settings cmd-chans]
  (some->> (make-sigma-instance id sigma-settings)
           (bind-event-listeners cmd-chans)))

(defn kill
  "Kill the specified sigma instance, if present. Returns nil"
  [sigma-inst]
  (some-> sigma-inst .kill)
  nil)