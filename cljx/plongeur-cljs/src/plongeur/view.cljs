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
    :on-mount (fn [node [state idx id props] {:keys [web-response-mult] :as cmd-chans}]
                (let [sigma-ctrl       (chan 10)

                      sigma-instance   (s/make-sigma-instance (graph-id id)
                                                              (m/sigma-settings state)
                                                              {;;:click        #(go (>! sigma-ctrl [:click        id]))
                                                               :double-click #(go (>! sigma-ctrl [:toggle-force id]))})

                      sigma-renderer   (s/renderer sigma-instance)

                      active-state     (s/active-state sigma-instance)

                      keyboard         (s/keyboard sigma-instance)

                      lasso            (s/lasso sigma-instance)

                      select-handle    (s/select sigma-instance active-state {:keyboard keyboard
                                                                              :lasso    lasso})

                      _                (s/drag-nodes sigma-instance
                                                     active-state
                                                     {:startdrag #(s/kill-force-atlas-2 sigma-instance)
                                                      :dragend   #(s/start-force-atlas-2 sigma-instance)})

                      web-response-tap (->> (chan 10)
                                            (tap web-response-mult))]

                  (go-loop []
                           (when-let [[k id] (<! sigma-ctrl)]

                             #_(condp = k
                               :click         (s/toggle-active lasso)
                               :toggle-force  (do
                                                (prn "double click captured")
                                                (s/toggle-force-atlas-2 sigma-instance)))

                             (recur)))

                  (go-loop []
                           (when-let [v (<! web-response-tap)]
                             (s/kill-force-atlas-2 sigma-instance)

                             (-> sigma-instance
                                 (s/clear)
                                 (s/read (s/make-shape))
                                 (s/refresh))

                             (s/start-force-atlas-2 sigma-instance)

                             (recur)))

                  (swap! sigma-state assoc id
                         {:sigma        sigma-instance
                          :ctrl-ch      sigma-ctrl
                          :tap-ch       web-response-tap
                          :keyboard     keyboard
                          :lasso        lasso})))

    :on-unmount (fn [node [state idx id props] {:keys [web-response-mult] :as cmd-chans}]
                  (swap! sigma-state
                         (fn [m]
                           (some->> id m :tap-ch         (a/untap web-response-mult))
                           (some->> id m :tap-ch         close!)
                           (some->> id m :ctrl-ch        close!)
                           (some->> id m :force-cmd-chan close!)
                           (some->> id m :sigma          s/kill)
                           (dissoc m id))))

    [[state idx id props] cmd-chans]
    (html [:div {:id         (graph-id id)
                 :class-name "sigma-dark"}])))

(defcomponent Card
  "Component surrounding the visualization containers."
  :keyfn (fn [[_ id _ _]] id)

  [[state id props idx] {:keys [drop-plot] :as cmd-chans}]
  (html [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
         [:div {:id         (str "card-" id)
                :class-name "mdl-card mdl-shadow--2dp"
                :tab-index  (inc idx)}

          (Sigma [state idx id props] cmd-chans)

          [:div {:class-name "mdl-card__actions"}

           [:button {:on-click   #(prn "bla")
                     :title      "Generate plots"
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"}
            [:i {:class-name "material-icons mdl-badge"} "play_arrow"]]

           [:button {:on-click   #(prn "bla")
                     :title      "Generate plots"
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"}
            [:i {:class-name "material-icons mdl-badge"} "pause"]]

           [:div {:on-click   #(go (>! drop-plot id))
                  :id         (str "drop-plot-" id)
                  :title      (str "Remove plot " id)
                  :class-name "mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab"}
            [:i {:class-name "material-icons"} "delete"]]]]]))

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
  [state {:keys [add-plot web-response-chan] :as cmd-chans}]
  (html [:header {:class-name "mdl-layout__header"}
         [:div {:class-name "mdl-layout__header-row"}

          [:div {:class-name "mdl-layout-spacer"}]

          [:div {:on-click   #(go (>! add-plot :tda))
                 :title      "Add plot"
                 :hidden     (>= (m/plot-count state) 4)
                 :class-name "material-icons mdl-badge mdl-button--icon"} "add"]

          [:div {:on-click #(go (>! web-response-chan :click))
                 :title      "Generate plots"
                 :class-name "material-icons mdl-badge mdl-button--icon"} "refresh"]

          [:button {:id "more-vert-btn"
                    :title      "More options"
                    :class-name "mdl-button mdl-js-button mdl-button--icon"}
           [:i {:class-name "material-icons"} "more_vert"]]

          (Menu state cmd-chans)]]))

(defcomponent Grid
  [state cmd-chans]
  (html [:main {:class-name "mdl-layout__content"}
         [:div {:class-name "mdl-grid mdl-grid--no-spacing"}
          [:div {:class-name "mdl-grid mdl-cell mdl-cell--12-col-desktop mdl-cell--12-col-tablet mdl-cell--4-col-phone mdl-cell--top"}
           (for [[idx [id props]] (->> (m/plots state)
                                       (map-indexed vector))]
             (Card [state id props idx] cmd-chans))]]]))

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
