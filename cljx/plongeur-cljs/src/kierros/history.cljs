(ns kierros.history
  (:require [cljs.core.async :as a :refer [>! chan mult mix tap admix unmix-all untap-all sliding-buffer]]
            [goog.events :as e]
            [goog.History])
  (:import [goog.history EventType])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn evt->clj
  "Turns a google history event into a clojure map."
  [e]
  {:token (.-token        e)
   :nav?  (.-isNavigation e)})

(defn init-history
  "Set up Google Closure history management."
  ;; TODO fix docs
  []
  (let [token-chan (chan 10) ; xf view-key to token here
        evt-chan   (chan 10)
        history    (doto (goog.History.)
                     (.setEnabled true)
                     (e/listen EventType.NAVIGATE #(go (->> (evt->clj %)
                                                            (>! evt-chan)))))
        evt-mult   (mult evt-chan)
        token-mix  (mix token-chan)]


    (go-loop []
             (when-let [token (<! token-chan)]
               (prn (str "received token: " token)) ;; TODO logging lib
               (.setToken history token)
               (recur)))

    [token-mix evt-mult]))

(defn connect-chans!
  "Register in and out channels to the history mix and mult."
  [[token-mix evt-mult] token-chan hist-evt-chan]
  (unmix-all token-mix)
  (admix     token-mix token-chan)
  (untap-all evt-mult)
  (tap       evt-mult hist-evt-chan))