(ns plongeur.view
  (:require [cljs.core.async :as a :refer [>! chan pipe]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [plongeur.model :as m]
            [plongeur.sigma-driver :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn upgrade-mdl-components
  "Material Design Lite component upgrade handler."
  []
  (js/setInterval #(.upgradeDom js/componentHandler) 100))

(def node-1 {:id "n1"
             :label "hello"
             :x 10
             :y 10
             :size 1
             :color "#FF0"})

(defn graph-id [id] (str "graph-" id))

(defcomponent Sigma
  :keyfn      (fn [[id _]] id)
  :on-mount   (fn [_ [id _] {:keys [sigma-ctrl]}] (go (>! sigma-ctrl (s/ctrl-update id))))
  :on-unmount (fn [_ [id _] {:keys [sigma-ctrl]}] (go (>! sigma-ctrl (s/ctrl-remove id))))
  [[id props] {:keys [drop-graph] :as intent-chans}]
  (html [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
         [:div {:class-name "mdl-card mdl-shadow--2dp"}

          [:div {:class-name "mdl-card__supporting-text"}
           [:div {:id (graph-id id)
                  :class-name "graph"}]]

          [:div {:class-name "mdl-card__title"}
           [:button {:on-click   #(go (>! drop-graph id))
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"} "delete " id]]

          ]]))

(defcomponent Root
  [state {:keys [add-graph debug] :as intent-chans}]

  (html [:div {:id "plongeur-main"} ;; extra wrapper for MDL React bug

         [:div {:id         "layout"
                :class-name "mdl-layout mdl-js-layout mdl-layout--fixed-drawer mdl-layout--fixed-header is-small-screen"}

          ;; HEADER

          [:header {:class-name "mdl-layout__header"}
           [:div {:class-name "mdl-layout__header-row"}
            [:div {:class-name "mdl-layout-spacer"}]


            ]]

          ;; DRAWER

          [:div {:class-name "mdl-layout__drawer"}
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

            [:div {:class-name "mdl-layout-spacer"}]

            [:a {:class-name "mdl-navigation__link"
                 :href "https://github.com/tmoerman/plongeur"
                 :target "_blank"}
             [:i {:class-name "material-icons"
                  :role       "presentation"} "link"] "Github"]


            ]]

          ;; CONTENT

          [:main {:class-name "mdl-layout__content"}
           [:div {:class-name "mdl-grid mdl-grid--no-spacing"}

            [:div {:class-name "mdl-grid mdl-cell mdl-cell--9-col-desktop mdl-cell--12-col-tablet mdl-cell--4-col-phone mdl-cell--top"}

             (for [graph-state (m/graphs state)]
               (Sigma graph-state intent-chans))

             ]]]

          ]])

  #_(html [:div {:id "plongeur-main"}
         [:div {:id         "layout"
                :class-name "demo-layout mdl-layout mdl-js-layout mdl-layout--fixed-drawer mdl-layout--fixed-header"}

          [:header {:class-name "demo-header mdl-layout__header mdl-color--grey-100 mdl-color-text--grey-600"}
           [:div {:class-name "mdl-layout__header-row"}
            [:span {:class-name "mdl-layout-title"} "Home"]
            [:div {:class-name "mdl-layout-spacer"}]
            [:button {:class-name "mdl-button mdl-js-button mdl-js-ripple-effect mdl-button--icon"
                      :id         "hdrbtn"}
             [:i {:class-name "material-icons"} "more_vert"]]
            [:ul {:class-name "mdl-menu mdl-js-menu mdl-js-ripple-effect mdl-menu--bottom-right"
                  :for        "hdrbtn"}
             [:li {:class-name "mdl-menu__item"} "About"]
             [:li {:class-name "mdl-menu__item"} "Contact"]]]]

          [:div {:class-name "demo-drawer mdl-layout__drawer mdl-color--blue-grey-900 mdl-color-text--blue-grey-50"}
           [:header {:class-name "demo-drawer-header"}
            [:div {:class-name "demo-avatar-dropdown"}
             [:span {} "Plongeur"]]]
           [:nav {:class-name "demo-navigation mdl-navigation mdl-color--blue-grey-800"}
            [:a {:class-name "mdl-navigation__link"
                 :href ""}
             [:i {:class-name "mdl-color-text--blue-grey-400 material-icons"
                  :role "presentation"} "home"] "home"]
            [:a {:class-name "mdl-navigation__link"
                 :href ""}
             [:i {:class-name "mdl-color-text--blue-grey-400 material-icons"
                  :role "presentation"} "settings"] "settings"]]]

          [:main {:class-name "mdl-layout__content mdl-color--grey-100"}
           [:button {:on-click   #(go (>! debug :click))
                     :class-name "mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent"} "print state"]
           (for [graph-state (m/graphs state)]
             (Sigma graph-state intent-chans))
           [:button {:on-click   #(go (>! add-graph :click))
                     :hidden     (>= (-> state :graphs count) 4)
                     :class-name "mdl-button mdl-js-button mdl-button--fab mdl-button--colored"}
            [:i {:class-name "material-icons"} "add"]]]]])

              )

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan intent-chans]
  (->> (fn [state] (Root state intent-chans)) ; fn
       (map)                                  ; xf
       (chan 10)                              ; ch
       (pipe states-chan)))