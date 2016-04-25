(ns plongeur.sigma-driver
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]]
            [foreign.sigma]
            [clojure.set :refer [difference]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn container-id [id] (str "graph-" id))

(def default-options
  {:sigma-settings {:verbose true}})

(defn new-sigma
  "Accepts an id and sigma settings in js format.
  Returns a new sigma instance or nil if the container div does not exist."
  [id settings-js]
  (try
    (let [sigma-inst (js/sigma. (container-id id))]
      (when settings-js (.settings sigma-inst settings-js))
      (.refresh sigma-inst))
    (catch :default _ nil)))

(defmulti apply-evt! (fn [_ evt] (:type evt))) ;; translate events into renderer actions.
(defmethod apply-evt! :add-node [sigma evt]
  (let [graph   (-> sigma :renderer .-graph)
        node-js (-> evt :node clj->js)]
    (.addNode graph node-js)))
; etc ...


(defn make-sigma-context
  "Accepts a graph id, the out channel and options.
  Return a map containing the sigma instance and a listener async go-loop channel."
  [in-mult out-chan options id]
  (when-let [sigma-inst (->> (some-> options :sigma-settings clj->js)
                             (new-sigma id))]
    (let [select-xf (filter #(-> % :graph (= id)))
          in-tap    (->> (sliding-buffer 10) (chan select-xf) (tap in-mult))
          in-loop   (go-loop []
                             (when-let [evt (<! in-tap)]
                               (apply-evt! sigma-inst evt)))]
      {:sigma    sigma-inst
       :listener in-loop})))

(defn dispose
  [m]
  (some-> m :sigma .kill)
  (some-> m :loop close!))

(defn remove-renderers
  [sigma-state-atom & ids]
  (swap! sigma-state-atom
         (fn [m]
           (for [id ids] (some-> m id dispose))
           (dissoc m ids))))

(defn update-renderer
  [sigma-state-map in-mult out-chan options id]
  (update-in sigma-state-map [id]
             (fn [old-renderer]
               (dispose old-renderer)
               (make-sigma-context in-mult out-chan options id))))

(defn update-renderers
  [sigma-state-atom in-mult out-chan options & ids]
  (swap! sigma-state-atom
         (fn [m]
           (reduce #(update-renderer %1 in-mult out-chan options %2) m ids))))


(defn sync-renderers
  "Accepts the current sigma state, a graphs-state which is the
  content of the :graphs key in the app state and an options map.

  app-state
    {:graphs ({:id 5} {:id 4} {:id 3}), :seq 6}"
  [sigma-state in-mult out-chan options app-state]
  (let [sigma-ids     (-> @sigma-state keys set)
        graph-ids     (-> app-state :graphs vals set)
        ids-to-remove (difference sigma-ids graph-ids)]
    (remove-renderers sigma-state ids-to-remove)
    (update-renderers sigma-state in-mult out-chan options graph-ids)))


(defmulti process (fn [_ msg _ _ _] (:type msg)))
(defmethod process :sync   [sigma-state app-state in-mult out-chan options] (sync-renderers   sigma-state in-mult out-chan options app-state))
(defmethod process :update [sigma-state id        in-mult out-chan options] (update-renderers sigma-state in-mult out-chan options id))
(defmethod process :remove [sigma-state id        _       _        _      ] (remove-renderers sigma-state id))

(defn make-sigma-driver
  "Accepts a sigma inbound channel.
  Returns a Sigma.js driver."
  [& options]
  (fn [in-chan]
    (let [in-mult     (mult in-chan)
          out-chan    (chan)
          sigma-state (atom {})
          options     (or options default-options)]
      (go-loop []
               (when-let [msg (<! in-chan)]
                 (process sigma-state msg in-mult out-chan options))
               (recur))
      out-chan)))