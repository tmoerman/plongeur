(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [plongeur.model :refer [graph-ids]]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


;; Sigma instance creation

(defn container-dom-id [id] (str "graph-" id))

(defn make-sigma-instance
  "Accepts an id and sigma settings in JSON format.
  Returns a new sigma instance or nil if the container div does not exist."
  [id settings-json]
  (try
    (let [sigma-inst (js/sigma. (container-dom-id id))]
      (when settings-json
        (.settings sigma-inst settings-json))
      (.refresh sigma-inst))
    (catch :default e
      (prn (str e " " (container-dom-id id))))))


;; Dispatch events to Sigma instanes

(defmulti apply-evt! (fn [_ evt] (:type evt))) ;; translate events into renderer actions.


;; Sigma context

(defn make-sigma-context
  "Accepts a graph id, the out channel and options.
  Return a map containing the sigma instance and a listener async go-loop channel."
  [in-mult out-chan options id]
  (when-let [sigma-inst (->> (some-> options :sigma-settings clj->js)
                             (make-sigma-instance id))]

    (let [select-xf (filter #(-> % :graph (= id)))
          in-tap    (->> select-xf (chan 10) (tap in-mult))
          in-loop   (go-loop []
                             (when-let [evt (<! in-tap)]
                               (do
                                 (apply-evt! sigma-inst evt)
                                 (recur))))]
      {:sigma    sigma-inst
       :listener in-loop})))

(defn dispose!
  [{:keys [sigma listener]}]
  (some-> sigma    .kill)
  (some-> listener close!))