(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [foreign.activestate]
            [foreign.forceatlas2]
            [foreign.dragnodes]
            [foreign.keyboard]
            [foreign.lasso]
            [foreign.select]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]]
            [sablono.util :as u])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn sigma-key
  [str-or-kw]
  (if (keyword? str-or-kw)
    (-> str-or-kw u/camel-case name)
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
;; TODO figure out state management of this plugin... something smells fishy here...

(defn active-state
  [sigma-inst]
  (.activeState (plugins) sigma-inst))

(defn kill-active-state
  []
  (.killActiveState (plugins))
  (plugins))

;; Keyboard
;; https://github.com/Linkurious/linkurious.js/tree/linkurious-version/plugins/sigma.plugins.keyboard

(defn keyboard
  ([sigma-inst]
   (keyboard sigma-inst nil))
  ([sigma-inst keyboard-options]
   (.keyboard (plugins) sigma-inst (renderer sigma-inst) (clj->js keyboard-options)))
  ([sigma-inst keyboard-options bindings-map]
   (let [keyboard-inst (keyboard sigma-inst keyboard-options)]
     (doseq [[k fn] bindings-map]
       (.bind keyboard-inst (sigma-key k) fn))
     keyboard-inst)))

;; Lasso
;; https://github.com/Linkurious/linkurious.js/blob/linkurious-version/examples/lasso.html

(defn lasso
  ([sigma-inst]
   (lasso sigma-inst nil))
  ([sigma-inst sigma-options]
   (.lasso (plugins) sigma-inst (renderer sigma-inst) (clj->js sigma-options)))
  ([sigma-inst sigma-options bindings-map]
   (let [lasso-inst (lasso sigma-inst sigma-options)]
     (doseq [[k fn] bindings-map]
       (.bind lasso-inst (sigma-key k) fn))
     lasso-inst)))

#_(defn lasso-active?    [lasso-inst] (.-isActive lasso-inst))
(defn activate-lasso   [lasso-inst] (.activate lasso-inst) lasso-inst)
(defn deactivate-lasso [lasso-inst] (.deactivate lasso-inst) lasso-inst)

(defn toggle-lasso
  [lasso-inst active?]
  (if active? (activate-lasso lasso-inst)
              (deactivate-lasso lasso-inst))
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

;; Sigma public API
;; See https://github.com/Linkurious/linkurious.js/wiki/Public-API

(defn refresh
  [sigma-inst]
  (some-> sigma-inst .refresh) sigma-inst)

(defn kill
  [sigma-inst]
  (some-> sigma-inst .kill) nil)

(defn apply-settings
  [sigma-inst sigma-settings]
  (when-let [sigma-settings-js (some-> sigma-settings clj->js)]
    (some-> sigma-inst (.settings sigma-settings-js)))
  sigma-inst)

(defn add-renderer
  [sigma-inst renderer-options]
  (when-let [renderer-options-js (some-> renderer-options clj->js)]
    (some-> sigma-inst (.addRenderer renderer-options-js)))
  sigma-inst)

;; Sigma constructor

(defn make-sigma-instance
  "Sigma instance contructor.
  See: https://github.com/jacomyal/sigma.js/wiki/Events-API "
  ([dom-container-id sigma-settings]
   (try
     (some-> (new js/sigma)
             (add-renderer   {:type "canvas" ;; some plugins only work with canvas renderer (as opposed to WebGL)
                              :container dom-container-id})
             (apply-settings sigma-settings)
             (refresh))
     (catch :default e
       (prn (str e " " dom-container-id)))))
  ([dom-container-id sigma-settings bindings-map]
   (when-let [sigma-inst (make-sigma-instance dom-container-id sigma-settings)]
     (doseq [[k fn] bindings-map]
       (.bind sigma-inst (sigma-key k) fn))
     sigma-inst)))

;; Sigma Graph API
;; See https://github.com/Linkurious/linkurious.js/wiki/Graph-API

(defn nodes
  ([sigma-inst]
   (some-> sigma-inst graph .nodes js->clj))
  ([sigma-inst node-id]
   (some-> sigma-inst graph (.nodes node-id) js->clj)))

(defn edges
  ([sigma-inst]
   (some-> sigma-inst graph .edges js->clj))
  ([sigma-inst node-id]
   (some-> sigma-inst graph (.edges node-id) js->clj)))

(defn data
  [sigma-inst]
  {:nodes (nodes sigma-inst)
   :edges (edges sigma-inst)})

(defn add-node
  [sigma-inst node]
  (try
    (.addNode (graph sigma-inst) (clj->js node))
    (catch :default e (prn (str e node))))
  sigma-inst)

(defn add-edge
  [sigma-inst edge]
  (try
    (.addEdge (graph sigma-inst) (clj->js edge))
    (catch :default e (prn (str e edge))))
  sigma-inst)

(defn read
  [sigma-inst network]
  (try
    (.read (graph sigma-inst) (clj->js network))
    (catch :default e (prn (str e network))))
  sigma-inst)

(defn clear
  [sigma-inst]
  (some-> sigma-inst graph .clear) sigma-inst)

;; Data generators

(defn make-node
  [id]
  {:id id
   :label id
   :x (rand-int 100)
   :y (rand-int 100)
   :size (-> (rand-int 10) (* 0.1))
   :color "#FF9"})

(defn make-edge
  [id source target]
  {:id id :label id :source source :target target})

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
                  target id
                  ]
              (make-edge id source target)))})

(defn make-shape
  []
  (condp = (rand-int 2)
    0 (make-loop (-> (rand-int 97) (+ 3)))
    1 (make-star (-> (rand-int 07) (+ 3))
                 (-> (rand-int 17) (+ 3)))))