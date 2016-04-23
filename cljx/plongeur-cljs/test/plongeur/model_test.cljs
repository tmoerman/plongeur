(ns plongeur.model-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests async]]))

(deftest fail
  (is (= 3 3)))

(run-tests)