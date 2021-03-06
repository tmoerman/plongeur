(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [foreign.activestate]
            [foreign.forceatlas2]
            [foreign.dragnodes]
            [foreign.keyboard]
            [foreign.lasso]
            [foreign.select]
            [foreign.halo]
            [foreign.graph]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]]
            [sablono.util :refer [camel-case]]
            [clojure.string :refer [starts-with?]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [taoensso.timbre :refer [log debug info warn error fatal]]))

(defn sigma-key
  [str-or-kw]
  (if (keyword? str-or-kw)
    (-> str-or-kw camel-case name)
    (-> str-or-kw)))

;; Sigma properties

(defn renderer
  "Returns the sigma instance's first renderer."
  [sigma-inst]
  (some-> sigma-inst .-renderers (aget 0)))

(defn graph
  "Returns the sigma instance's graph."
  [sigma-inst]
  (some-> sigma-inst .-graph))

;; Plugins

(defn plugins [] (.-plugins js/sigma))

;; Active state
;; https://github.com/Linkurious/linkurious.js/tree/develop/plugins/sigma.plugins.activeState

(defn active-state
  [sigma-inst]
  ;; (.customActiveState (plugins) sigma-inst))
  (.customActiveState (plugins) sigma-inst))

;; Keyboard
;; https://github.com/Linkurious/linkurious.js/tree/linkurious-version/plugins/sigma.plugins.keyboard

(defn keyboard
  ([sigma-inst]
   (keyboard sigma-inst nil))
  ([sigma-inst keyboard-options]
   (if keyboard-options
     (.keyboard (plugins) sigma-inst (renderer sigma-inst) (clj->js keyboard-options))
     (.keyboard (plugins) sigma-inst (renderer sigma-inst))))
  ([sigma-inst keyboard-options bindings-map]
   (let [keyboard-inst (keyboard sigma-inst keyboard-options)]
     (doseq [[k fn] bindings-map]
       (.bind keyboard-inst (sigma-key k) fn))
     keyboard-inst)))

;; Lasso
;; https://github.com/Linkurious/linkurious.js/blob/linkurious-version/examples/lasso.html

(defn lasso
  "The bindings are functions with signature (lasso-inst, event) -> unit"
  ([sigma-inst]
   (lasso sigma-inst nil))
  ([sigma-inst sigma-options]
   (.lasso (plugins) sigma-inst (renderer sigma-inst) (clj->js sigma-options)))
  ([sigma-inst sigma-options bindings-map]
   (let [lasso-inst (lasso sigma-inst sigma-options)]
     (doseq [[k fn] bindings-map]
       (.bind lasso-inst (sigma-key k) (partial fn lasso-inst)))
     lasso-inst)))

#_(defn lasso-active?    [lasso-inst] (.-isActive lasso-inst))
(defn activate   [lasso-inst] (.activate lasso-inst) lasso-inst)
(defn deactivate [lasso-inst] (.deactivate lasso-inst) lasso-inst)

(defn toggle-lasso
  [lasso-inst active?]
  (if active? (activate lasso-inst)
              (deactivate lasso-inst))
  lasso-inst)

;; Sigma Force Atlas 2
;; https://github.com/Linkurious/linkurious.js/tree/develop/plugins/sigma.layouts.forceAtlas2

(defn start-force-atlas-2
  ([sigma-inst]
   (some-> sigma-inst .startForceAtlas2) sigma-inst)
  ([sigma-inst force-atlas-config]
   (if-let [cfg-js (some-> force-atlas-config clj->js)]
     (do
       (some-> sigma-inst (.startForceAtlas2 cfg-js)) sigma-inst)
     (start-force-atlas-2 sigma-inst))))

#_(defn force-atlas-2-running? [sigma-inst] (.isForceAtlas2Running sigma-inst))
#_(defn stop-force-atlas-2     [sigma-inst] (some-> sigma-inst .stopForceAtlas2) sigma-inst)
(defn kill-force-atlas-2     [sigma-inst] (some-> sigma-inst .killForceAtlas2) sigma-inst)

(defn toggle-force-atlas-2
  [sigma-inst running?]
  (if running? (start-force-atlas-2 sigma-inst)
               (kill-force-atlas-2 sigma-inst))
  sigma-inst)

;; Select
;; https://github.com/Linkurious/linkurious.js/tree/linkurious-version/plugins/sigma.plugins.select

(defn select
  ([sigma-inst active-state]
   (.select (plugins) sigma-inst active-state (renderer sigma-inst)))
  ([sigma-inst active-state {:keys [keyboard lasso]}]
   (let [select-inst (select sigma-inst active-state)]
     (some->> keyboard (.bindKeyboard select-inst))
     (some->> lasso    (.bindLasso    select-inst))
     select-inst)))

;; Drag nodes
;; https://github.com/Linkurious/linkurious.js/tree/develop/plugins/sigma.plugins.dragNodes

(defn drag-nodes
  "Arity 2: returns the drag listener.
  Arity 3: returns the sigma instance."
  ([sigma-inst active-state]
   (.dragNodes (plugins) sigma-inst (renderer sigma-inst) active-state))
  ([sigma-inst active-state bindings-map]
   (let [drag-listener (drag-nodes sigma-inst active-state)]
     (doseq [[k fn] bindings-map]
       (.bind drag-listener (sigma-key k) fn)))
   sigma-inst))

(defn kill-drag-nodes
  [sigma-inst]
  (.killDragNodes sigma-inst) sigma-inst)

;; Halo
;; https://github.com/Linkurious/linkurious.js/tree/develop/plugins/sigma.renderers.halo

(defn halo-active-nodes
  [sigma-inst active-state]
  (let [renderer     (->> sigma-inst renderer)
        active-nodes (->> active-state .nodes (hash-map :nodes) clj->js)]
    (.bind renderer "render" #(.halo renderer active-nodes))))

;; Sigma public API
;; See https://github.com/Linkurious/linkurious.js/wiki/Public-API

(defn refresh
  [sigma-inst]
  (some-> sigma-inst .refresh) sigma-inst)

(defn kill
  [sigma-inst]
  (some-> sigma-inst .kill) nil)

;; Sigma constructor
;; https://github.com/jacomyal/sigma.js/wiki/Events-API

(defn make-sigma-instance
  "Sigma instance contructor.
  The bindings are functions with signature (sigma-inst, payload) -> unit"
  ([dom-container-id sigma-settings]
   (debug "settings " sigma-settings)
   (try
     (some-> (new js/sigma (clj->js {:id       dom-container-id
                                     :settings sigma-settings
                                     :renderer {:type      "canvas"
                                                :container dom-container-id}}))
             (refresh))
     (catch :default e
       (debug e " " dom-container-id))))
  ([dom-container-id sigma-settings bindings-map]
   (when-let [sigma-inst (make-sigma-instance dom-container-id sigma-settings)]
     (doseq [[k fn] bindings-map]
       (.bind sigma-inst (sigma-key k) (partial fn sigma-inst)))
     sigma-inst)))

;; Sigma Graph API
;; See https://github.com/Linkurious/linkurious.js/wiki/Graph-API

(defn clean
  [entries]
  (->> entries
       (map #(->> %
                  (remove (fn [[k _]] (or (starts-with? k "renderer")
                                          (starts-with? k "read_cam"))))
                  (into {})))))

(defn nodes
  ([sigma-inst]
   (some-> sigma-inst graph .nodes js->clj clean))
  ([sigma-inst node-id]
   (some-> sigma-inst graph (.nodes node-id) js->clj clean)))

(defn edges
  ([sigma-inst]
   (some-> sigma-inst graph .edges js->clj clean))
  ([sigma-inst node-id]
   (some-> sigma-inst graph (.edges node-id) js->clj clean)))

(defn data
  [sigma-inst]
  {:nodes (nodes sigma-inst)
   :edges (edges sigma-inst)})

(defn add-node
  [sigma-inst node]
  (try
    (.addNode (graph sigma-inst) (clj->js node))
    (catch :default e (debug e node)))
  sigma-inst)

(defn add-edge
  [sigma-inst edge]
  (try
    (.addEdge (graph sigma-inst) (clj->js edge))
    (catch :default e (debug e edge)))
  sigma-inst)

(defn read
  [sigma-inst network]
  (try
    (.read (graph sigma-inst) (clj->js network))
    (catch :default e (debug e network)))
  sigma-inst)

(defn clear
  [sigma-inst]
  (some-> sigma-inst graph .clear) sigma-inst)

;; Data generators

(defn make-node
  [id]
  {:id    id
   :label (str "node " id)
   :x     (rand-int 100)
   :y     (rand-int 100)
   :size  (-> (rand-int 10) (* 0.1))})

(defn make-edge
  [id source target]
  {:id     id
   :label  (str id)
   :source source
   :target target})

(defn make-loop
  [size]
  {:nodes (->> size
               range
               (map (fn [id] (make-node id))))
   :edges (->> size
               range
               (map (fn [id] (make-edge id id (-> id inc (mod size))))))})

(defn make-star
  [nr-arms arm-size]
  {:nodes (-> (for [a (->> nr-arms  range (map inc))
                    e (->> arm-size range (map inc))]
                (make-node (-> (dec a) (* arm-size) (+ e))))
              (conj (make-node 0)))
   :edges (for [a (->> nr-arms  range (map inc))  ;; 1 2 3
                e (->> arm-size range (map inc))] ;; 1 2 3 4 5
            (let [id     (-> (dec a) (* arm-size) (+ e))
                  source (if (= e 1) 0 (dec id))
                  target id]
              (make-edge id source target)))})

(defn make-shape
  []
  (condp = (rand-int 2)
    0 (make-loop (-> (rand-int 97) (+ 3)))
    1 (make-star (-> (rand-int 07) (+ 3))
                 (-> (rand-int 17) (+ 3)))))