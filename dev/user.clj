(ns user
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [htfu.config :as config]
            [htfu.system :as system]
            [duct.component.figwheel :as figwheel]
            [eftest.runner :as eftest]
            [meta-merge.core :refer [meta-merge]]
            [reloaded.repl :refer [go init reset start stop system]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]))

(def dev-config
  {:app {:middleware [wrap-stacktrace]}
   :figwheel
   {:css-dirs ["resources/htfu/public/css"]
    :builds   [{:source-paths ["src" "dev"]
                :build-options
                {:optimizations :none
                 :main "cljs.user"
                 :asset-path "/js"
                 :output-to  "target/figwheel/htfu/public/js/main.js"
                 :output-dir "target/figwheel/htfu/public/js"
                 :source-map true
                 :source-map-path "/js"}}]}})

(def config
  (meta-merge config/defaults
              config/environ
              dev-config))

(defn new-system []
  (into (system/new-system config)
        {:figwheel (figwheel/server (:figwheel config))}))

(ns-unmap *ns* 'test)

(defn test []
  (eftest/run-tests (eftest/find-tests "test") {:multithread? false}))

(defn cljs-repl []
  (figwheel/cljs-repl (:figwheel system)))

(when (io/resource "local.clj")
  (load "local"))

(reloaded.repl/set-init! new-system)
