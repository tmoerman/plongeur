(ns kierros.sente-server-diver-test
  (use midje.sweet)
  (:require [clojure.core.async :as a :refer [<! <!! >! chan close!]]
            [kierros.sente-server-driver :refer :all]))

(defn pause
  ([] (pause 100))
  ([n] (<!! (a/timeout n))))

(facts

  "about the sente server driver"

  (facts

    "init smoke tests"

    (make-sente-server-driver) => truthy
    (make-sente-server-driver {}) => truthy
    (make-sente-server-driver {:sente {}}) => truthy
    (make-sente-server-driver {:http-kit {}}) => truthy

    )

  (facts

    "driver call smoke tests"

    (let [driver      (make-sente-server-driver)
          sink-proxy  (chan)
          _           (driver sink-proxy)]
      (close! sink-proxy) (pause)) => nil

    (let [driver      (make-sente-server-driver {})
          sink-proxy  (chan)
          _           (driver sink-proxy)]
      (close! sink-proxy) (pause)) => nil

    (let [driver      (make-sente-server-driver {:sente {}})
          sink-proxy  (chan)
          _           (driver sink-proxy)]
      (close! sink-proxy) (pause)) => nil

    (let [driver      (make-sente-server-driver {:http-kit {:port 3000}})
          sink-proxy  (chan)
          _           (driver sink-proxy)]
      (close! sink-proxy) (pause)) => nil

    )

  (facts

    "calling two sente drivers on different ports"

    (let [driver-0     (make-sente-server-driver {:http-kit {:port 3000}})
          sink-proxy-0 (chan)
          _            (driver-0 sink-proxy-0)
          driver-1     (make-sente-server-driver {:http-kit {:port 3001}})
          sink-proxy-1 (chan)
          _            (driver-1 sink-proxy-1)]
      (close! sink-proxy-0)
      (close! sink-proxy-1)
      (pause)) => nil

    )

  )