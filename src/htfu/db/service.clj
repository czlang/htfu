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
  (d/transact conn [{:db/id (cljc-util/new-id)
                     :item/title (:title item)
                     :item/desc (:desc item)}]))




