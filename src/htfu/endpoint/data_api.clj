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
    (hiccup-frame
     title
     [:div
      [:div#app
       [:div {:class "loading-text"}
        "Insert coin..."
        [:svg {:viewBox "0 0 200 200"
               :x "0px"
               :y "0px"}
         [:g {:stroke "none"
              :stroke-width "1"
              :fill "none"
              :fill-rule "evenodd"}
          [:g {:transform "translate(-1152.000000, -100.000000)"}
           [:g {:transform "translate(1202.000000, 156.000000)" :fill "#000000"}
            [:g {:transform "translate(4.000000, 0.000000)"}
             [:path {:d "M18,3 L70,3 C78.5108832,3 85,9.48685873 85,18 L85,70 C85,78.5108832 78.5131413,85 70,85 L18,85 C9.48911679,85 3,78.5131413 3,70 L3,18 C3,9.48911679 9.48685873,3 18,3 Z M0,70 C0,80.1702293 7.83249623,88 18,88 L70,88 C80.1702293,88 88,80.1675038 88,70 L88,18 C88,7.82977071 80.1675038,0 70,0 L18,0 C7.82977071,0 0,7.83249623 0,18 L0,70 Z"}]]
            [:path {:d "M57.3820815,63.2695075 C55.0244443,65.4136379 51.6423126,66.6717418 47.9970804,66.6717418 C44.3518483,66.6717418 40.9697166,65.4136379 38.6120793,63.2695075 C37.7662245,62.5002529 36.5001374,63.5233819 37.0747114,64.5118594 C39.2688687,68.2866129 43.418211,70.6717418 47.9970804,70.6717418 C52.5759499,70.6717418 56.7252922,68.2866129 58.9194495,64.5118594 C59.4940235,63.5233819 58.2279364,62.5002529 57.3820815,63.2695075 Z"}]
            [:g {:transform "translate(0.000000, 30.000000)"}
             [:path {:d "M48.0000001,3.59349274 L43.0572205,3.59349274 C21.9661307,-3.57275708 7.02549765,2.19316986 7.02549765,2.19316986 C7.02549765,2.19316986 -2.67861385e-07,1.82598271 0,5.66314132 C4.62647642e-07,9.59805122 3.53362673,8.04985793 5.05685136,12.2283025 C6.58007599,16.406747 5.05685147,30.0895665 24.9357476,30.0895522 C42.4596698,30.0895669 44.4748889,10.6786392 46.5209166,10.6786396 L47.9999999,10.6786399 L49.4790834,10.6786396 C51.5251111,10.6786392 53.5403304,30.0895669 71.0642524,30.0895522 C90.9431485,30.0895665 89.4199241,16.406747 90.9431487,12.2283025 C92.4663733,8.04985793 95.9999996,9.59805122 96,5.66314132 C96.0000004,1.82598271 88.9745024,2.19316986 88.9745024,2.19316986 C88.9745024,2.19316986 74.0338693,-3.57275708 52.9427795,3.59349274 L48.0000001,3.59349274 L48.0000001,3.59349274 Z"}]]]]]]]]
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

    (GET "/plan" [message user :as req]
      (response/content-type
       (response/response
        (json/write-str
         (service/find-all-day-num-plans (d/db conn))))
       "application/json"))

    (POST "/save-plan-day" {:keys [params]}
      (service/save-plan-day conn params)
      (response/content-type
       (response/response
        (json/write-str {}))
       "application/json"))

    (POST "/delete-plan-day" {:keys [params]}
      (service/delete-plan-day conn params)
      (response/content-type
       (response/response
        (json/write-str {}))
       "application/json"))))
