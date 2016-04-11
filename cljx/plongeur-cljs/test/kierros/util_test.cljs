(ns kierros.util-test
  "Unit tests for the scan transducer."
  (:require [kierros.util :refer [scan]]
            [cljs.core.async :as a :refer [<! >! close! chan]]
            [cljs.test :refer-macros [deftest is testing run-tests async]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def amend-state (fn [state f] (f state)))

(deftest sequence-scan-empty
  (let [s (->> []
               (sequence (scan amend-state)))]
    (is (= s []))))

(deftest sequence-scan-singleton
  (let [s (->> [{:key 1}]
               (sequence (scan amend-state)))]
    (is (= s [{:key 1}]))))

(deftest sequence-scan-one-amendment
  (let [s (->> [{:key 1}
                #(assoc % :foo 2)]
               (sequence (scan amend-state)))]
    (is (= s [{:key 1} {:key 1 :foo 2}]))))

(deftest sequence-scan-noops
  (let [s (->> [{:key 1} identity identity identity]
               (sequence (scan amend-state)))]
    (is (= s [{:key 1} {:key 1} {:key 1} {:key 1}]))))

(def init+amendments [{:key 1} #(assoc % :foo 2) #(assoc % :key "one")])
(def expected-states [{:key 1} {:key 1 :foo 2} {:key "one" :foo 2}])

(deftest scan-amendments
  (let [s (sequence (scan amend-state) init+amendments)]
    (is (= s expected-states))))

(deftest scan-amendments-async
  (async done
    (let [c (chan 1 (scan amend-state))
          _ (a/onto-chan c init+amendments)
          r (a/into [] c)]
      (go
        (is (= (<! r) expected-states))
        (done)))))

(run-tests)
