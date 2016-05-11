(ns kierros.util)

(defn scan
  "Returns a stateful transducer that implements scan (cfr. Scala) or reductions (cfr. Clojure) semantics."
  [f]
  (fn [xf]
    (let [state (volatile! ::none)]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
         (let [prev @state
               next (if (= prev ::none)
                      input
                      (f prev input))]
           (vreset! state next)
           (xf result next)))))))

#_(defn memo
  "Returns a stateful transducer that remembers the last item to pass through it."
  []
  (fn [xf]
    (let [state (volatile! ::none)]
      (fn
        ([] (xf))
        ([result]
         (let [prev @state]


           )
         (xf result))
        ([result input]
         (let [prev @state]

           ))))))

#_(defn dedupe
   "Returns a lazy sequence removing consecutive duplicates in coll.
   Returns a transducer when no collection is provided."
   {:added "1.7"}
   ([]
    (fn [rf]
      (let [pv (volatile! ::none)]
        (fn
          ([] (rf))
          ([result] (rf result))
          ([result input]
           (let [prior @pv]
             (vreset! pv input)
             (if (= prior input)
               result
               (rf result input))))))))
   ([coll] (sequence (dedupe) coll)))

(defn frame [s]
  "Decorate the specified string with a frame."
  (let [len (-> s count inc inc)
        bar (str "+" (->> "-" (repeat len) (apply str)) "+")]
    (str "\n\n" bar "\n| " s " |\n" bar "\n")))