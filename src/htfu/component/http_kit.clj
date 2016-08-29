(ns htfu.component.http-kit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-kit]
            [suspendable.core :as suspendable]))

(defrecord HttpKitServer [app]
  component/Lifecycle
  (start [component]
    (if (:server component)
      component
      (let [options (dissoc component :app)
            handler (atom (delay (:handler app)))
            server  (http-kit/run-server (fn [req] (@@handler req)) options)]
        (assoc component
               :server  server
               :handler handler))))
  (stop [component]
    (if-let [server (:server component)]
      (do (server)
          (dissoc component :server :handler))
      component))
  suspendable/Suspendable
  (suspend [component]
    (if-let [handler (:handler component)]
      (do (reset! handler (promise))
          (assoc component :suspended? true))
      component))
  (resume [component old-component]
    (if (and (:suspended? old-component)
             (= (dissoc component :suspended? :server :handler :app)
                (dissoc old-component :suspended? :server :handler :app)))
      (let [handler (:handler old-component)]
        (deliver @handler (:handler app))
        (-> component
            (assoc :server (:server old-component), :handler handler)
            (dissoc :suspended?)))
      (do (when old-component (component/stop old-component))
          (component/start component)))))

#_(def Options
  {(s/optional-key :ip) sc/IpAddress
   (s/optional-key :port) sc/Port
   (s/optional-key :thread) sc/PosInt
   (s/optional-key :worker-name-prefix) s/Str
   (s/optional-key :queue-size) sc/PosInt
   (s/optional-key :max-body) sc/PosInt
   (s/optional-key :max-line) sc/PosInt})

(defn http-kit-server
  ([options]
   (map->HttpKitServer options)))
