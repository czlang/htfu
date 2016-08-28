(ns htfu.endpoint.data-api
  (:require [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [clj-brnolib.hiccup :as brn-hiccup]
            [hiccup.page :as hiccup]
            [ring.util.anti-forgery :as anti-forgery]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.coercions :refer [as-int]]
            [crypto.password.scrypt :as scrypt]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre]))

(def system-title "Htfu")

(defn hiccup-frame [title body]
  (list
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title title]
    [:link {:rel "stylesheet" :href "/css/site.css"}]]
   [:body
    body
    ]))

(defn hiccup-response
  [body]
  (-> (hiccup/html5 {:lang "cs"}
                    body)
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


(defn data-api-endpoint [{{conn :spec} :db
                          {ring-ajax-post :ring-ajax-post
                           ring-ajax-get-or-ws-handshake :ring-ajax-get-or-ws-handshake} :sente}]
  (context "" {{user :user} :session}
    (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
    (POST "/chsk" req (ring-ajax-post                req))

    (GET "/" []
      (cljs-landing-page system-title))))
