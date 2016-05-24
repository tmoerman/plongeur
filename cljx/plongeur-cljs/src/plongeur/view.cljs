(ns plongeur.view
  (:require [cljs.core.async :as a :refer [>! chan pipe close!]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [plongeur.model :as m]
            [plongeur.sigma :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; MDL machinery

(defn upgrade-mdl-components
  "Material Design Lite component upgrade handler."
  []
  (js/setInterval #(.upgradeDom js/componentHandler) 100))

;; React components

(let [sigma-state (atom {})]
  (defcomponent Sigma
    :keyfn (fn [[id _]] id)

    :on-mount (fn [node [id _] {:keys [sigma-ctx]}]
                (prn "on-mount" @sigma-state)

                (swap! sigma-state assoc id {:ch (chan 10)})

                ;(go (>! update-sigma [id cmd-chans]))
                )

    :on-unmount (fn [_ [id _] {:keys [sigma-ctx]}]
                  (prn "on-unmount" @sigma-state)

                  (swap! sigma-state
                         (fn [m]
                           (-> id m :ch close!)
                           (dissoc m id)))

                  ;(go (>! remove-sigma id))
                  )
    [[id props] cmd-chans]
    (html [:div {:id         (s/graph-id id)
                 :class-name "graph"}])))

(defcomponent Card
  "Component surrounding the visualization containers."
  [[id props] {:keys [drop-graph] :as cmd-chans}]
  (html [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
         [:div {:class-name "mdl-card mdl-shadow--2dp"}

          [:div {:class-name "mdl-card__supporting-text"}
           (Sigma [id props] cmd-chans)]

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
           (for [graph-state (m/graphs state)]
             (Card graph-state cmd-chans))]]]))

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


#_(def node-1 {:id "n1"
               :label "hello"
               :x 10
               :y 10
               :size 1
               :color "#FF0"})