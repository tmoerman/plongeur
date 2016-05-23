(ns plongeur.sigma-test
  (:require [cljs.core.async :as a :refer [<! chan mult put! close!]]
            [cljs.test :as t :refer-macros [deftest is testing run-tests async]]
            [dommy.core :as d :refer-macros [sel sel1]]
            [plongeur.sigma :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; dommy helpers

(defn make-el [tag id]
  (-> (d/create-element tag)
      (d/set-attr! "id" id)))

(defn make-div     [id] (make-el "div" id))
(defn make-section [id] (make-el "section" id))

(defn select-1 [& args] (sel1 js/document args))

(defn append-body! [node]
  (-> (select-1 :body)
      (d/append! node)))

(defn clean-test-section! []
  (if-let [test-section (select-1 :body :#test)]
    (-> test-section d/clear!)
    (-> "test" make-section append-body!)))

(defn append-test-section!
  [node]
  (let [test-section (select-1 :body :#test)]
    (d/append! test-section node)))

(defn append-graph-container! [id]
  (-> id s/graph-id make-div append-test-section!))

(defn append-graph-containers! [ids]
  (doseq [id ids] (append-graph-container! id)))






(run-tests)