(ns plongeur.model
  (:use com.rpl.specter)
  (:use com.rpl.specter.macros)
  (:require [clojure.core.async :as a :refer [<! >! chan go go-loop close!]]
            [clojure.data :refer [diff]]
            [kierros.model :refer [scan-to-states]]
            [plongeur.util :refer [sliding]]
            [plongeur.datagen :as g]))

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
  "Transducer for diff-ops."
  (comp
    (sliding 2 1)
    (map (fn [v]
           (condp = (count v)
             1 nil
             2 (let [[old new] v] (diff-ops old new)))))
    (remove nil?)))

(defn apply-diff-ops
  "Apply the diff-ops to the specified core.async mix."
  [diff-ops-ch mix]
  (go-loop []
    (when-let [{:keys [added removed]} (<! diff-ops-ch)]
      (do
        (doseq [ch added]   (a/admix mix ch))
        (doseq [ch removed] (a/unmix mix ch))
        (recur)))))

;; Intent handlers

(defn machine-response
  "Creates a machine response"
  [id type payload]
  {:push-type :machine-response
   :id        id
   :type      type
   :payload   payload})

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
  [id spark-ctx]
  (let [in-chan  (chan 10)
        out-chan (chan 10)]
    {:in in-chan
     :out out-chan}))

(defn make-loop-machine
  "Make a machine that returns loops."
  [id]
  (let [ch (chan 10 (map (fn [{:keys [size]}]
                           (->> (g/make-loop size)
                                (machine-response id :graph)))))]
    {:in  ch
     :out ch}))

(defn make-star-machine
  "Make a machine that returns stars."
  [id]
  (let [ch (chan 10 (map (fn [{:keys [nr-arms arm-size]}]
                           (->> (g/make-star nr-arms arm-size)
                                (machine-response id :graph)))))]
    {:in  ch
     :out ch}))

(defn make-random-machine
  [id]
  "Make a machine that returns random shapes (loops or stars)."
  (let [ch (chan 10 (map (fn [_]
                           (->> (g/make-shape)
                                (machine-response id :graph)))))]
    {:in  ch
     :out ch}))

(defn make-machine
  "Create a machine of specified type."
  [id type spark-ctx]
  (condp = type
    :tda    (make-tda-machine id spark-ctx)
    :loop   (make-loop-machine id)
    :star   (make-star-machine id)
    :random (make-random-machine id)))

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
                                         (make-machine id type)
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