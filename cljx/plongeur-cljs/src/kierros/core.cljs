(ns kierros.core
  "Cycle-flavoured core functions."
  (:require [cljs.core.async :as a :refer [<! chan close!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn sink-proxies
  "Accepts a map of drivers. Returns a map of the equivalent of Rx.ReplaySubject instance
  for each key in the driver map."
  [drivers]
  (->> drivers
       keys
       (map (fn [name] [name (chan 10)]))
       (into {})))

(defn call-drivers
  "Accepts a map of drivers and a map of sink proxies (channels).
  Returns a map of sources."
  [drivers sink-proxy-chans]
  (->> drivers
       (map (fn [[name driver]]
              (let [sink-proxy (sink-proxy-chans name)
                    source     (driver sink-proxy)]
                [name source])))
       (into {})))

(defn weld-cycle!
  "Closes the cycle between the sinks and sink-proxies."
  [sink-chans sink-proxy-chans]
  (doseq [[key sink-chan] sink-chans]
    (when sink-chan
      (a/pipe sink-chan (key sink-proxy-chans)))))

(defn run
  "Cycle.run equivalent. Accepts a main function and a map of drivers.
  Returns a shutdown function that closes the sink proxy channels."
  [main drivers]
  (let [sink-proxy-chans (sink-proxies drivers)
        source-chans     (call-drivers drivers sink-proxy-chans)
        sink-chans       (main source-chans)
        _                (weld-cycle! sink-chans sink-proxy-chans)
        close-all!       (fn [] (doseq [ch (vals sink-proxy-chans)] (close! ch)))]
    (println "cycle running")
    close-all!))