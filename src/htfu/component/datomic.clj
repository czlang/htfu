(ns htfu.component.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [io.rkn.conformity :as c]
            [taoensso.timbre :as timbre]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [db (d/create-database uri)
          conn (d/connect uri)
          norms-map (c/read-resource "htfu/htfu_schema.edn")]
      (timbre/info "Obtained datomic connection" conn)
      (c/ensure-conforms conn norms-map)
      (timbre/info "Conformity finished")
      (assoc component :spec conn)))
  (stop [component]
    (timbre/info "Discarding datomic connection")
    (assoc component :spec nil)))

(defn datomic [uri]
  (map->Datomic {:uri uri}))
