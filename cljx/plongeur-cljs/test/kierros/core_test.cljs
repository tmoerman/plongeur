(ns kierros.core-test
  (:require [kierros.core :as k]
            [cljs.test :refer-macros [deftest is testing run-tests]]))

(enable-console-print!)

(deftest make-sink-proxies-test
  (is (= 2 3)))

(run-tests)
