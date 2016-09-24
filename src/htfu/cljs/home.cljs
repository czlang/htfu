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
                 :uri "/all-exercises"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:process-response :all-exercises]
                 :on-failure [:bad-response]}
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
      [rui/list
       [rui/subheader "All exercises"]
       (doall
        (map
         (fn [{:keys [id title desc] :as item}]
           ^{:key id}
           [rui/list-item {:primary-text title
                           :secondary-text desc
                           :on-touch-tap #(do
                                            (prn "BOOM")
                                            )}])
         @all-exercises))])))

(defn dashboard []
  (fn []
    [rui/subheader "Dashboard"]))

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
(register-comp :item-form item-form)
(register-comp :all-exercises all-exercises)


