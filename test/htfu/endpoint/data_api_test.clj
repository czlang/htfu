(ns htfu.endpoint.data-api-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [kerodon.core :refer :all]
            [kerodon.test :refer :all]
            [htfu.endpoint.data-api :as data-api]))

(def handler
  (data-api/data-api-endpoint {}))

(deftest smoke-test
  (testing "example page exists"
    (-> (session handler)
        (visit "/data-api")
        (has (status? 200) "page exists"))))
