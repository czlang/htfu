(ns htfu.cljs.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [htfu.cljs.pages :as pages]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [htfu.cljs.common :as common]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub dispatch]]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]
            htfu.cljs.home)
  (:import goog.History))

(enable-console-print!)

(reg-event-db
 :init-app
 (fn [db [_]]
   (merge db {})))

(reg-sub
 :current-page
 (fn [db _]
   (reaction (:current-page @db))))

(reg-event-db
 :set-current-page
 (fn [db [_ current-page]]
   (assoc db :current-page current-page)))

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

(defn page-main []
  [htfu.cljs.home/home])

(defn main-app-area []
  (fn []
    #_[pages/page] ;; FIXME 
    [htfu.cljs.home/home]))

(defn main []
  ;;(start-router!)
  (hook-browser-navigation!)
  (if-let [node (.getElementById js/document "app")]
    (reagent/render [main-app-area] node)))

(pages/add-page :main #'page-main)

(main)
