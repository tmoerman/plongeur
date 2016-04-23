(ns plongeur.sigma-driver-test
  (:require [plongeur.sigma-driver :as s]
            [cljs.core.async :as a :refer [<! chan]]
            [cljs.test :refer-macros [deftest is testing run-tests async]]
            [dommy.core :as d :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn append-to-dom [node]
  (d/append! (sel1 js/document :body) node))

(defn make-graph-node [id]
  (-> (d/create-element "div")
      (d/set-attr! "id" (s/container-id id))))

(deftest new-sigma-renderer-nil-settings
  (let [id 1
        _  (-> id make-graph-node append-to-dom)
        s  (s/new-sigma-renderer id nil)]
    (prn s)
    ))

#_(deftest make-sigma-driver-test
    (async done
      (let [sigma-driver-fn (sig/make-sigma-driver)
            sigma-in-chan   (chan)
            sigma-out-chan  (sigma-driver-fn sigma-in-chan)]
        (go
          (>! sigma-in-chan {:sync :TODO})
          (<! (a/timeout 1000))))))