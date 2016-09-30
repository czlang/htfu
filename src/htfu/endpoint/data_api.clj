(ns htfu.endpoint.data-api
  (:require [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [hiccup.page :as hiccup]
            [ring.util.anti-forgery :as anti-forgery]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.coercions :refer [as-int]]
            [crypto.password.scrypt :as scrypt]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre]
            [clojure.data.json :as json]
            [htfu.db.service :as service]
            [datomic.api :as d]))

(def system-title "HTFU")

(defn hiccup-frame [title body]
  (list
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, user-scalable=0, maximum-scale=1, minimum-scale=1"}]
    [:title title]
    [:link {:rel "stylesheet" :href "/css/site.css"}]]
   [:body body]))

(defn hiccup-response
  [body]
  (-> (hiccup/html5 {:lang "cs"} body)
      response/response
      (response/content-type "text/html")
      (response/charset "utf-8")))

(defn cljs-landing-page
  ([]
   (cljs-landing-page ))
  ([title]
   (hiccup-response
    (hiccup-frame title
                  [:div
                   [:div#app "Loading " title "..."]
                   (anti-forgery/anti-forgery-field)
                   [:script {:src "/js/main.js"}]]))))


(defn data-api-endpoint [{{conn :spec} :db}]
  (context "" {{user :user} :session}
    (GET "/" []
      (cljs-landing-page system-title))

    (GET "/login" []
      (-> (response/redirect "/" :see-other)
          (assoc-in [:session :user] {:username "Langi"})))

    (GET "/all-groups-with-exs" [message user :as req]
      (response/content-type
       (response/response
        (json/write-str
         (service/find-all-groups-with-exs (d/db conn))))
       "application/json"))

    (POST "/item-save" {:keys [params]}
      (service/save-item conn params)
      (response/content-type
       (response/response
        (json/write-str {}))
       "application/json"))

    (POST "/save-plan-day" {:keys [params]}
      (service/save-plan-day conn params)
      (response/content-type
       (response/response
        (json/write-str {}))
       "application/json"))))
