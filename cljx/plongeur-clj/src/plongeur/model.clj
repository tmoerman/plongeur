(ns plongeur.model
  (:use com.rpl.specter)
  (:use com.rpl.specter.macros)
  (:require [clojure.core.async :as a :refer [<! >! chan go go-loop close!]]
            [clojure.data :refer [diff]]
            [kierros.model :refer [scan-to-states]]
            [plongeur.util :refer [sliding]]))

;; State queries
;;

(def machines-path [:machines])
(defn machine-path [id] (conj machines-path (keypath id)))

(defn machines          [state]    (select-one machines-path state))
(defn machine           [state id] (select-one (machine-path id) state))
(defn machine-out-chans [state]    (select (conj machines-path MAP-VALS (keypath :out)) state))

(def spark-ctx-path [:spark-ctx])

(defn spark-ctx [state] (select-one spark-ctx-path state))

;;

(defn diff-ops
  "Computes the difference between specified old and new sets.
  Returns a map with keys :to-remove and :to-add."
  [old new]
  (let [[only-in-old only-in-new _] (diff (set old) (set new))]
    {:removed only-in-old
     :added   only-in-new}))

(def xf*diff-ops
  "A transducer for diff-ops."
  (comp
    (sliding 2 1)
    (map (fn [v]
           (condp = (count v)
             1 nil
             2 (let [[old new] v] (diff-ops old new)))))
    (remove nil?)))

;; Intent handlers

(defn update-spark-ctx
  [spark-ctx state]

  ; TODO implement

  state)

(defn drop-machine
  [id state]
  (->> state
       (transform machines-path #(dissoc % id))))

(defn make-tda-machine
  "Create a TDA machine.
  Returns map with :in and :out channel."
  [spark-ctx]
  (let [in-chan  (chan 10)
        out-chan (chan 10)]
    {:in in-chan
     :out out-chan}))

(defn make-loop-machine
  "Make a machine that returns loops."
  []
  (let [in-chan  (chan 10)
        out-chan (chan 10)]
    {:in in-chan
     :out out-chan}))

(defn make-star-machine
  "Make a machine that returns stars."
  []
  (let [in-chan  (chan 10)
        out-chan (chan 10)]
    {:in in-chan
     :out out-chan}))

(defn make-random-machine
  []
  "Make a machine that returns random shapes (loops or stars)."
  (let [in-chan  (chan 10)
        out-chan (chan 10)]
    {:in in-chan
     :out out-chan}))

(defn make-machine
  "Create a machine of specified type."
  [type spark-ctx]
  (condp = type
    :tda    (make-tda-machine spark-ctx)
    :loop   (make-loop-machine)
    :star   (make-star-machine)
    :random (make-random-machine)))

(defn put-params!
  "Put the params to the machine in-chan.
  TODO retain a notion of the latest submitted params."
  [id params state]
  (let [{:keys [in]} (machine state id)]
    (go (>! in params)))
  state)

(defn drive-machine
  "Drive a TDA machine."
  [{:keys [id type params]} state]
  (->> state
       (transform machines-path (fn [machines-state]
                                  (if (machines-state id)
                                    machines-state
                                    (->> (spark-ctx state)
                                         (make-machine type)
                                         (assoc machines-state id)))))
       (put-params! id params)))

(defn brush-data
  [data state]
  state
  )

;; Model machinery

(def intent-handlers
  {:update-spark-ctx update-spark-ctx
   :drive-machine    drive-machine
   :brush-data       brush-data})

;; The model is a channel of applications states.

(def default-state
  "Returns a new initial application state."
  {:config   {:spark {:master   "local[*]"
                      :app-name "Plongeur"}} ;; use environment management to obtain this.
   :machines {}})


;; TODO SparkContext should be part of the state map

;; TODO incorrect: the Spark context channel must also be taken into account
(defn model
  [intent-chans]
  (scan-to-states (a/to-chan [default-state]) intent-chans intent-handlers))