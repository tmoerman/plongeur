(ns plongeur.model-test
  (:use midje.sweet)
  (:require [clojure.core.async :as a :refer [<! >! chan go go-loop close!]]
            [plongeur.model :as m]))

(facts
  "about diff-ops"

  (m/diff-ops [] [])           => {:added nil :removed nil}

  (m/diff-ops [1] [1])         => {:added nil :removed nil}

  (m/diff-ops [1 2 3] [3 2 1]) => {:added nil :removed nil}

  (m/diff-ops [] [1])          => {:added #{1} :removed nil}

  (m/diff-ops [1] [])          => {:added nil :removed #{1}}

  (m/diff-ops [1 2 3] [])      => {:added nil :removed #{1 2 3}}

  )

(facts
  "about machine state queries"

  (let [a (chan) b (chan) c (chan) d (chan) x (chan)

        state-1 {:machines {1 {:in a :out b}
                            2 {:in c :out d}}}

        state-2 {:machines {1 {:in a :out b}
                            2 {:in c :out x}}}]

    (m/machine state-1 1) => {:in a :out b}

    (m/machine-out-chans state-1) => [b d]

    (m/diff-ops (m/machine-out-chans state-1)
                (m/machine-out-chans state-2)) => {:added #{x} :removed #{d}}

    ))

(facts
  "about driving machines"

  (let [config-1 {:id 1
                  :type :tda
                  :params {:bla 100}}
        config-2 {:id 2
                  :type :tda
                  :params {:bla 200}}
        state  {:machines {2 {:in (chan) :out (chan)}}}]

    (let [updated-state (m/drive-machine config-1 state)]

      (get-in updated-state [:machines 1]) => (just {:in anything :out anything})

      (-> updated-state :machines keys set) => #{1 2}

      )

    (m/drive-machine config-2 state) => state

    ))

(facts
  "about xf*diff-ops transducer"

  (->> [[] [1] [1 2] [1 3]]
       (sequence m/xf*diff-ops)) => [{:added #{1} :removed nil}
                                     {:added #{2} :removed nil}
                                     {:added #{3} :removed #{2}}]

  )