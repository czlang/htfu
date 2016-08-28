(ns htfu.cljs.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [htfu.cljs.pages :as pages]
            [clj-brnolib.cljs.sente :refer [server-call start-router!]]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [htfu.cljs.common :as common]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-com.core :as re-com]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [secretary.core :as secretary]
            [taoensso.timbre :as timbre]
            htfu.cljs.home)
  (:import goog.History))

(enable-console-print!)

(re-frame/register-sub
 :current-page
 (fn [db _]
   (ratom/reaction (:current-page @db))))

(re-frame/register-handler
 :set-current-page
 common/debug
 (fn [db [_ current-page]]
   (assoc db :current-page current-page)))

(re-frame/register-handler
 :init-app
 common/debug
 (fn [db [_]]
   (merge db {})))

;; ---- Routes ---------------------------------------------------------------
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (re-frame/dispatch [:set-current-page :main]))

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
    [pages/page]))

(defn main []
  (start-router!)
  (hook-browser-navigation!)
  (if-let [node (.getElementById js/document "app")]
    (reagent/render [main-app-area] node)))

(pages/add-page :main #'page-main)

(main)
