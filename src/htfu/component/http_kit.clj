(ns htfu.component.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-kit]
            [suspendable.core :as suspendable]
            [taoensso.timbre :as timbre]))

(defrecord HttpKitServer [app]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [options (dissoc component :app)
            handler (atom (delay (:handler app)))
            server  (http-kit/run-server (fn [req] (@@handler req)) options)]
        (timbre/info "Started http-kit server")
        (assoc component
               :server  server
               :handler handler))))
  (stop [component]
    (if-let [server (:server component)]
      (do (server)
          (timbre/info "Stopped http-kit server")
          (dissoc component :server :handler))
      component)))

(defn http-kit-server
  ([options]
   (map->HttpKitServer options)))
