(ns kierros.spark-driver-test
  (use midje.sweet)
  (:require [plongeur.spark-driver :refer :all]
            [clojure.string :as str]
            [sparkling.conf :as c]
            [sparkling.core :as s]))

(facts

  "about turning an edn data structure into a SparkConf instance."

  (facts

    "verifying SparkConf debug strings"

    (->> nil
         (spark-conf)
         (c/to-string)) => (str/join ["spark.app.name=Plongeur\nspark.master=local[*]\n"
                                      "spark.serializer=org.apache.spark.serializer.KryoSerializer"])

    (->> {:master   "local[2]"
          :app-name "my app"}
         (spark-conf)
         (c/to-string)) => (str/join ["spark.app.name=my app\nspark.master=local[2]\n"
                                      "spark.serializer=org.apache.spark.serializer.KryoSerializer"])

    (->> {:master     "local[2]"
          :app-name   "my app"
          :properties {:spark-driver-cores    2
                       :spark.executor.memory "4g"}}
         (spark-conf)
         (c/to-string)) => (str/join ["spark.app.name=my app\nspark.driver.cores=2\n"
                                      "spark.executor.memory=4g\nspark.master=local[2]\n"
                                      "spark.serializer=org.apache.spark.serializer.KryoSerializer"])

    )

  (facts

    "creating SparkContext smoke tests"

    (s/with-context
      sc (spark-conf nil)
      sc => truthy)

    (s/with-context
      sc (spark-conf {:master   "local[2]"
                      :app-name "my app"})
      sc => truthy)

    (s/with-context
      sc (spark-conf {:master     "local[2]"
                      :app-name   "my app"
                      :properties {:spark-driver-cores    2
                                   :spark.executor.memory "4g"}})
      sc => truthy)

    )

  )