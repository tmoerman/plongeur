(ns plongeur.env-test
  (:use midje.sweet)
  (:require [environ.core :refer [env]]
            [plongeur.util :refer [deep-merge]]))

(facts
  "about Plongeur env variables"

  (-> :data-path env) => "resources"

  (-> :spark-conf env read-string) => {:app-name  "Plongeur",
                                       :master     "local[*]",
                                       :properties {:spark-serializer "org.apache.spark.serializer.KryoSerializer"}}

  (deep-merge (-> :spark-conf env read-string)
              {:app-name "Trieste"
               :properties {:bla "bla"}}) => {:app-name "Trieste"
                                              :master   "local[*]"
                                              :properties {:bla "bla"
                                                           :spark-serializer "org.apache.spark.serializer.KryoSerializer"}}

  )
