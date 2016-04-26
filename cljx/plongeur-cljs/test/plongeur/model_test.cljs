(ns plongeur.model-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async]]
            [plongeur.model :as m]))

(deftest graph-ids
  (let [state {:graphs {1 {:k :v}
                        2 {:k :v}
                        3 {:k :v}}}]
    (is (= (m/graph-ids state) [1 2 3]))))

(deftest add-graph
  (let [old-state {:seq 1
                   :graphs {}}
        new-state (m/add-graph nil old-state)]
    (is (= new-state {:seq 2 :graphs {1 {}}}))))

(deftest drop-graph
  (let [old-state {:seq 3
                   :graphs {1 {}
                            2 {}}}
        new-state (m/drop-graph 1 old-state)]
    (is (= new-state {:seq 3 :graphs {2 {}}}))))

(run-tests)