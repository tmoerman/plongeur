(ns plongeur.view
  "Quiescent/React powered front end.

  Template: https://github.com/CreativeIT/material-dashboard-lite
  Icons:    https://design.google.com/icons/"
  (:require [cljs.core.async :as a :refer [<! >! timeout alts! tap chan pipe close!]]
            [quiescent.core :as q :include-macros true :refer-macros [defcomponent]]
            [sablono.core :refer-macros [html]]
            [foreign.sigma]
            [foreign.linkurious]
            [plongeur.route :as r]
            [plongeur.model :as m]
            [plongeur.sigma :as s]
            [plongeur.config :as c]
            [kierros.async :refer [debounce val-timeout]]
            [dommy.core :as d :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ascii [c] (-> c {\f 70
                       \l 76
                       \s 83} str))

;; Set MDL upgrade interval

(defn upgrade-mdl-components
  "Material Design Lite component upgrade handler."
  []
  (js/setInterval #(.upgradeDom js/componentHandler) 100))

;; React components

(defn plot-container-id [id] (str "plot-" id))

(let [sigma-state (atom {})]
  (defcomponent Sigma
    "Component on which the Sigma canvas is mounted."

    :keyfn (fn [[plot-state id idx]] id)

    :on-mount (fn [node [plot-state id idx] {:keys [set-force
                                                    toggle-force
                                                    toggle-lasso
                                                    merge-plot-data]}]
                (prn "on-mount")

                (let [sigma-instance   (-> (s/make-sigma-instance (plot-container-id id)
                                                                  (-> c/default-config :sigma :settings)
                                                                  ;(m/plot-settings plot-state)
                                                                  )
                                           (s/read (m/plot-data plot-state))
                                           (s/toggle-force-atlas-2 (m/force-layout-active? plot-state))
                                           (s/refresh))

                      active-state     (s/active-state sigma-instance)

                      keyboard         (s/keyboard sigma-instance nil
                                                   {(ascii \f) #(go (>! toggle-force id))
                                                    (ascii \l) #(go (>! toggle-lasso id))})

                      lasso            (s/lasso sigma-instance
                                                {}
                                                {:selected-nodes (fn [lasso event]
                                                                   (->> event
                                                                        .-data
                                                                        (map #(.-id %))
                                                                        clj->js
                                                                        (.addNodes active-state))
                                                                   (s/refresh sigma-instance)
                                                                   (go (>! toggle-lasso id)))})

                      ; _                 (.halo (s/renderer sigma-instance) (clj->js {:nodes (-> sigma-instance .-graph .nodes)}))

                      _                (s/select sigma-instance active-state {
                                                                              :keyboard keyboard
                                                                              :lasso    lasso
                                                                              })

                      _                (s/drag-nodes sigma-instance active-state
                                                     {:startdrag #(go (>! set-force [id false]))})

                      ctrl-ch          (chan 10)]

                  (go-loop []
                           (let [interval-ms (-> plot-state m/sync-interval-ms (or 30000))
                                 timeout-ch  (timeout interval-ms)
                                 [v ch]      (alts! [timeout-ch ctrl-ch])]
                             (condp = ch
                               timeout-ch (do
                                            (prn (str "merging plot " id))
                                            (>! merge-plot-data [id (s/data sigma-instance)])
                                            (recur))
                               ctrl-ch    (when v
                                            (prn (str "merging plot " id))
                                            (>! merge-plot-data [id (s/data sigma-instance)])
                                            (recur)))))

                  (swap! sigma-state assoc id
                         {:sigma     sigma-instance
                          :keyboard  keyboard
                          :lasso     lasso
                          :ctrl      ctrl-ch})))

    :on-unmount (fn [node [plot-state id idx] cmd-chans]

                  (prn "on-unmount")

                  (swap! sigma-state
                         (fn [m]
                           (some-> id m :sigma s/kill)
                           (some-> id m :ctrl  close!)
                           (dissoc m id))))

    :on-update (fn [node [plot-state id idx] old cmd-chans]
                 (let [{{:keys [sigma lasso]} id} @sigma-state]
                   (some-> sigma
                           (s/toggle-force-atlas-2 (m/force-layout-active? plot-state)))
                   (some-> lasso
                           (s/toggle-lasso (m/lasso-tool-active? plot-state)))
                   ;; (some-> sigma s/refresh)
                   ))

    [[plot-state id idx] {:keys [drop-plot toggle-force toggle-lasso]}]

    (html
      [:div {:class-name "mdl-cell mdl-cell--6-col-desktop mdl-cell--6-col-tablet mdl-cell--6-col-phone"}
       [:div {:id         (str "card-" id)
              :class-name "mdl-card mdl-shadow--2dp"}

        [:div {:id         (plot-container-id id)
               :class-name "sigma-dark"}]

        [:div {:class-name "mdl-card__actions"}

         (let [force? (m/force-layout-active? plot-state)]
           [:button {:on-click   #(go (>! toggle-force id))
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"
                     :title      (if force? "Pause force layout" "Resume force layout")}
            [:i {:class-name "material-icons mdl-badge"} (if force? "pause" "play_arrow")]])

         (let [lasso? (m/lasso-tool-active? plot-state)]
           [:button {:on-click   #(go (>! toggle-lasso id))
                     :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"
                     :title      (if lasso? "Pan tool" "Lasso tool")}
            [:i {:class-name "material-icons mdl-badge"} (if lasso? "pan_tool" "gesture")]])

         [:button {:on-click   #(let [{{ctrl-ch :ctrl} id} @sigma-state]
                                  (go (>! ctrl-ch :!)))
                   :class-name "mdl-button mdl-js-button mdl-js-ripple-effect"
                   :title      "Persist"}
          [:i {:class-name "material-icons mdl-badge"} "save"]]

         [:div {:on-click   #(go (>! drop-plot id))
               :id         (str "drop-plot-" id)
               :title      (str "Remove plot " id)
               :class-name "mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab"}
         [:i {:class-name "material-icons"} "delete"]]]]])))

(defcomponent Scene-grid
  [state cmd-chans]
  (html [:main {:class-name "mdl-layout__content"}
         [:div {:class-name "mdl-grid mdl-grid--no-spacing"}
          [:div {:class-name "mdl-grid mdl-cell mdl-cell--12-col-desktop mdl-cell--12-col-tablet mdl-cell--4-col-phone mdl-cell--top"}
           (for [[idx [id plot-state]] (->> (m/plots state)
                                            (map-indexed vector))]
             (condp = (m/plot-type plot-state)
               :sigma (Sigma [plot-state id idx] cmd-chans)))]]]))

(defcomponent More-menu
  [state {:keys [debug post-request]}]
  (html [:ul {:class-name "mdl-menu mdl-list mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect mdl-shadow--2dp account-dropdown"
              :for "more-vert-btn"}
         [:div {}] ;; dummy element to remedy sizing glitch

         [:li {:class-name "mdl-menu__item mdl-list__item"}
          [:span {:on-click #(go (>! debug :click))
                  :class-name "mdl-list__item-primary-content"}
           [:i {:class-name "material-icons mdl-list__item-icon"} "bug_report"] "Write app state to console"]]
         [:li {:class-name "mdl-menu__item mdl-list__item"}
          [:span {:on-click #(go (>! post-request [:plongeur/ping {}]))
                  :class-name "mdl-list__item-primary-content"}
           [:i {:class-name "material-icons mdl-list__item-icon"} "priority_high"] "Ping server"]]

         [:li {:class-name "list__item--border-top"}]

         [:a {:class-name "mdl-menu__item mdl-list__item"}
          [:span {:class-name "mdl-list__item-primary-content"}
           [:i {:class-name "material-icons mdl-list__item-icon"} "attach_file"] "Publication"]]
         [:a {:class-name "mdl-menu__item mdl-list__item"
              :href       "https://github.com/tmoerman/plongeur"}
          [:span {:class-name "mdl-list__item-primary-content"}
           [:i {:class-name "material-icons mdl-list__item-icon"} "link"] "Github"]]]))

(defcomponent Nav-buttons
  [[current-view user-id] {:keys [navigate]}]
  (html [:span {}

         ;; TODO refactor with loop?

         #_(let [disabled (= :view/login-user current-view)]
           [:button {:title      "User login"
                     :class-name (if disabled
                                   "mdl-button mdl-js-button mdl-button--icon mdl-button--disabled"
                                   "mdl-button mdl-js-button mdl-button--icon")
                     :on-click   #(when-not disabled (go (>! navigate :view/login-user)))}
            [:i {:class-name "material-icons"} (if disabled "person" "person_outline")]])

         (let [disabled (or (= :view/browse-scenes current-view)
                            (nil? user-id))]
           [:button {:title      "Browse scenes"
                     :class-name (if disabled
                                   "mdl-button mdl-js-button mdl-button--icon mdl-button--disabled"
                                   "mdl-button mdl-js-button mdl-button--icon")
                     :on-click   #(when-not disabled (go (>! navigate :view/browse-scenes)))}
            [:i {:class-name "material-icons"} (if disabled "folder" "folder_open")]])

         (let [disabled (or (= :view/create-scene current-view)
                            (nil? user-id))]
           [:button {:title      "Create scene"
                     :class-name (if disabled
                                   "mdl-button mdl-js-button mdl-button--icon mdl-button--disabled"
                                   "mdl-button mdl-js-button mdl-button--icon")
                     :on-click   #(when-not disabled (go (>! navigate :view/create-scene)))}
            [:i {:class-name "material-icons"} (if disabled "insert_drive_file" "note_add")]])

         (let [disabled (or (= :view/edit-config current-view)
                            (nil? user-id))]
           [:button {:title      "Configuration"
                     :class-name (if disabled
                                   "mdl-button mdl-js-button mdl-button--icon mdl-button--disabled"
                                   "mdl-button mdl-js-button mdl-button--icon")
                     :on-click   #(when-not disabled (go (>! navigate :view/edit-config)))}
            [:i {:class-name "material-icons"} "settings"]])]))

(defcomponent User-menu
  [[user-id state] {:keys [logout-user navigate]}]
  (html [:span {}
         [:div {:class-name "avatar-dropdown"
                :id         "user-menu"}
          [:span {} user-id]]
         [:ul {:class-name "mdl-menu mdl-list mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect mdl-shadow--2dp account-dropdown"
               :for        "user-menu"
               :on-click   #(go
                             (prn "logout clicked")
                             (>! logout-user :-P)
                             ;(>! navigate    :view/login-user)
                             (>! navigate    :view/none)
                             )}
          [:li {}]
          [:li {:class-name "mdl-menu__item mdl-list__item"}
           [:span {:class-name "mdl-list__item-primary-content"}
            [:i {:class-name "material-icons mdl-list__item-icon text-color--secondary"} "exit_to_app"]
            "Log out"]]]]))

(defcomponent Header
  [state {:keys [add-plot] :as cmd-chans}]
  (html [:header {:class-name "mdl-layout__header"}
         [:div {:class-name "mdl-layout__header-row"}

          (Nav-buttons ((juxt m/current-view m/user-id) state) cmd-chans)

          [:div {:class-name "mdl-layout-spacer"}]

          (when-let [user-id (m/user-id state)]
            (User-menu [user-id state] cmd-chans))

          (when (= :view/edit-scene (m/current-view state))
            [:div {:on-click   #(go (>! add-plot :sigma))
                   :title      "Add plot"
                   :hidden     (>= (m/plot-count state) 4)
                   :class-name "material-icons mdl-badge mdl-button--icon"} "add"])

          [:button {:id         "more-vert-btn"
                    :title      "More options"
                    :class-name "mdl-button mdl-js-button mdl-button--icon"}
           [:i {:class-name "material-icons"} "more_vert"]]

          (More-menu state cmd-chans)]]))

(defcomponent Drawer
              [state cmd-chans]
              (html [:div {:class-name "mdl-layout__drawer"}

                     ;; TODO Perhaps display some connection status info

                     "hello"]))

(defn select-val  [el-id] (some-> js/document (sel1 [el-id]) .-value))
(defn select-vals [el-ids] (->> el-ids (map select-val) vec))

(defcomponent Login-box
  [[user-id state] {:keys [login-user navigate] :as cmd-chans}]
  (html [:main {class-name "mdl-layout__content mdl-color--grey-100"}
         [:div {:class-name "mdl-card mdl-shadow--2dp employer-form"}
          [:div {:class-name "mdl-card__title"}
           [:h2 {} "Sign in"]
           [:div {:class-name "mdl-card__subtitle"} "Please provide your user credentials"]]
          [:div {:class-name "mdl-card__supporting-text"}
           [:div {:class-name "form"}

            [:div {:class-name "form__article"}
             [:div {:class-name "mdl-grid"}

              [:div {:class-name "mdl-cell mdl-cell--6-col mdl-textfield mdl-js-textfield mdl-textfield--floating-label"}
               [:input {:class-name "mdl-textfield__input"
                        :type       "text"
                        :id         "user-id"
                        :default-value user-id}]
               [:label {:class-name "mdl-textfield__label" :for "user-id"} "User name"]]

              [:div {:class-name "mdl-cell mdl-cell--6-col mdl-textfield mdl-js-textfield mdl-textfield--floating-label"}
               [:input {:class-name "mdl-textfield__input"
                        :type       "password"
                        :id         "password"}]
               [:label {:class-name "mdl-textfield__label" :for "password"} "Password"]]]]

            [:div {:class-name "form__action"}
             [:div {:class-name "mdl-layout-spacer"}]
             [:button {:class-name "mdl-button mdl-js-button mdl-button--raised mdl-button--colored"
                       :on-click   #(go
                                     (>! login-user (select-vals [:#user-id :#password]))
                                     (>! navigate   :view/browse-scenes))} "Submit"]]]]]]))

(defcomponent Scene-browser
  [state cmd-chans]
  (html [:h1 {} "scene-browser"]))

(defcomponent
  Scene-form
  [state cmd-chans]
  (html [:h1 {} "create-scene"]))

(defcomponent
  Edit-config
  [state cmd-chans]
  (html [:h1 {} "edit-config"]))

(let [a (atom nil)]
  (defcomponent
    Splash-screen
    :on-mount (fn [_ _ {:keys [navigate]}]
                (let [t (val-timeout 3000)]
                  (swap! a (fn [c] (some-> c close!) t))
                  (go (when (<! t)
                        (do
                          (prn "splash timeout fired!")
                          (>! navigate :view/login-user))))))
    [state {:keys [navigate] :as cmd-chans}]
    (html [:main {:class-name "mdl-layout__content mdl-color--grey-100"}
           [:div {:class-name "mdl-card mdl-shadow--2dp employer-form"
                  :on-click   #(go
                                (swap! a (fn [c] (some-> c close!) nil))
                                (>! navigate :view/login-user))}
            #_[:h3 {} "Ceci n'est pas une soumarine"]
            [:img {:src "img/trieste.jpg"}]]])))

(defcomponent Root
  [state cmd-chans]
  (html [:div {:id "plongeur-main"}
         [:div {:id         "layout"
                :class-name "mdl-layout mdl-js-layout mdl-layout--fixed-header"}

          (Header state cmd-chans)

          #_(Drawer state cmd-chans)

          (case (m/current-view state)
            :view/login-user    (Login-box     [(m/user-id state) state] cmd-chans)
            :view/browse-scenes (Scene-browser state cmd-chans)
            :view/create-scene  (Scene-form    state cmd-chans)
            :view/edit-scene    (Scene-grid    state cmd-chans)
            :view/edit-config   (Edit-config   state cmd-chans)
                                (Splash-screen state cmd-chans))]]))

(defn view
  "Returns a stream of view trees, represented as a core.async channel."
  [states-chan cmd-chans]
  (->> (fn [state] (Root state cmd-chans)) ; fn
       (map)                               ; xf
       (chan 10)                           ; ch
       (pipe states-chan)))
