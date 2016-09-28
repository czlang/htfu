(ns htfu.db.service
  (:require [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as timbre]
            [taoensso.truss :refer [have have! have?]]
            [htfu.cljc.cljc-util :as cljc-util]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defn pull-by-id [db id]
  (d/pull db "[*]" id))

(defn save-item [conn item]
  (let [tx-data [{:db/id (cljc-util/new-id)
                  :ex/title (:title item)
                  :ex/desc (:desc item)}]]
    (timbre/info "transact tx-data type: " (type tx-data))
    (timbre/info "transact tx-data: \n" (with-out-str (pprint tx-data)))
    (let [tx-result @(d/transact conn tx-data)]
      (timbre/info "result: \n" (with-out-str (pprint tx-result))))))

(defn find-all-groups [db]
  (d/q '[:find
         [(pull ?ex-group [*])...]
         :where
         [?ex-group :ex-group/title _]]
       db))

(defn find-exs-by-group-id [db group-id]
  (d/q '[:find
         [(pull ?ex [*])...]
         :in $ ?ex-group
         :where
         [?ex :ex/group ?ex-group]]
       db group-id))

(defn find-all-groups-with-exs [db]
  (let [groups (find-all-groups db)]
    (map
     #(assoc % :exs (find-exs-by-group-id db (:db/id %)))
     groups)))
