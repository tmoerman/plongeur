(ns lab.async-lab
  (:require [cljs.core.async :as a :refer [<! >! close! pipe chan to-chan]]
            [cljs.test :refer-macros [deftest is testing run-tests async]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(deftest pipe-to-multiple
  "Illustration of the behaviour of the core.async pipe operator applied to multiple channels."
  (async done
    (let [a (chan 10)

          x (chan 10)
          y (chan 10)

          _ (pipe a x)
          _ (pipe a y)]

      (go
        (>! a :ping)
        (close! a)
        (is (= [(<! x) (<! y)]
               [:ping  nil]))
        (done))
      )))