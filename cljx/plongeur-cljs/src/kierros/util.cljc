(ns kierros.util)

(defn scan
  "Returns a transducer that implements scan (cfr. Scala) or reductions (cfr. Clojure) semantics.
  TODO try this: http://dev.clojure.org/jira/secure/attachment/15550/0001-clojure.core-add-reductions-stateful-transducer.patch"
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

