(ns kierros.async-test
  (:require [kierros.util :refer [scan]]
            [kierros.async :as ka :refer [chain combine-latest]]
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

(deftest close-chan
  (async done
    (let [c (chan)
          a (atom)
          r (go
              (<! c)
              (swap! a (fn [_] :token)))]
      (go
        (close! c)
        (<! r)
        (is (= :token @a)))
      (done))))

(testing "combine-latest"

  (deftest trivial-case
    (async done
      (let [a (chan 10)
            combo (combine-latest [a])
            res (a/into [] combo)]
        (go
          (>! a :a1)
          (>! a :a2)
          (>! a :a3)
          (close! a)
          (is (= (<! res) [[:a1] [:a2] [:a3]])))
        (done))))

  (deftest two-input-channels
    (async done
      (let [a (chan)
            b (chan)
            combo (combine-latest [a b])
            res (a/into [] combo)]
        (go
          (>! a :a1)
          (>! a :a2)
          (>! b :b1)
          (>! a :a3)
          (>! b :b2)
          (close! b)
          (is (= (<! res) [[:a2 :b1] [:a3 :b1] [:a3 :b2]])))
        (done))))

  (deftest with-selector-function
    (async done
      (let [a (chan)
            b (chan)
            combo (combine-latest + [a b])
            res (a/into [] combo)]
        (go
          (>! a 1)
          (>! a 2)
          (>! b 1000)
          (>! a 3)
          (>! b 2000)
          (close! b)
          (is (= (<! res) [1002 1003 2003])))
        (done))))

  (deftest input-channel-closes
    (async done
      (let [a (chan)
            b (chan)
            c (chan)
            combo (combine-latest [a b c])
            res (a/into [] combo)]
        (go
          (>! a :a1)
          (>! b :b1)
          (close! b)
          (is (= (<! res) [])))
        (done))))

  ;; deftest hit-me-baby

  )

(run-tests)