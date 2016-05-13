(ns kierros.async-test
  (use midje.sweet)
  (:require [clojure.core.async :as async :refer [<! <!! >! >!! chan close! go timeout]]
            [kierros.async :refer :all]
            [clojure.core.async :as a]))

(facts

  "about chain"

  (let [a (chan 10)
        c (->> (chain [a]) (async/into []))]
    (>!! a :a1)
    (>!! a :a2)
    (>!! a :a3)
    (close! a)
    (<!! c)) => [:a1 :a2 :a3]

  (let [a (chan 10)
        b (chan 10)
        c (->> (chain [a b]) (async/into []))]
    (>!! a :a1)
    (>!! b :b1)
    (>!! a :a2)
    (>!! a :a3)
    (close! b)
    (close! a)
    (<!! c)) => [:a1 :a2 :a3 :b1]

  )

(facts

  "about pipe-to-atom"

  (let [ch (chan 10)
        a  (pipe-to-atom ch)
        v0 @a
        _  (>!! ch :a1)
        _  (>!! ch :a2)
        _  (>!! ch :a3)
        _  (<!! (timeout 50))
        v1 @a
        _  (>!! ch :a4)
        _  (<!! (timeout 50))
        v2 @a
        _  (close! ch)
        _  (<!! (timeout 50))
        _  (>!! ch :a5)
        v3 @a]
    [v0 v1 v2 v3]) => [nil :a3 :a4 nil]

  )

(facts

  "about combine-latest"

  (fact

    "a trivial case"

    (let [a (chan)
          combo (combine-latest [a])
          res (a/into [] combo)]
      (go
        (>! a :a1)
        (>! a :a2)
        (>! a :a3)
        (close! a))
      (<!! res)) => [[:a1] [:a2] [:a3]]

    )

  (fact

    "two input channels"

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
        (close! b))
      (<!! res)) => [[:a2 :b1]
                     [:a3 :b1]
                     [:a3 :b2]]

    )

  (fact

    "with selector function (+)"

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
        (close! b))
      (<!! res)) => [1002 1003 2003]

    )

  (fact

    "input channel closes before all input channels
    have been offered a value"

    (let [a (chan)
          b (chan)
          c (chan)
          combo (combine-latest [a b c])
          res (a/into [] combo)]
      (go
        (>! a :a1)
        (>! b :b1)
        (close! b))
      (<!! res)) => []

    )

  )