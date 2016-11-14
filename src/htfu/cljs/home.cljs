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
 :load-plan
 (fn
   [{db :db} _]
   {:http-xhrio {:method :get
                 :uri "/plan"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:process-response :plan]
                 :on-failure [:bad-response]}
    :db  (assoc db :loading? true)}))

(reg-event-fx
 :save-plan-day
 (fn
   [{db :db} [_ data]]
   {:http-xhrio {:method :post
                 :params {:data data}
                 :uri "/save-plan-day"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:load-plan]
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
 :plan
 (fn [db [_]]
   (or (:plan db)
       (do
         (dispatch [:load-plan])
         (:plan db)))))

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

(defn plan-table-select [title data selected type-keyw day-num on-change-fn]
  (fn [title data selected type-keyw day-num]
    [rui/select-field {:hint-text title
                       :on-change (fn [e idx value]
                                    (on-change-fn value))
                       :value selected}
     (doall
      (map
       (fn [{:keys [id title desc] :as ex}]
         ^{:key id}
         [rui/menu-item {:value id :primary-text title}])
       data))]))

(defn group-param [data id param]
  (-> (filter #(= id (:id %)) data)
      first
      param))

(defn ex-param [data id param]
  (-> (mapcat #(filter (fn [ex] (= id (:id ex))) (:exs %)) data)
      first
      param))

(defn add-ex-modal [open? days all-exercises]
  (let [empty-data {:day nil :group nil :ex nil :standard nil}
        data-to-save (reagent/atom empty-data)]
    (fn []
      (let [exs (sort-by :id (-> (filter #(= (:group @data-to-save) (:id %)) @all-exercises)
                                 first
                                 :exs))]
        [:div
         [:div
          [rui/select-field {:hint-text "Day"
                             :on-change (fn [e idx value]
                                          (swap! data-to-save assoc :day idx))
                             :value (:day @data-to-save)}

           (doall
            (map-indexed
             (fn [idx day]
               ^{:key idx}
               [rui/menu-item {:value idx :primary-text day}])
             days))]]

         (when (:day @data-to-save)
           [:div
            [plan-table-select
             "Add group"
             @all-exercises
             (:group @data-to-save)
             :group
             (:day @data-to-save)
             (fn [group-id] (swap! data-to-save assoc :group group-id))]])

         (when (:group @data-to-save)
           [:div
            [plan-table-select
             "Exercise"
             exs
             (:ex @data-to-save)
             :ex
             (:day @data-to-save)
             (fn [ex-id] (swap! data-to-save assoc :ex ex-id))]])

         (when (:ex @data-to-save)
           (let [ex (first
                     (filter
                      #(= (:ex @data-to-save) (:id %))
                      exs))]
             [rui/radio-button-group {:name "goals"
                                      :label-position "left"
                                      :on-change (fn [e v]
                                                   (swap! data-to-save assoc :standard v))}
              [rui/radio-button {:value :beginner-standard :label (:beginner-standard ex)}]
              [rui/radio-button {:value :intermediate-standard :label (:intermediate-standard ex)}]
              [rui/radio-button {:value :progression-standard :label (:progression-standard ex)}]]))

         [rui/flat-button {:label "Cancel"
                           :on-touch-tap #(do
                                            (reset! data-to-save empty-data)
                                            (reset! open? false))
                           :style {:margin "10px 0 0 0"}}]
         [rui/flat-button {:label "Submit"
                           :keyboard-focused true
                           :on-touch-tap #(do
                                            (dispatch [:save-plan-day @data-to-save])
                                            (reset! data-to-save empty-data)
                                            (reset! open? false))
                           :style {:margin "10px 0 0 0"}}]]))))

(defn plan-table-row [idx day all-exercises plan]
  (fn []
    (let [day-plan (filter #(= idx (:day-num %)) @plan)]
      [rui/table-row {:selectable false}
       [rui/table-row-column {:style {:padding "10px" :width "30px" :background "#EAEAEA"}} day]
       [rui/table-row-column
        (doall
         (map
          (fn [v]
            ^{:key (:id v)}
            [:div {:style {:display "flex" :flexWrap "wrap"}}
             #_[rui/chip
                {:on-request-delete #(prn "DELETE " (get-in v [:group :id]))
                 :style {:margin "4px"}}
                (group-param @all-exercises (get-in v [:group :id]) :title)]
             [rui/chip
              {:on-touch-tap #(prn "TODO EDIT " (:id v))
               :on-request-delete #(prn "TODO DELETE " (:id v))
               :style {:margin "4px"}}
              (str
               (ex-param @all-exercises (get-in v [:ex :id]) :title)
               ", "
               ((keyword (:standard v))
                (first (filter #(= (get-in v [:ex :id]) (:id %)) (mapcat :exs @all-exercises)))))]
             #_[rui/chip
                {:on-request-delete #(prn "DELETE " (get-in v [:ex :id]))
                 :style {:margin "4px"}}
                ((keyword (:standard v))
                 (first (filter #(= (get-in v [:ex :id]) (:id %)) (mapcat :exs @all-exercises))))]])
          day-plan))]])))

(defn plan []
  (dispatch [:load-plan])
  (let [all-exercises (re-frame/subscribe [:all-exercises])
        plan (re-frame/subscribe [:plan])
        open? (reagent/atom false)
        days ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]]
    (fn []
      [rui/paper
       [rui/subheader "Plan"]

       [:div {:style {:position "fixed" :bottom "10px" :right "10px"} }
        [rui/raised-button {:label ""
                            :icon (ic/content-add)
                            :secondary true
                            :on-touch-tap #(reset! open? (not @open?))
                            :style {:margin "10px 0 0 0"}}]

        [rui/dialog
         {:title "Add exercise"
          :modal false
          :open @open?
          :on-request-close #(reset! open? false)}
         [add-ex-modal open? days all-exercises]]]

       [rui/table
        [rui/table-header {:adjust-for-checkbox false :display-select-all false}
         [rui/table-row
          [rui/table-header-column {:style {:padding "10px"
                                            :width "30px"
                                            :background "#EAEAEA"}} "Day"]
          [rui/table-header-column ""]]]
        [rui/table-body {:display-row-checkbox false}
         (doall
          (map-indexed
           (fn [idx day]
             ^{:key idx}
             [plan-table-row idx day all-exercises plan])
           days))]]])))

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
                                          (reset! drawer-open? (not @drawer-open?)))} "All exercises"]]
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
(register-comp :all-exercises all-exercises)


