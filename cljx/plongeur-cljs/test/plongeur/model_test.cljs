(ns plongeur.model-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async]]
            [plongeur.model :as m]
            [plongeur.config :as c]))

(deftest graph-ids-test
  (let [state {:graphs {1 {:k :v}
                        2 {:k :v}
                        3 {:k :v}}}]
    (is (= (m/graph-ids state)
           [1 2 3]))))


(deftest graph-count-test
  (let [state {:graphs {1 {:k :v}
                        2 {:k :v}
                        3 {:k :v}}}]
    (is (= (m/graph-count state)
           3))))


(deftest seq-val-test
  (let [state {:seq 3}]
    (is (= (m/seq-val state)
           3))))


(deftest viz-defaults
  (let [state {:config c/default-config}]
    (is (= (m/screen-defaults :tda state)
           {:force-layout :force-atlas2}))))


(deftest add-graph-test
  (let [old-state {:seq 1
                   :graphs {}
                   :config c/default-config}
        new-state (m/add-graph nil old-state)]
    (is (= new-state
           {:seq 2
            :graphs {1 {:force-layout :force-atlas2}}
            :config c/default-config}))))


(deftest drop-graph-test
  (let [old-state {:seq 3
                   :graphs {1 {}
                            2 {}}}
        new-state (m/drop-graph 1 old-state)]
    (is (= new-state
           {:seq 3
            :graphs {2 {}}}))))

(run-tests)