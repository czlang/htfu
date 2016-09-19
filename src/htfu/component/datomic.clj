(ns htfu.component.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [db (d/create-database uri)
          conn (d/connect uri)]
      (assoc component :spec conn)))
  (stop [component]
    (assoc component :spec nil)))

(defn datomic [uri]
  (map->Datomic {:uri uri}))
