(ns plongeur.sigma-driver
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [plongeur.model :refer [graph-ids]]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]]
            )
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn container-id [id] (str "graph-" id))

(defn new-sigma
  "Accepts an id and sigma settings in js format.
  Returns a new sigma instance or nil if the container div does not exist."
  [id settings-js]
  (try
    (let [sigma-inst (js/sigma. (container-id id))]
      (when settings-js (.settings sigma-inst settings-js))
      (.refresh sigma-inst))
    (catch :default e
      (prn (str e " " (container-id id))))))

(defmulti apply-evt! (fn [_ evt] (:type evt))) ;; translate events into renderer actions.
(defmethod apply-evt! :add-node [sigma evt]
  (let [graph   (-> sigma :renderer .-graph)
        node-js (-> evt :node clj->js)]
    (.addNode graph node-js)
    ;; (.refresh ) here?
    ))

(defmethod apply-evt! :init-graph [sigma evt]
  ;; draw the entire TDA-graph
  :TODO
  )
;; etc ...

(defn sigma-ids  [sigma-state] (keys sigma-state))
(defn sigma-ctxs [sigma-state] (vals sigma-state))

(defn make-sigma-context
  "Accepts a graph id, the out channel and options.
  Return a map containing the sigma instance and a listener async go-loop channel."
  [in-mult out-chan options id]
  (when-let [sigma-inst (->> (some-> options :sigma-settings clj->js)
                             (new-sigma id))]
    (let [select-xf (filter #(-> % :graph (= id)))
          in-tap    (tap in-mult (-> (sliding-buffer 10) (chan select-xf)))
          in-loop   (go-loop []
                             (when-let [evt (<! in-tap)]
                               (apply-evt! sigma-inst evt)))]
      {:sigma    sigma-inst
       :listener in-loop})))

(defn dispose!
  [{:keys [sigma listener]}]
  (some-> sigma    .kill)
  (some-> listener close!))

(defn dispose-all!
  [sigma-state]
  (doseq [sigma-ctx (sigma-ctxs sigma-state)] (dispose! sigma-ctx)))

(defn remove-renderers
  "Dispose and remove renderers with specified graph ids."
  [sigma-state-atom ids]
  (swap! sigma-state-atom
         (fn [sigma-state]
           (doseq [id ids] (some-> id sigma-state dispose!))
           (apply dissoc sigma-state ids))))

(defn update-renderer
  "Update the sigma-state with a new renderer, if instantiation of the renderer was successful."
  [sigma-state in-mult out-chan options id]
  (if-let [sigma-ctx (make-sigma-context in-mult out-chan options id)]
    (update-in sigma-state [id]
               (fn [old-sigma-ctx] (dispose! old-sigma-ctx) sigma-ctx))
    sigma-state))

(defn update-renderers
  [sigma-state-atom in-mult out-chan options ids]
  (swap! sigma-state-atom
         (fn [sigma-state]
           (reduce #(update-renderer %1 in-mult out-chan options %2) sigma-state ids))))

(defn sync-renderers
  "Synchronizes the sigma-state with respect to the current graph ids."
  [sigma-state-atom in-mult out-chan options ids]
  (let [sigma-ctx-ids (-> @sigma-state-atom sigma-ids set)
        ids-to-remove (difference sigma-ctx-ids ids)]
    (remove-renderers sigma-state-atom ids-to-remove)
    (update-renderers sigma-state-atom in-mult out-chan options ids)))

(defmulti process (fn [_ msg _ _ _] (:type msg)))
(defmethod process :sync   [sigma-state-atom ids in-mult out-chan options] (sync-renderers   sigma-state-atom in-mult out-chan options ids))
(defmethod process :update [sigma-state-atom ids in-mult out-chan options] (update-renderers sigma-state-atom in-mult out-chan options ids))
(defmethod process :remove [sigma-state-atom ids _       _        _      ] (remove-renderers sigma-state-atom ids))

(def default-options
  {:graph-lib      :sigma          ;; or :linkurious
   :sigma-settings {:verbose true} ;; gets turned into json and passed to the sigma constructor.
   })

(defn make-sigma-driver
  "Accepts a sigma inbound channel.
  Returns a Sigma/Linkurious driver."
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