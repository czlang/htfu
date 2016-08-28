(ns htfu.cljc.schema
  (:require [schema.core :as s]))

(def AppDb
  {:current-page s/Keyword
   (s/optional-key :auth-user) s/Any})
