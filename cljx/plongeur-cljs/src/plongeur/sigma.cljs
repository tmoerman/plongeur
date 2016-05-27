(ns plongeur.sigma
  "See: https://github.com/Linkurious/linkurious.js/wiki"
  (:require [foreign.sigma]
            [clojure.set :refer [difference]]
            [cljs.core.async :as a :refer [<! chan mult tap untap close! sliding-buffer]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;; Sigma Public API
;; See https://github.com/Linkurious/linkurious.js/wiki/Public-API

(defn graph
  "Returns the sigma instance's graph property"
  [sigma-inst]
  (.-graph sigma-inst))

(defn refresh
  [sigma-inst]
  (some-> sigma-inst .refresh) sigma-inst)

(defn kill
  [sigma-inst]
  (some-> sigma-inst .kill) nil)

(defn settings
  [sigma-inst sigma-settings]
  (when-let [settings-js (some-> sigma-settings clj->js)]
    (some-> sigma-inst (.settings settings-js)))
  sigma-inst)

;; Sigma constructors

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
     (some-> (js/sigma. dom-container-id)
             (settings sigma-settings)
             (refresh))
     (catch :default e
       (prn (str e " " dom-container-id)))))

  ([dom-container-id sigma-settings cmd-chans]
   (some->> (make-sigma-instance dom-container-id sigma-settings)
            (bind-event-listeners cmd-chans))))


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