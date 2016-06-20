(ns plongeur.machine-test
  (:use midje.sweet)
  (:require [plongeur.machine :as m]))

(facts

  "about diff-ops"

  (m/diff-ops [] [])  => {:to-add nil :to-remove nil}

  (m/diff-ops [1] [1]) => {:to-add nil :to-remove nil}

  (m/diff-ops [1 2 3] [3 2 1]) => {:to-add nil :to-remove nil}

  (m/diff-ops [] [1]) => {:to-add #{1} :to-remove nil}

  (m/diff-ops [1] []) => {:to-add nil :to-remove #{1}}

  (m/diff-ops [1 2 3] []) => {:to-add nil :to-remove #{1 2 3}}

  )

