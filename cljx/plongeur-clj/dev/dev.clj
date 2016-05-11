(ns dev
  (:require [clojure.core.async :as a]
            [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [plongeur.core :as plongeur]
            [plongeur.util :as u]
            [taoensso.timbre :as t :refer [info]]))

(def system nil)

(defn shutdown [system] (when-let [stop-fn (some-> system :SHUTDOWN)] (stop-fn)))

(defn cycle-off-on []
  (alter-var-root #'system (fn [system]
                             (shutdown system)
                             (plongeur/launch-server))))

(defn stop []
  (alter-var-root #'system (fn [system]
                             (shutdown system)))
  :stopped)

(defn reset [] (refresh :after 'dev/cycle-off-on))