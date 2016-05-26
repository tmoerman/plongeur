(ns plongeur.sigma-test
  (:require [cljs.core.async :as a :refer [<! chan mult put! close!]]
            [cljs.test :as t :refer-macros [deftest is testing run-tests async]]
            [dommy.core :as d :refer-macros [sel sel1]]
            [plongeur.sigma :as s]
            [plongeur.view :as v])
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
  (-> id make-div append-test-section!))

(defn append-graph-containers! [ids]
  (doseq [id ids] (append-graph-container! id)))

;; Sigma creation tests

(deftest make-sigma-instance-no-container
  (clean-test-section!)
  (let [sigma-inst (s/make-sigma-instance "graph-100" nil)]
    (is (nil? sigma-inst))
    (some-> sigma-inst s/kill)))

(deftest make-sigma-instance-nil-settings
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)]
    (is (some? sigma-inst))
    (is (some? (select-1 :body :#test :#graph-100)))
    (some-> sigma-inst s/kill)))

(deftest make-sigma-instance-blue-skies
  (clean-test-section!)
  (let [id           "graph-100"
        _            (append-graph-container! id)
        settings-clj {:verbose   true
                      :immutable true}
        sigma-inst (s/make-sigma-instance id settings-clj)]
    (is (some? sigma-inst))
    (is (some? (select-1 :body :#test :#graph-100)))
    (some-> sigma-inst s/kill)))

;; Sigma API tests

(defn ->node [id]
  {:id id})

(defn ->edge [id from to]
  {:id id
   :source from
   :target to})


;; nodes

(deftest get-nodes-none
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)
        nodes      (s/nodes sigma-inst)]
    (is (= [] nodes))))

(deftest get-nodes-one
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)
        _          (-> sigma-inst
                       (s/add-node {:id 1}))
        nodes      (s/nodes sigma-inst)]
    (is (= 1 (count nodes)))))

(deftest get-nodes-many
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)
        _          (-> sigma-inst
                       (s/add-node {:id 1})
                       (s/add-node {:id 2})
                       (s/add-node {:id 3}))
        nodes      (s/nodes sigma-inst)]
    (is (= 3 (count nodes)))))

;; edges

(deftest get-edges-none
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)
        edges      (s/edges sigma-inst)]
    (is (= [] edges))))

(deftest get-edges-many
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (-> (s/make-sigma-instance id nil)
                       (s/add-node {:id 1})
                       (s/add-node {:id 2})
                       (s/add-node {:id 3})
                       (s/add-edge {:id 1 :source 1 :target 2})
                       (s/add-edge {:id 2 :source 1 :target 3})
                       (s/add-edge {:id 3 :source 2 :target 2}))
        edges      (s/edges sigma-inst)]
    (is (= 3 (count edges)))))

;; read network

(deftest read-network
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        network    {:nodes [{:id 1}
                            {:id 2}
                            {:id 3}]
                    :edges [{:id 1 :source 1 :target 2}
                            {:id 2 :source 1 :target 3}
                            {:id 3 :source 2 :target 2}]}
        sigma-inst (-> (s/make-sigma-instance id nil)
                       (s/read network))]
    (.log js/console (clj->js network))
    (is (= 3 (-> sigma-inst s/nodes count)))
    (is (= 3 (-> sigma-inst s/edges count)))))

;; clear

(deftest clear
  (clean-test-section!)
  (let [id         "graph-100"
        _          (append-graph-container! id)
        sigma-inst (s/make-sigma-instance id nil)
        _          (-> sigma-inst
                       (s/add-node {:id 1})
                       (s/add-node {:id 2})
                       (s/add-node {:id 3})
                       (s/add-edge {:id 1 :source 1 :target 2})
                       (s/add-edge {:id 2 :source 1 :target 3})
                       (s/add-edge {:id 3 :source 2 :target 2})
                       (s/clear))]
    (is (= 0 (-> sigma-inst s/nodes count)))
    (is (= 0 (-> sigma-inst s/edges count)))))

(run-tests)