(ns plongeur.datagen)

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