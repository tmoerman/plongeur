(ns plongeur.view
  "See https://github.com/CreativeIT/material-dashboard-lite"
  (:require [cljs.core.async :as a :refer [<! >! timeout tap chan pipe close!]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [foreign.sigma]
            [foreign.fullscreen]
            [foreign.select]
            [plongeur.model :as m]
            [plongeur.sigma :as s]
            [kierros.async :refer [debounce]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ascii [c] (-> c {\f 70
                       \l 76
                       \s 83} str))

;; MDL machinery

(defn upgrade-mdl-components
  "Material Design Lite component upgrade handler."
  []
  (js/setInterval #(.upgradeDom js/componentHandler) 100))

;; React components

(defn graph-id [id] (str "graph-" id))

(let [sigma-state (atom {})]
  (defcomponent Sigma
    "Component on which the Sigma canvas is mounted."
    :on-mount (fn [node [plot-state id] {:keys [set-force toggle-force toggle-lasso]}]
                (let [sigma-instance   (-> (s/make-sigma-instance (graph-id id)
                                                                  (m/sigma-settings plot-state))
                                           (s/read (m/sigma-data plot-state))
                                           (s/toggle-force-atlas-2 (m/force-layout-active? plot-state))
                                           (s/refresh))

                      active-state     (s/active-state sigma-instance)

                      keyboard         (s/keyboard sigma-instance nil {(ascii \f) #(go (>! toggle-force id))
                                                                       (ascii \l) #(go (>! toggle-lasso id))})

                      lasso            (s/lasso sigma-instance)

                      _                (s/select sigma-instance active-state {:keyboard keyboard
                                                                              :lasso    lasso})

                      _                (s/drag-nodes sigma-instance active-state
                                                     {:startdrag #(go (>! set-force [id false]))})]

                  (swap! sigma-state assoc id
                         {:sigma        sigma-instance
                          :keyboard     keyboard
                          :lasso        lasso})))

    :on-unmount (fn [node [plot-state id] cmd-chans]
                  (swap! sigma-state
                         (fn [m]
                           (some->> id m :sigma s/kill)
                           (dissoc m id))))

    :on-update (fn [node [plot-state id] old cmd-chans]
                 (let [{{:keys [sigma keyboard lasso]} id} @sigma-state]
                   (some-> sigma (s/toggle-force-atlas-2 (m/force-layout-active? plot-state)))
                   (some-> lasso (s/toggle-lasso         (m/lasso-tool-active? plot-state)))))

    [[plot-state id] cmd-chans]
    (html [:div {:id         (graph-id id)
                 :class-name "sigma-dark"}])))

(defcomponent
  Card
  "Component surrounding the visualization containers."
  :keyfn (fn [[_ id _]] id)

  [[plot-state id idx] {:keys [drop-plot toggle-force toggle-lasso] :as cmd-chans}]

  (let [force? (m/force-layout-active? plot-state)
        lasso? (m/lasso-tool-active?   plot-state)]

    (html [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
           [:div {:id         (str "card-" id)
                  :class-name "mdl-card mdl-shadow--2dp"}

            (Sigma [plot-state id] cmd-chans)

            [:div {:class-name "mdl-card__actions"}

             [:button {:on-click   #(go (>! toggle-force id))
                       :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"
                       :title      (if force? "Pause force layout" "Resume force layout")}
              [:i {:class-name "material-icons mdl-badge"} (if force? "pause" "play_arrow")]]

             [:button {:on-click   #(go (>! toggle-lasso id))
                       :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"
                       :title      (if lasso? "Pan tool" "Lasso tool")}
              [:i {:class-name "material-icons mdl-badge"} (if lasso? "pan_tool" "gesture" )]]

             [:div {:on-click   #(go (>! drop-plot id))
                    :id         (str "drop-plot-" id)
                    :title      (str "Remove plot " id)
                    :class-name "mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab"}
              [:i {:class-name "material-icons"} "delete"]]]]])))


(defcomponent Grid
  [state cmd-chans]
  (html [:main {:class-name "mdl-layout__content"}
         [:div {:class-name "mdl-grid mdl-grid--no-spacing"}
          [:div {:class-name "mdl-grid mdl-cell mdl-cell--12-col-desktop mdl-cell--12-col-tablet mdl-cell--4-col-phone mdl-cell--top"}
           (for [[idx [id plot-state]] (->> (m/plots state)
                                            (map-indexed vector))]
             (Card [plot-state id idx] cmd-chans))]]]))

(defcomponent Menu
              [state {:keys [debug]}]
              (html [:ul {:class-name "mdl-menu mdl-list mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect mdl-shadow--2dp account-dropdown"
                          :for "more-vert-btn"}

                     [:li {:class-name "mdl-menu__item mdl-list__item"}
                      [:span {:class-name "mdl-list__item-primary-content"}
                       [:i {:class-name "material-icons mdl-list__item-icon"} "settings"] "Configuration"]]

                     [:li {:class-name "list__item--border-top"}]

                     [:li {:class-name "mdl-menu__item mdl-list__item"}
                      [:span {:on-click #(go (>! debug :click))
                              :class-name "mdl-list__item-primary-content"}
                       [:i {:class-name "material-icons mdl-list__item-icon"} "get_app"] "Write app state to console"]]

                     [:li {:class-name "list__item--border-top"}]

                     [:li {:class-name "mdl-menu__item mdl-list__item"}
                      [:span {:class-name "mdl-list__item-primary-content"}
                       [:i {:class-name "material-icons mdl-list__item-icon"} "attach_file"] "Publication"]]

                     [:li {:class-name "mdl-menu__item mdl-list__item"}
                      [:span {:class-name "mdl-list__item-primary-content"}
                       [:i {:class-name "material-icons mdl-list__item-icon"} "link"] "Github"]]]))

(defcomponent Header
              [state {:keys [add-plot fill-plots] :as cmd-chans}]
              (html [:header {:class-name "mdl-layout__header"}
                     [:div {:class-name "mdl-layout__header-row"}

                      [:div {:class-name "mdl-layout-spacer"}]

                      [:div {:on-click   #(go (>! add-plot :sigma))
                             :title      "Add plot"
                             :hidden     (>= (m/plot-count state) 4)
                             :class-name "material-icons mdl-badge mdl-button--icon"} "add"]

                      [:div {:on-click #(go (>! fill-plots :click))
                             :title      "Generate plots"
                             :class-name "material-icons mdl-badge mdl-button--icon"} "new_releases"]

                      [:button {:id "more-vert-btn"
                                :title      "More options"
                                :class-name "mdl-button mdl-js-button mdl-button--icon"}
                       [:i {:class-name "material-icons"} "more_vert"]]

                      (Menu state cmd-chans)]]))

(defcomponent Root
  [state cmd-chans]
  (html [:div {:id "plongeur-main"}
         [:div {:id         "layout"
                :class-name "mdl-layout mdl-js-layout mdl-layout--fixed-header"}
          (Header state cmd-chans)
          #_(Drawer state cmd-chans)
          (Grid   state cmd-chans)]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan cmd-chans]
  (->> (fn [state] (Root state cmd-chans)) ; fn
       (map)                               ; xf
       (chan 10)                           ; ch
       (pipe states-chan)))
