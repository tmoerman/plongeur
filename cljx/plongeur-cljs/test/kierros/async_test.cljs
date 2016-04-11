(ns kierros.async-test
  (:require [kierros.util :refer [scan]]
            [kierros.async :refer [chain]]
            [cljs.core.async :as a :refer [<! >! close! chan to-chan]]
            [cljs.test :refer-macros [deftest is testing run-tests async]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(deftest merge-chans-async
  (async done
    (let [a$ (a/to-chan [:a])
          b$ (chan)
          m$ (->> (a/merge [a$ b$])
                  (a/into []))]
      (go
        (>! b$ :b1)
        (>! b$ :b2)
        (>! b$ :b3)
        (close! b$) ;; required by (a/into) to produce a result.
        (is (= (<! m$) [:a :b1 :b2 :b3])))
      (done))))

(deftest chain-chans-async
  (async done
    (let [a$ (chan 10)
          b$ (chan 10)
          c$ (->> (chain [a$ b$])
                  (a/into []))]
      (go
        (>! a$ :a1)
        (>! b$ :b1)
        (>! a$ :a2)
        (>! a$ :a3)
        (close! b$)
        (close! a$)
        (is (= (<! c$) [:a1 :a2 :a3 :b1])))
      (done))))

(run-tests)
