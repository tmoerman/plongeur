(ns kierros.async-test
  (use midje.sweet)
  (:require [clojure.core.async :as async :refer [<! <!! >! >!! chan close! go]]
            [kierros.async :refer :all]))

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
        _  (<!! (async/timeout 50))
        v1 @a
        _  (>!! ch :a4)
        _  (<!! (async/timeout 50))
        v2 @a
        _  (close! ch)
        _  (<!! (async/timeout 50))
        _  (>!! ch :a5)
        v3 @a]
    [v0 v1 v2 v3]) => [nil :a3 :a4 :a4]

  )