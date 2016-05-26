(ns plongeur.model-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async]]
            [plongeur.model :as m]
            [plongeur.config :as c]))

(deftest plot-ids-test
  (let [state {:plots {1  {:k :v}
                       2 {:k :v}
                       3 {:k :v}}}]
    (is (= (m/plot-ids state)
           [1 2 3]))))


(deftest plot-count-test
  (let [state {:plots {1  {:k :v}
                       2 {:k :v}
                       3 {:k :v}}}]
    (is (= (m/plot-count state)
           3))))


(deftest seq-val-test
  (let [state {:seq 3}]
    (is (= (m/seq-val state)
           3))))


(deftest defaults
  (let [state {:config c/default-config}]
    (is (= (m/defaults :tda state)
           {:force-layout :force-atlas2}))))


(deftest add-plot-test
  (let [old-state {:seq    1
                   :plots  {}
                   :config c/default-config}
        new-state (m/add-plot :tda old-state)]
    (is (= new-state
           {:seq    2
            :plots  {1 {:tda  {:force-layout :force-atlas2}
                        :data nil}}
            :config c/default-config}))))


(deftest drop-plot-test
  (let [old-state {:seq   3
                   :plots {1  {}
                           2 {}}}
        new-state (m/drop-plot 1 old-state)]
    (is (= new-state
           {:seq   3
            :plots {2 {}}}))))


(run-tests)