(ns htfu.db.service
  (:require [clojure.pprint :refer [pprint]]
            [taoensso.timbre :as timbre]
            [taoensso.truss :refer [have have! have?]]
            [htfu.cljc.cljc-util :as cljc-util]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defn pull-by-id [db id]
  (d/pull db "[*]" id))

(defn find-all-exercises [db]
  (d/q '[:find [(pull ?exercise [*])...]
         :where
         [?exercise :item/title _]]
       db))

(defn save-item [conn item]
  (let [tx-data [{:db/id (cljc-util/new-id)
                  :item/title (:title item)
                  :item/desc (:desc item)}]]
    (timbre/info "transact tx-data type: " (type tx-data))
    (timbre/info "transact tx-data: \n" (with-out-str (pprint tx-data)))
    (let [tx-result @(d/transact conn tx-data)]
      (timbre/info "result: \n" (with-out-str (pprint tx-result))))))




