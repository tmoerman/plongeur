(ns plongeur.spark-driver
  (:import [org.apache.spark SparkConf])
  (:require [clojure.core.async :as a :refer [<! >! chan go-loop close!]]
            [clojure.string :as str]
            [sparkling.conf :as c]
            [sparkling.core :as s]))

(defn prop-str
  "Accepts a string or keyword. Turns the input into a spark property key string."
  [s-or-kw]
  (assert (or (string? s-or-kw)
              (keyword? s-or-kw)))
  (-> (if (keyword? s-or-kw) (-> s-or-kw str (subs 1)) s-or-kw)
      (str/replace "-" ".")))

(defn str-str-map
  "Turn the idiomatic clojure edn map into a [String, String] map."
  [properties]
  (->> properties
       (map (fn [[k v]] [(prop-str k) (str v)]))
       (into {})))

(defn spark-conf
  "Accepts a clojure edn data structure. Returns a Spark conf instance.
  See http://spark.apache.org/docs/latest/configuration.html"
  [{:keys [master app-name properties]}]
  (as-> (SparkConf.) c
        (c/set c "spark.serializer" "org.apache.spark.serializer.KryoSerializer")
        (c/master c (or master "local[*]"))
        (c/app-name c (or app-name "Plongeur"))
        (c/set c (str-str-map properties))))

(defn make-spark-context-driver
  "Makes a Spark driver.
  The driver accepts a channel of edn configuration data structures and
  returns a channel of SparkContext instances."
  ;; TODO: invent a test where the SparkContext is stopped with computations in flight.
  []
  (fn [cfg-chan]
    (let [spark-ctx-chan (chan)]
      (go-loop [sc nil]
        (if-let [cfg-edn (<! cfg-chan)]
          (do
            (some-> sc s/stop)
            (let [new-sc (-> cfg-edn spark-conf s/spark-context)]
              (>! spark-ctx-chan new-sc)
              (recur new-sc)))
          (close! spark-ctx-chan)))
      spark-ctx-chan)))