(ns plongeur-vdom.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]))

(deftest bla
  (is (= 2 1)))

(run-tests)