(ns plongeur.spark-driver-test
  (use midje.sweet)
  (:require [clojure.core.async :as a :refer [<! <!! >! chan close! go]]
            [clojure.string :as str]
            [plongeur.spark-driver :refer :all]
            [sparkling.conf :as c]
            [sparkling.core :as s]))

(facts
  "about transforming idiomatic clj keys to spark property keys"

  (format-property-key :spark-serializer) => "spark.serializer"

  (format-property-key "spark.serializer") => "spark.serializer"

  (format-properties {:properties {:spark-prop        1
                                         "spark.test" "2"}}) => {:properties {"spark.prop" "1"
                                                                              "spark.test" "2"}}

  (default+format {}) => {:master   "local[*]"
                          :app-name "Plongeur"
                          :properties {"spark.serializer" "org.apache.spark.serializer.KryoSerializer"}}
  )

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

(facts

  "about the spark driver"

  (let [cfg-ch (chan)
        driver (make-spark-context-driver)
        sc-ch  (driver cfg-ch)
        res    (a/into [] sc-ch)]
    (go
      (>! cfg-ch {:app-name "app 1"})
      (>! cfg-ch {:app-name "app 2"})
      (>! cfg-ch {:app-name "app 3"})
      (close! cfg-ch))

    (let [scs       (<!! res)
          app-names (map #(.appName %) scs)]

      app-names => ["app 1" "app 2" "app 3"]

      (doseq [sc scs] (stop?! sc))))


  )