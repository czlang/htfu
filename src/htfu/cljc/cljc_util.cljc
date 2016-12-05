(ns htfu.cljc.cljc-util
  (:require [clojure.string :as str])
  #?(:clj
     (:require
      [datomic.api :as d])))

(defonce id-counter (atom 0))

(defn new-id []
  #?(:clj  (d/tempid :db.part/user)
     :cljs (swap! id-counter dec)))

(defn ffilter [f coll]
  (reduce #(when (f %2) (reduced %2)) nil coll))
