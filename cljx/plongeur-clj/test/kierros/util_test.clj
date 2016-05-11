(ns kierros.util-test
  (use midje.sweet)
  (:require [clojure.core.async :as a :refer [<! <!! >! go close! chan]]
            [kierros.util :refer :all]))

(defn amend-state [state f] (f state))

(def init+amendments [{:key 1} #(assoc % :foo 2) #(assoc % :key "one")])
(def expected-states [{:key 1} {:key 1 :foo 2} {:key "one" :foo 2}])

(facts

  "about the scan transducer"

  (->> []
       (sequence (scan amend-state))) => []

  (->> [{:key 1}]
       (sequence (scan amend-state))) => [{:key 1}]

  (->> [{:key 1} #(assoc % :foo 2)]
       (sequence (scan amend-state))) => [{:key 1} {:key 1 :foo 2}]

  (->> [{:key 1} identity identity identity]
       (sequence (scan amend-state))) => [{:key 1} {:key 1} {:key 1} {:key 1}]

  (->> init+amendments
       (sequence (scan amend-state))) => expected-states

  (let [c (chan 10 (scan amend-state))
        _ (a/onto-chan c init+amendments)
        r (a/into [] c)]
    (<!! r)) => expected-states

)

(facts

  "about the memo transducer"

  )