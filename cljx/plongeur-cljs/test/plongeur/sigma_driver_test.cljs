(ns plongeur.sigma-driver-test
  (:require [plongeur.sigma-driver :as s]
            [cljs.core.async :as a :refer [<! chan mult put! close!]]
            [cljs.test :as t :refer-macros [deftest is testing run-tests async]]
            [dommy.core :as d :refer-macros [sel sel1]])
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
  (-> id s/container-id make-div append-test-section!))

(defn append-graph-containers! [ids]
  (doseq [id ids] (append-graph-container! id)))

;; new-sigma tests

(deftest new-sigma-no-container
  (clean-test-section!)
  (let [sigma-inst (s/new-sigma 100 nil)]
    (is (nil? sigma-inst))
    (some-> sigma-inst .kill)))

(deftest new-sigma-nil-settings
  (clean-test-section!)
  (let [id         100
        _          (append-graph-container! id)
        sigma-inst (s/new-sigma id nil)]
    (is (some? sigma-inst))
    (is (some? (select-1 :body :#test :#graph-100)))
    (some-> sigma-inst .kill)))

(deftest new-sigma-blue-skies
  (clean-test-section!)
  (let [id         100
        _          (append-graph-container! id)
        settings   (clj->js {:edgeColor "default"})
        sigma-inst (s/new-sigma id settings)]
    (is (some? sigma-inst))
    (some-> sigma-inst .kill)))

;; make-sigma-context

(deftest make-sigma-context-no-container
  (clean-test-section!)
  (let [id        100
        sigma-ctx (s/make-sigma-context (mult (chan)) (chan) {} id)]
    (is (nil? sigma-ctx))
    (s/dispose! sigma-ctx)))

(deftest make-sigma-context-nil-options
  (clean-test-section!)
  (let [id        100
        _         (append-graph-container! id)
        sigma-ctx (s/make-sigma-context (mult (chan)) (chan) nil id)]
    (is (some? sigma-ctx))
    (s/dispose! sigma-ctx)))

(deftest make-sigma-context-blue-skies
  (clean-test-section!)
  (let [id        100
        _         (append-graph-container! id)
        options   {:sigma-settings {:edgeColor "default"}}
        sigma-ctx (s/make-sigma-context (mult (chan)) (chan) options id)]
    (is (some? sigma-ctx))
    (s/dispose! sigma-ctx)))

;; remove renderers

(deftest remove-renderers-blue-skies
  (clean-test-section!)
  (let [ids        [101 102 103]
        _          (doseq [id ids] (append-graph-container! id))
        state-atom (atom {})]
    (s/update-renderers state-atom (mult (chan)) (chan) s/default-options ids)
    (s/remove-renderers state-atom [101 103])
    (is (= [102] (s/sigma-ids @state-atom)))))

;; update renderer

(deftest update-renderer-no-container
  (clean-test-section!)
  (let [id        100
        old-state {}
        new-state (s/update-renderer old-state (mult (chan)) (chan) s/default-options id)]
    (is (-  old-state new-state))))

(deftest update-renderer-blue-skies
  (clean-test-section!)
  (let [id        100
        _         (append-graph-container! id)
        new-state (s/update-renderer {} (mult (chan)) (chan) s/default-options id)
        sigma-ctx (new-state id)]
    (is (some? sigma-ctx))
    (s/dispose! sigma-ctx)))

;; update renderers

(deftest update-renderers-missing-container
  (clean-test-section!)
  (let [ids        [101 103]
        _          (append-graph-containers! ids)
        sync-ids   [101 102 103]
        state-atom (atom {})]
    (s/update-renderers state-atom (mult (chan)) (chan) s/default-options sync-ids)
    (is (= ids (s/sigma-ids @state-atom)))
    (s/dispose-all! @state-atom)))

(deftest update-renderers-blue-skies
  (clean-test-section!)
  (let [ids        [1 2 3]
        _          (append-graph-containers! ids)
        state-atom (atom {})]
    (s/update-renderers state-atom (mult (chan)) (chan) s/default-options ids)
    (is (= ids (s/sigma-ids @state-atom)))
    (s/dispose-all! @state-atom)))

;; sync renderers

(deftest sync-renderers-empty-state
  (clean-test-section!)
  (let [ids         [101 102 103]
        _           (append-graph-containers! ids)
        state-atom  (atom {})]
    (s/sync-renderers state-atom (mult (chan)) (chan) s/default-options ids)
    (is (= ids (s/sigma-ids @state-atom)))
    (s/dispose-all! @state-atom)))

#_(deftest sync-renderers-delta-state
  (clean-test-section!)
  (let [ids        [101 102 103]
        _          (append-graph-containers! ids)
        state-atom (atom {})
        _          (s/sync-renderers state-atom (mult (chan)) (chan) s/default-options ids)
        ])
  )

;; sigma driver

#_(deftest make-sigma-driver-test
    (async done
      (let [sigma-driver-fn (sig/make-sigma-driver)
            sigma-in-chan   (chan)
            sigma-out-chan  (sigma-driver-fn sigma-in-chan)]
        (go
          (>! sigma-in-chan {:sync :TODO})
          (<! (a/timeout 1000))))))