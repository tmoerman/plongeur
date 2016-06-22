(ns plongeur.util-test
  (:use midje.sweet)
  (:require [plongeur.util :refer [deep-merge] :as u]))

(facts
  "about deep merging"

  (deep-merge nil) => nil

  (deep-merge 1) => 1

  (deep-merge 1 2) => 2

  (deep-merge [1] [2]) => [1 2]

  (deep-merge {:foo 1} {:bar 2}) => {:foo 1 :bar 2}

  (deep-merge {:key :old} {:key :new}) => {:key :new}

  (deep-merge {:foo {:gee 1}} {:foo {:gee 2}}) => {:foo {:gee 2}}

  (deep-merge {:foo {:gee 1}} {:foo {:bar 2}}) => {:foo {:gee 1
                                                         :bar 2}}
  (deep-merge {:foo {:gee "gee"}}
              {:foo {:bar "bar"}}) => {:foo {:gee "gee"
                                             :bar "bar"}}

  (deep-merge {:foo {:gee [1]}} {:foo {:gee [2]}}) => {:foo {:gee [1 2]}}

  )

