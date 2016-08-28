(ns htfu.system
  (:require [clj-brnolib.component.http-kit :refer [http-kit-server]]
            [clj-brnolib.component.nrepl-server :refer [nrepl-server]]
            [clj-brnolib.component.sente :refer [new-channel-socket-server]]
            [clj-brnolib.middleware :as middleware]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [htfu.endpoint.data-api :refer [data-api-endpoint]]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [duct.middleware.route-aliases :refer [wrap-route-aliases]]
            [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.sente.server-adapters.http-kit
             :refer
             [sente-web-server-adapter]]
            [taoensso.timbre :as timbre]))

(def base-config
  {:app {:middleware [middleware/wrap-logging
                      #_[middleware/wrap-auth :api-routes-pattern]
                      [middleware/wrap-exceptions :api-routes-pattern]
                      [wrap-not-found :not-found]
                      [wrap-defaults :defaults]
                      [wrap-route-aliases :aliases]]
         :api-routes-pattern #"/data-api"
         :not-found  (io/resource "htfu/errors/404.html")
         :defaults   (meta-merge site-defaults (cond-> {:static {:resources "htfu/public"}
                                                        :security {:anti-forgery false}
                                                        :proxy true}
                                                 (:dev env)
                                                 (assoc :session {:store (cookie/cookie-store {:key "hardenthefuckup!"})})))
         :aliases    {}}})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :nrepl (nrepl-server (:nrepl-port config))
         :http (http-kit-server (:http config))
         :sente (new-channel-socket-server sente-web-server-adapter)
         :app  (handler-component (:app config))
         :data-api (endpoint-component data-api-endpoint))
        (component/system-using
         {:http [:app]
          :app  [:data-api]
          :sente []
          :data-api [:sente]}))))
