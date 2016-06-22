(ns plongeur.data-browser
  (:require [environ.core :refer [env]]
            [me.raynes.fs :as fs]))

(def data-path (env :data-path))

(defn tree
  ([]
   (tree data-path))
  ([data-path]
   (->> data-path
        (fs/walk (fn [root dirs files]
                   [(.getName root)
                    (some-> dirs seq sort)
                    (some-> files seq sort)])))))