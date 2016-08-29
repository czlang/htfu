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


(reg-event-db
 :clear-message
 (fn [db [_]]
   (-> db
       (dissoc :message))))

(reg-event-db
 :item-new
 (fn [db [_ _]]
   (-> db
       (assoc :data {:items {-1 {:id -1 :title "" :desc ""}}}))))

(reg-event-db
 :item-change
 (fn [db [_ item-id k v]]
   (-> db
       (assoc-in [:data :items item-id k] v))))

(reg-event-db
 :process-response
 (fn [db [_ response]]
   (prn "process-response")
   (pprint response)
   (-> db
       (assoc :loading? false)
       (assoc :data (js->clj response)))))

(reg-event-db
 :bad-response
 (fn [db [_ response]]
   (prn "bad-response")
   (pprint response)
   (-> db
       (assoc :loading? false)
       (assoc :data (js->clj response)))))


#_(reg-event-fx
 :item-save
 (fn
   [{db :db} _]
   {:http-xhrio {:method :get
                 :params {:message "Zdarec"
                          :user "Voe"}
                 :uri "/disp-test"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:process-response]
                 :on-failure [:bad-response]}
    :db  (assoc db :loading? true)}))


(reg-event-db
 :item-saved
 (fn [db [_ response]]
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


;; SUBS

(reg-sub
 :all-data
 (fn [db [_]]
   db))

(reg-sub
 :message
 (fn [db [_]]
   (get-in db [:message])))

(reg-sub
 :get-item-new
 (fn [db [_ _]]
   (get-in db [:data :items -1])))

(reg-sub
 :get-item
 (fn [db [_ item-id]]
   (get-in db [:data :items item-id])))




(defn item-form [item-id]
  (let [item-id (or item-id -1)
        item-data (re-frame/subscribe [:get-item item-id])]
    (fn []
      [rui/paper {:zDepth 2 :style {:margin "10px"
                                    :padding "5px 10px 10px 10px"}}
       [:h4 "New Item"]
       [rui/text-field {:floatingLabelText "Title"
                        :underlineShow false
                        :on-change #(dispatch [:item-change item-id :title (-> % .-target .-value)])}]
       [rui/divider]
       [rui/text-field {:floatingLabelText "Description"
                        :underlineShow false
                        :multiLine true
                        :fullWidth true
                        :on-change #(dispatch [:item-change item-id :desc (-> % .-target .-value)])}]
       [rui/divider]
       [rui/raised-button {:label        "Save item"
                           :icon         (ic/content-save)
                           :on-touch-tap #(dispatch [:item-save @item-data])
                           :style {:margin "10px 0 0 0"}}]])))

(defn set-form []
  (fn[]
    [rui/paper {:zDepth 2 :style {:margin "10px"
                                  :padding "5px 10px 10px 10px"}}
     [:h4 "New Set"]
     [rui/text-field {:floatingLabelText "Title"
                      :underlineShow false}]
     [rui/divider]
     [rui/text-field {:floatingLabelText "Description"
                      :underlineShow false
                      :multiLine true
                      :fullWidth true}]
     [rui/divider]
     [rui/raised-button {:label        "Save set"
                         :icon         (ic/content-save)
                         :on-touch-tap #(prn "SAVE SET")
                         :style {:margin "10px 0 0 0"}}]]))

(defn home []
  (let [testing (reagent/atom 0)
        show-new-item? (reagent/atom false)
        show-new-set? (reagent/atom false)
        all-data (re-frame/subscribe [:all-data])
        message (re-frame/subscribe [:message])]
    (fn []
      [rui/mui-theme-provider
       [rui/paper
        [rui/app-bar {:title "HTFU!"
                      :onLeftIconButtonTouchTap #(reset! testing (inc @testing))}]
        [:pre (with-out-str (pprint @all-data))]
        [rui/raised-button {:label        "New item"
                            :icon         (ic/content-add)
                            :on-touch-tap #(do
                                             (reset! show-new-item? (not @show-new-item?))
                                             (dispatch [:item-new]))}]
        [rui/raised-button {:label        "New set"
                            :icon         (ic/content-add-circle)
                            :on-touch-tap #(reset! show-new-set? (not @show-new-set?))}]


        (when @show-new-item?
          [item-form])
        (when @show-new-set?
          [set-form])

        [rui/snackbar {:message (str (:text @message))
                       :open (boolean @message)
                       :autoHideDuration 3000
                       :bodyStyle {:background (condp = (:id @message)
                                                 :info "green"
                                                 :error "red"
                                                 "black")}
                       :onRequestClose #(dispatch [:clear-message])}]]])))





