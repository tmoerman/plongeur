(ns dev
  (:require [clojure.core.async :as a]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [plongeur.core :as plongeur]
            [plongeur.util :as u]
            [taoensso.timbre :as t :refer [info warn error]]))

(def system nil)

(defn stop  [] (alter-var-root #'system (fn [sys] (plongeur/shutdown sys))))
(defn start [] (alter-var-root #'system (fn [ _ ] (plongeur/launch))))
(defn reset [] (stop) (refresh :after 'dev/start))