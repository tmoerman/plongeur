(ns plongeur.data-browser-test
  (:use midje.sweet)
  (:require [environ.core :refer [env]]
            [me.raynes.fs :as fs]
            [clojure.string :as str]
            [plongeur.data-browser :refer [data-path] :as br]))

(facts
  "about data-path environment variable"

  data-path => "resources"

  (fs/list-dir data-path) => (contains ["iris.csv", "test"])

  (br/tree) => (contains [["resources" '("test") '("iris.csv")]])

  (br/tree (str/join "/" [data-path "test"])) => (contains [["test" '("bar" "foo" "gee") nil]])


  )