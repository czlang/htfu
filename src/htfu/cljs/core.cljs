(ns htfu.cljs.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [htfu.cljs.pages :as pages]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [htfu.cljs.common :as common]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :refer [reg-event-db reg-event-fx path reg-sub dispatch dispatch-sync subscribe]]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]
            htfu.cljs.home
            [cljs-react-material-ui.reagent :as rui]
            [cljs-react-material-ui.icons :as ic]
            [ajax.core :as ajax])
  (:import goog.History))

(enable-console-print!)

(reg-event-db
 :init-app
 (fn [db [_]]
   (merge db {:show-comps [:dashboard]})))

(reg-sub
 :all-data
 (fn [db [_]]
   db))

(reg-sub
 :all-exercises
 (fn [db [_]]
   (or (:all-exercises db)
       (do
         (dispatch [:all-exercises])
         (:all-exercises db)))))

(reg-sub
 :current-page
 (fn [db _]
   (reaction (:current-page @db))))

(reg-event-db
 :set-current-page
 (fn [db [_ current-page]]
   (assoc db :current-page current-page)))

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



;; ---- Routes ---------------------------------------------------------------
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (dispatch [:set-current-page :main]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


(defn inspector []
  (let [all-data (subscribe [:all-data])
        show-data? (reagent/atom false)
        default-height (/ (.-innerHeight js/window) 2)
        default-width (/ (.-innerWidth js/window) 3)
        height (reagent/atom default-height)
        width (reagent/atom default-width)]
    (fn []
      [:div {:style {:position "fixed" :bottom "10px" :left "10px"} }
       [rui/raised-button {:label ""
                           :icon (ic/action-code)
                           :on-touch-tap #(swap! show-data? not)
                           :style {:margin "10px 0 0 0"}}]
       (when @show-data?
         [:pre {:style {:overflow "scroll"
                        :height (str @height"px")
                        :width (str @width"px")}} (with-out-str (pprint @all-data))])])))

(defn main-app-area []
  (dispatch [:init-app])
  (dispatch [:load-plan])
  (let [all-exercises (subscribe [:all-exercises])]
    (fn []
      [rui/mui-theme-provider
       (if-not @all-exercises
         [rui/linear-progress]
         [rui/paper
          [htfu.cljs.home/home]
          [inspector]])])))

(defn main []
  (hook-browser-navigation!)
  (if-let [node (.getElementById js/document "app")]
    (reagent/render [main-app-area] node)))


(main)
