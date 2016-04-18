(ns kierros.model-test
  (:require [kierros.model :refer [scan-to-states]]
            [cljs.core.async :as a :refer [<! >! close! chan to-chan dropping-buffer]]
            [cljs.test :refer-macros [deftest is testing run-tests async]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(deftest scan-to-states-async
  (async done
    (let [init-chan (a/to-chan [{}])
          intent-chans {:foo (chan (dropping-buffer 10))
                        :gee (chan (dropping-buffer 10))
                        :bar (chan (dropping-buffer 10))}
          intent-handlers {:foo (fn [e state] (update-in state [:foo] #(conj % e)))
                           :bar (fn [e state] (update-in state [:bar] #(conj % e)))}
          states-chan (scan-to-states init-chan intent-chans intent-handlers)
          r (a/into [] states-chan)]
      (go
        (-> intent-chans :foo (>! :a))
        (-> intent-chans :foo (>! :b))
        (-> intent-chans :gee (>! :c))
        (-> intent-chans :bar (>! :d))
        ; close all intent channels
        (->> intent-chans vals (map close!) dorun)
        ; collect the scan result and inspect latest value
        (is (= (-> r <! last) {:foo [:b :a] :bar [:d]})))
      (done))))
