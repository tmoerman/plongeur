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
                    source     (driver sink-proxy name)]
                [name source])))
       (into {})))

(defn weld-cycle!
  "Closes the cycle between the sinks and sink-proxies."
  [sink-chans sink-proxy-chans]
  (doseq [[key sink-chan] sink-chans]
    (when sink-chan
      (a/pipe sink-chan (key sink-proxy-chans)))))

(defn shutdown-channel
  "Accepts a colletion of channels. Returns the shutdown channel.
  When a signal is taken from the shutdown channel, all specified channels are closed."
  [chans]
  (let [shutdown-chan (chan 1)]
    (go
      (<! shutdown-chan)              ;; wait for the shutdown signal
      (doseq [ch chans] (close! ch))) ;; close all driver channels
    shutdown-chan))

(defn run
  "Cycle.run equivalent. Accepts a main function and a map of drivers."
  [main drivers]
  (let [sink-proxy-chans (sink-proxies drivers)
        source-chans     (call-drivers drivers sink-proxy-chans)
        sink-chans       (main source-chans)
        _                (weld-cycle! sink-chans sink-proxy-chans)]
    (println "cycle running")
    (->> sink-proxy-chans vals shutdown-channel)))
