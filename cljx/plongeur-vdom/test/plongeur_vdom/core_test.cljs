(ns plongeur-vdom.core-test
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [plongeur-vdom.core :refer [people]]))

(enable-console-print!)

(deftest bla
  (is (= people ["Billy" "Bobby" "Joey"])))

(run-tests)