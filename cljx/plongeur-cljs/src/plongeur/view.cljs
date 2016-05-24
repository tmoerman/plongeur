(ns plongeur.view
  (:require [cljs.core.async :as a :refer [<! >! tap chan pipe close!]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [plongeur.model :as m]
            [plongeur.sigma :as s])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn rand-node [id]
  (let [node-id (str (rand-int 10))]
    {:id    node-id
     :label node-id
     :x (rand-int 20)
     :y (rand-int 20)
     :size 0.5
     :color "#FF0"}))

(defn add-node
  [sigma-inst node]
  (try
    (-> sigma-inst .-graph (.addNode node))
    (catch :default _ nil)))

;; MDL machinery

(defn upgrade-mdl-components
  "Material Design Lite component upgrade handler."
  []
  (js/setInterval #(.upgradeDom js/componentHandler) 100))

;; React components

(let [sigma-state (atom {})]
  (defcomponent Sigma
    "Component on which the Sigma canvas is mounted."
    :on-mount (fn [node [state id props] {:keys [web-response-mult] :as cmd-chans}]
                (let [sigma-instance   (s/make-sigma-instance-with-events id (m/sigma-settings state) cmd-chans)
                      web-response-tap (->> (chan 10)
                                            (tap web-response-mult))]
                  #_(prn (str "on-mount " id))
                  (go-loop []
                           (when-let [v (<! web-response-tap)]
                             (do
                               #_(prn (str "captured in Sigma " id ": " v))
                               (->> (rand-node id)
                                    (clj->js)
                                    (add-node sigma-instance))
                               (.refresh sigma-instance)
                               (recur))))

                  (swap! sigma-state assoc id
                         {:sigma sigma-instance
                          :ch    web-response-tap})))

    :on-unmount (fn [node [state id props] {:keys [web-response-mult] :as cmd-chans}]
                  #_(prn (str "on-unmount " id))
                  (swap! sigma-state
                         (fn [m]
                           (->> id m :ch (a/untap web-response-mult))
                           (->> id m :ch close!)
                           (->> id m :sigma s/kill)
                           (dissoc m id))))
    [[state id props] cmd-chans]
    (html [:div {:id         (s/graph-id id)
                 :class-name "mdl-card__supporting-text sigma-graph"}])))

(defcomponent Card
  "Component surrounding the visualization containers."
  :keyfn (fn [[state id props]] id)
  [[state id props] {:keys [drop-graph] :as cmd-chans}]
  (html [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
         [:div {:class-name "mdl-card mdl-shadow--2dp"}

          #_[:div {:class-name "mdl-card__supporting-text"}
           (Sigma [state id props] cmd-chans)]

          (Sigma [state id props] cmd-chans)

          [:div {:class-name "mdl-card__title"}
           [:button {:on-click   #(go (>! drop-graph id))
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"} "delete " id]]]]))

(defcomponent Header
  [state cmd-chans]
  (html [:header {:class-name "mdl-layout__header"}
         [:div {:class-name "mdl-layout__header-row"}
          [:div {:class-name "mdl-layout-spacer"}]

          ]]))

(defcomponent Drawer
  [state {:keys [add-graph] :as cmd-chans}]
  (html [:div {:class-name "mdl-layout__drawer"}
         [:header {} "Plongeur"]
         [:nav {:class-name "mdl-navigation"}
          [:a {:class-name "mdl-navigation__link mdl-navigation__link--current"
               :href "index.html"}
           [:i {:class-name "material-icons"
                :role       "presentation"} "dashboard"] "Dashboard"]
          [:a {:class-name "mdl-navigation__link"
               :href "config.html"}
           [:i {:class-name "material-icons"
                :role       "presentation"} "settings"] "Settings"]
          [:a {:class-name "mdl-navigation__link"
               :href "config.html"}
           [:i {:class-name "material-icons"
                :role       "presentation"} "reorder"] "Logs"]
          [:button {:on-click   #(go (>! add-graph :click))
                    :hidden     (>= (m/graph-count state) 4)
                    :class-name "mdl-button mdl-js-button mdl-button--fab mdl-button--colored"}
           [:i {:class-name "material-icons"} "add"]]

          [:div {:class-name "mdl-layout-spacer"}]

          [:a {:class-name "mdl-navigation__link"
               :href "https://github.com/tmoerman/plongeur"
               :target "_blank"}
           [:i {:class-name "material-icons"
                :role       "presentation"} "link"] "Github"]

          ]]))

(defcomponent Grid
  [state cmd-chans]
  (html [:main {:class-name "mdl-layout__content"}
         [:div {:class-name "mdl-grid mdl-grid--no-spacing"}
          [:div {:class-name "mdl-grid mdl-cell mdl-cell--9-col-desktop mdl-cell--12-col-tablet mdl-cell--4-col-phone mdl-cell--top"}
           (for [[id props] (m/graphs state)]
             (Card [state id props] cmd-chans))]]]))

(defcomponent Root
  [state cmd-chans]
  (html [:div {:id "plongeur-main"}
         [:div {:id         "layout"
                :class-name "mdl-layout mdl-js-layout mdl-layout--fixed-drawer mdl-layout--fixed-header is-small-screen"}
          (Header state cmd-chans)
          (Drawer state cmd-chans)
          (Grid   state cmd-chans)]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan cmd-chans]
  (->> (fn [state] (Root state cmd-chans)) ; fn
       (map)                               ; xf
       (chan 10)                           ; ch
       (pipe states-chan)))
