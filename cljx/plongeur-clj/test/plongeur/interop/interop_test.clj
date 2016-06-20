(ns plongeur.interop.interop-test
  (:use midje.sweet)
  (:require [t6.from-scala.core :refer ($ $$) :as $])
  (:import (org.tmoerman.plongeur.tda TDA)
           (scala.collection.immutable List Set)
           (clojure.lang ExceptionInfo)))

(facts

  "about chain"

  ($ ($ List & 1 2 3 4)
     reduce
     ($/function [x y] (+ x y))) => 10

  ($ TDA/echo "hello") => "hello hello"

  )