(ns plongeur.util
  (:require [taoensso.timbre :refer [info]])
  (:import [java.util ArrayDeque]))

(defn sliding
  "Returns a transducer like partition-all but also accepts a step argument.
  See: https://gist.github.com/nornagon/03b85fbc22b3613087f6"
  ([^long n] (sliding n 1))
  ([^long n ^long step]
   (fn [rf]
     (let [a (ArrayDeque. n)]
       (fn
         ([] (rf))
         ([result]
          (let [result (if (.isEmpty a)
                         result
                         (let [v (vec (.toArray a))]
                           ;;clear first!
                           (.clear a)
                           (unreduced (rf result v))))]
            (rf result)))
         ([result input]
          (.add a input)
          (if (= n (.size a))
            (let [v (vec (.toArray a))]
              (dorun (take step (repeatedly #(.removeFirst a))))
              (rf result v))
            result)))))))

(defn echo
  ([x] (echo "echo" x))
  ([prefix x] (prn (str prefix ": " x) x)))