(ns plongeur.spark-driver
  (:use com.rpl.specter)
  (:use com.rpl.specter.macros)
  (:import [org.apache.spark SparkConf])
  (:require [clojure.core.async :as a :refer [<! >! chan go-loop close!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [sparkling.conf :as c]
            [sparkling.core :as s]
            [taoensso.timbre :refer [warn]]
            [plongeur.util :refer [deep-merge]]))

(def env-spark-conf
  "Spark configuration map read from environment."
  (->> :spark-conf env read-string))

(defn format-property-key
  "Accepts a string or keyword. Turns the input into a spark property key string."
  [prop-key]
  (-> prop-key name (str/replace "-" ".")))

(defn format-properties
  "Accepts a Spark configuration map, specified as an idiomatic clj data structure with keyword keys.
  Returns a map where the properties map has been transformed into a map of Spark-formatted keys to string values."
  [spark-conf-map]
  (transform [:properties ALL]
             (fn [[k v]] [(format-property-key k) (str v)])
             spark-conf-map))

(defn default+format
  "Accepts a (partial) Spark configuration map.
  Merges the specified map with the environment default map and formats the properties submap."
  [spark-conf-map]
  (->> (or spark-conf-map {}) (deep-merge env-spark-conf) format-properties))

(defn spark-conf
  "Accepts a clojure edn data structure. Returns a Spark conf instance.
  See http://spark.apache.org/docs/latest/configuration.html"
  [spark-conf-map]
  (let [{:keys [master app-name properties]} (default+format spark-conf-map)]
    (doto (SparkConf.)
          (c/master   master)
          (c/app-name app-name)
          (c/set      properties))))

(defn stop?!
  "Stops the specified SparkContext if not nil."
  [sc]
  (if sc (warn "stopping SparkContext " sc))
  (some-> sc s/stop))

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
            (stop?! sc)
            (let [new-sc (-> cfg-edn spark-conf s/spark-context)]
              (>! spark-ctx-chan new-sc)
              (recur new-sc)))
          (do
            (stop?! sc)
            (close! spark-ctx-chan))))
      spark-ctx-chan)))