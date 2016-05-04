(ns dev
  (:require [clojure.tools.namespace.repl :refer [refresh set-refresh-dirs]]
            [plongeur.core :as plongeur]))

(def system nil)

(defn cycle-off-on []
  (alter-var-root #'system (fn [shutdown!]
                             (when-let [f shutdown!] (f))
                             (plongeur/launch-server))))

(defn stop []
  (alter-var-root #'system (fn [shutdown!]
                             (when-let [f shutdown!] (f)))))

(defn reset [] (refresh :after 'dev/cycle-off-on))