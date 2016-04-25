(ns plongeur.sigma-driver-test
  (:require [plongeur.sigma-driver :as s]
            [cljs.core.async :as a :refer [<! chan mult]]
            [cljs.test :refer-macros [deftest is testing run-tests async]]
            [dommy.core :as d :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; dommy helpers

(defn make-el [tag id]
  (-> (d/create-element tag)
      (d/set-attr! "id" id)))

(defn make-div     [id] (make-el "div" id))
(defn make-section [id] (make-el "section" id))

(defn select-1 [& args] (sel1 js/document args))

(defn append-body! [node]
  (-> (sel1 js/document :body)
      (d/append! node)))

(defn clean-test-section! []
  (if-let [test-section (sel1 js/document [:body :#test])]
    (-> test-section d/clear!)
    (-> "test" make-section append-body!)))

(defn append-test-section! [node]
  (-> (sel1 js/document [:body :#test])
      (d/append! node)))

(defn append-graph-container! [id] (-> id s/container-id make-div append-test-section!))

;; new-sigma tests

(deftest new-sigma-no-container
  (clean-test-section!)
  (let [sigma-inst (s/new-sigma 1 nil)]
    (is (nil? sigma-inst))
    (some-> sigma-inst .kill)))

(deftest new-sigma-nil-settings
  (clean-test-section!)
  (let [id         1
        _          (append-graph-container! id)
        sigma-inst (s/new-sigma id nil)]
    (is (some? sigma-inst))
    (is (some? (select-1 :body :#test :#graph-1)))
    (some-> sigma-inst .kill)))

(deftest new-sigma-with-settings
  (clean-test-section!)
  (let [id         1
        _          (append-graph-container! id)
        settings   (clj->js {:edgeColor "default"})
        sigma-inst (s/new-sigma id settings)]
    (is (some? sigma-inst))
    (some-> sigma-inst .kill)))

;; make-sigma-context

(deftest make-sigma-context-no-container
  (clean-test-section!)
  (let [id        1
        in-mult   (mult (chan))
        out-chan  (chan)
        sigma-ctx (s/make-sigma-context in-mult out-chan {} id)]
    (is (nil? sigma-ctx))
    (some-> sigma-ctx s/dispose)))




#_(deftest make-sigma-driver-test
    (async done
      (let [sigma-driver-fn (sig/make-sigma-driver)
            sigma-in-chan   (chan)
            sigma-out-chan  (sigma-driver-fn sigma-in-chan)]
        (go
          (>! sigma-in-chan {:sync :TODO})
          (<! (a/timeout 1000))))))