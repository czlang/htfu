(ns htfu.db.service
  (:require [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as timbre]
            [taoensso.truss :refer [have have! have?]]
            [htfu.cljc.cljc-util :as cljc-util]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defn pull-by-id [db id]
  (d/pull db "[*]" id))

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

(defn find-all-day-num-plans [db]
  (d/q '[:find
         [(pull ?plan [*])...]
         :where
         [?plan :plan/day-num _]]
       db))

(defn find-day-num-plan [db day-num]
  (d/q '[:find
         [(pull ?plan [*])...]
         :in $ ?day-num
         :where
         [?plan :plan/day-num ?day-num]]
       db day-num))

(defn save-item [conn item]
  (let [tx-data [{:db/id (cljc-util/new-id)
                  :ex/title (:title item)
                  :ex/desc (:desc item)}]]
    (timbre/info "transact tx-data type: " (type tx-data))
    (timbre/info "transact tx-data: \n" (with-out-str (pprint tx-data)))
    (let [tx-result @(d/transact conn tx-data)]
      (timbre/info "result: \n" (with-out-str (pprint tx-result))))))

(defn save-plan-day [conn plan-day-data]
  (let [plan-day-num (first (find-day-num-plan (d/db conn) (:day-num plan-day-data)))
        ccc (pprint plan-day-num)
        tx-data [{:db/id (or (:db/id plan-day-num)
                             (cljc-util/new-id))
                  :plan/day-num (:day-num plan-day-data)
                  (keyword (str "plan") (str (:type-keyw plan-day-data) "s"))
                  [{:db/id (:value plan-day-data)}]}]]
    (timbre/info "transact tx-data type: " (type tx-data))
    (timbre/info "transact tx-data: \n" (with-out-str (pprint tx-data)))
    (let [tx-result @(d/transact conn tx-data)]
      (timbre/info "result: \n" (with-out-str (pprint tx-result))))))


