(ns htfu.cljs.home
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [cljs.pprint :refer [pprint]]
            [htfu.cljs.common :refer [debug]]
            [re-frame.core :as re-frame]
            [htfu.cljc.cljc-util :as cljc-util]
            cljsjs.material-ui
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.reagent :as rui]
            [cljs-react-material-ui.icons :as ic]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [reg-event-db reg-event-fx path reg-sub dispatch dispatch-sync subscribe]]))


(defonce registered-comps (atom nil))

(defn register-comp [key comp]
  (swap! registered-comps assoc key comp))


(reg-event-db
 :clear-message
 (fn [db [_]]
   (-> db
       (dissoc :message))))

(reg-event-db
 :show-comps
 (fn [db [_ comps]]
   (assoc db :show-comps (map identity comps))))

(reg-event-db
 :exercise-change
 (fn [db [_ item-id k v]]
   (-> db
       (assoc-in [:data :items item-id k] v))))

(reg-event-db
 :process-response
 (fn [db [_ kw response]]
   (-> db
       (assoc :loading? false)
       (assoc kw (js->clj response)))))

(reg-event-db
 :bad-response
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :data (js->clj response)))))

(reg-event-db
 :item-saved
 (fn [db [_ response]]
   (dispatch [:all-exercises])
   (-> db
       (assoc :loading? false)
       (assoc :message {:id :info :text "Item save OK"}))))

(reg-event-db
 :item-save-fail
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :message {:id :error :text "Item save FAIL"}))))

(reg-event-fx
 :item-save
 (fn
   [{db :db} [_ item-data]]
   {:http-xhrio {:method :post
                 :params item-data
                 :uri "/item-save"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:item-saved]
                 :on-failure [:item-save-fail]}
    :db  (assoc db :loading? true)}))

(reg-event-fx
 :all-exercises
 (fn
   [{db :db} _]
   {:http-xhrio {:method :get
                 :uri "/all-groups-with-exs"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:process-response :all-exercises]
                 :on-failure [:bad-response]}
    :db  (assoc db :loading? true)}))


(reg-event-fx
 :save-plan-day
 (fn
   [{db :db} [_ type-keyw day-num value]]
   {:http-xhrio {:method :post
                 :params {:type-keyw type-keyw :day-num day-num :value value}
                 :uri "/save-plan-day"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:item-saved]
                 :on-failure [:item-save-fail]}
    :db  (assoc db :loading? true)}))

;; SUBS

(reg-sub
 :all-exercises
 (fn [db [_]]
   (or (:all-exercises db)
       (do
         (dispatch [:all-exercises])
         (:all-exercises db)))))

(reg-sub
 :message
 (fn [db [_]]
   (get-in db [:message])))

(reg-sub
 :get-item
 (fn [db [_ item-id]]
   (get-in db [:data :items item-id])))


(defn comp-box []
  (fn [comp-key]
    [:div
     [(get @registered-comps comp-key)]]))

(defn item-form [item-id]
  (let [item-id (or item-id -1)
        item-data (re-frame/subscribe [:get-item item-id])]
    (fn []
      [rui/paper {:zDepth 2 :style {:margin "10px"
                                    :padding "5px 10px 10px 10px"}}
       [:h4 "New Item"]
       [rui/text-field {:floatingLabelText "Title"
                        :underlineShow false
                        :on-change #(dispatch [:exercise-change item-id :title (-> % .-target .-value)])}]
       [rui/divider]
       [rui/text-field {:floatingLabelText "Description"
                        :underlineShow false
                        :multiLine true
                        :fullWidth true
                        :on-change #(dispatch [:exercise-change item-id :desc (-> % .-target .-value)])}]
       [rui/divider]
       [rui/raised-button {:label "Save item"
                           :icon (ic/content-save)
                           :on-touch-tap #(dispatch [:item-save @item-data])
                           :style {:margin "10px 0 0 0"}}]
       [rui/raised-button {:label "Cancel"
                           :icon (ic/content-save)
                           :on-touch-tap #(dispatch [:item-save @item-data])
                           :style {:margin "10px 0 0 50px"}}]])))

(defn all-exercises []
  (let [all-exercises (re-frame/subscribe [:all-exercises])]
    (fn []

      [rui/paper
       [rui/subheader "All exercises"]
       (doall
        (map
         (fn [{:keys [id title desc] :as item}]
           ^{:key id}
           [rui/paper
            [rui/subheader title]
            [rui/paper
             (doall
              (map
               (fn [{:keys [id title desc] :as ex}]
                 ^{:key id}
                 [:div
                  [rui/subheader title]
                  [rui/card-text desc]
                  [rui/subheader "Training goals"]

                  [rui/table
                   [rui/table-body {:display-row-checkbox false}
                    [rui/table-row
                     [rui/table-row-column "Beginner standard "]
                     [rui/table-row-column (:beginner-standard ex)]]
                    [rui/table-row
                     [rui/table-row-column "Intermediate standard "]
                     [rui/table-row-column (:intermediate-standard ex)]]
                    [rui/table-row
                     [rui/table-row-column "Progression standard "]
                     [rui/table-row-column (:progression-standard ex)]]]]])
               (:exs item)))]])
         @all-exercises))])))

(defn dashboard []
  (fn []
    [rui/subheader "Dashboard"]))

(defn plan-table-select [title data selected type-keyw day-num]
  (fn [title data selected type-keyw day-num]
    [rui/select-field {:hint-text title
                       :on-change (fn [e idx value]
                                    (prn value day-num)
                                    (dispatch [:save-plan-day type-keyw day-num value])
                                    (swap! selected assoc type-keyw value))
                       :value (get @selected type-keyw)}

     (doall
      (map
       (fn [{:keys [id title desc] :as ex}]
         ^{:key id}
         [rui/menu-item {:value id :primary-text title}])
       data))]))

(defn plan-table-row [idx day all-exercises]
  (let [selected (reagent/atom {:group nil :ex nil})]
    (fn []
      (let [groups (-> (filter #(= (:group @selected) (:id %)) @all-exercises) first :exs)]
        [rui/table-row {:selectable false}
         [rui/table-row-column {:style {:padding "10px"
                                        :width "30px"
                                        :background "#EAEAEA"}} day]
         [rui/table-row-column
          [plan-table-select "Group" @all-exercises selected :group idx]]

         [rui/table-row-column
          (when (:group @selected)
            [plan-table-select "Exercise" groups selected :ex idx])]

         [rui/table-row-column
          (when (:ex @selected)
            (let [ex (first
                      (filter
                       #(= (:ex @selected) (:id %))
                       groups))]
              [rui/radio-button-group {:name "goals" :label-position "left"}
               [rui/radio-button {:value :beginner-standard :label (:beginner-standard ex)}]
               [rui/radio-button {:value :intermediate-standard :label (:intermediate-standard ex)}]
               [rui/radio-button {:value :progression-standard :label (:progression-standard ex)}]]))]]))))

(defn plan []
  (let [all-exercises (re-frame/subscribe [:all-exercises])]
    (fn []
      [rui/paper
       [rui/subheader "Plan"]

       [rui/table

        [rui/table-header {:adjust-for-checkbox false
                           :display-select-all false}
         [rui/table-row
          [rui/table-header-column {:style {:padding "10px"
                                            :width "30px"
                                            :background "#EAEAEA"}} "Day"]
          [rui/table-header-column "Group"]
          [rui/table-header-column "Exercise"]
          [rui/table-header-column "Goals"]]]

        [rui/table-body {:display-row-checkbox false}
         (doall
          (map-indexed
           (fn [idx day]
             ^{:key idx}
             [plan-table-row idx day all-exercises])
           ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]))]]])))

(defn home []
  (let [drawer-open? (reagent/atom false)
        all-data (re-frame/subscribe [:all-data])
        message (re-frame/subscribe [:message])]
    (fn []
      [rui/mui-theme-provider
       [rui/paper
        [rui/app-bar {:title "HTFU!"
                      :onLeftIconButtonTouchTap #(reset! drawer-open? (not @drawer-open?))}]

        [rui/drawer {:open @drawer-open?
                     :docked false
                     :label "label"
                     :on-request-change #(reset! drawer-open? (not @drawer-open?))}
         [rui/subheader "HTFU!"]
         [rui/menu-item {:on-touch-tap #(do
                                          (dispatch [:show-comps [:dashboard]])
                                          (reset! drawer-open? (not @drawer-open?)))} "Dashboard"]
         [rui/menu-item {:on-touch-tap #(do
                                          (dispatch [:show-comps [:plan]])
                                          (reset! drawer-open? (not @drawer-open?)))} "My plan"]
         [rui/menu-item {:on-touch-tap #(do
                                          (dispatch [:show-comps [:all-exercises]])
                                          (reset! drawer-open? (not @drawer-open?)))} "All exercises"]
         [rui/menu-item {:on-touch-tap #(do
                                          (dispatch [:show-comps [:item-form]])
                                          (reset! drawer-open? (not @drawer-open?)))} "New exercise"]]

        (map
         (fn [comp-key]
           ^{:key comp-key}
           [comp-box comp-key])
         (:show-comps @all-data))

        [rui/snackbar {:message (str (:text @message))
                       :open (boolean @message)
                       :autoHideDuration 3000
                       :bodyStyle {:background (condp = (:id @message)
                                                 :info "green"
                                                 :error "red"
                                                 "black")}
                       :onRequestClose #(dispatch [:clear-message])}]]])))

(register-comp :dashboard dashboard)
(register-comp :plan plan)
(register-comp :item-form item-form)
(register-comp :all-exercises all-exercises)


