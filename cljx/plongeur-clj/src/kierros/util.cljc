(ns kierros.util)

(defn scan
  "Returns a transducer that implements scan (cfr. Scala) or reductions (cfr. Clojure) semantics."
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

(defn deco [s]
  "Decorate the specified string."
  (let [length (-> s count inc inc)
        bar    (str "+" (->> "-" (repeat length) (apply str)) "+")
        ]
    (str "\n\n" bar "\n| " s " |\n" bar "\n")))