(ns htfu.cljc.cljc-util
  (:require [clojure.string :as str])
  #?(:clj
     (:require
      [datomic.api :as d])))

(defonce id-counter (atom 0))

(defn new-id []
  #?(:clj  (d/tempid :db.part/user)
     :cljs (swap! id-counter dec)))
