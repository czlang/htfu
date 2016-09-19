(ns htfu.system
  (:require [htfu.component.http-kit :refer [http-kit-server]]
            [htfu.component.nrepl-server :refer [nrepl-server]]
            [htfu.middleware :as middleware]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [htfu.component.datomic :refer [datomic]]
            [htfu.endpoint.data-api :refer [data-api-endpoint]]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [duct.middleware.route-aliases :refer [wrap-route-aliases]]
            [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.json :refer [wrap-json-params]]
            [taoensso.timbre :as timbre]))

(def base-config
  {:app {:middleware [middleware/wrap-logging
                      [middleware/wrap-auth :api-routes-pattern]
                      [middleware/wrap-exceptions :api-routes-pattern]
                      [wrap-not-found :not-found]
                      [wrap-defaults :defaults]
                      [wrap-route-aliases :aliases]
                      [wrap-json-params :json-params]]
         :api-routes-pattern #"/data-api"
         :not-found  (io/resource "htfu/errors/404.html")
         :defaults   (meta-merge site-defaults (cond-> {:static {:resources "htfu/public"}
                                                        :security {:anti-forgery false}
                                                        :proxy true}
                                                 (:dev env)
                                                 (assoc :session {:store (cookie/cookie-store {:key "hardenthefuckup!"})})))
         :aliases    {}}})

(defn new-system [config]
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (timbre/error ex "Uncaught exception on" (.getName thread)))))
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :nrepl (nrepl-server (:nrepl-port config))
         :db (datomic (get-in config [:db :uri]))
         :http (http-kit-server (:http config))
         :app  (handler-component (:app config))
         :data-api (endpoint-component data-api-endpoint))
        (component/system-using
         {:http [:app]
          :app  [:data-api]
          :data-api [:db]}))))
