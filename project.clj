(defproject htfu "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :min-lein-version "2.0.0"
  :dependencies [[cljs-ajax "0.5.2"]
                 [org.apache.httpcomponents/httpclient "4.5.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [clj-brnolib "0.1.0-SNAPSHOT"]
                 [com.taoensso/sente "1.8.1"]
                 [compojure "1.5.0"]
                 [duct "0.5.10"]
                 [environ "1.0.2"]
                 [http-kit "2.1.19"]
                 [io.rkn/conformity "0.4.0"]
                 [meta-merge "0.1.1"]
                 [org.slf4j/slf4j-nop "1.7.14"]
                 [secretary "1.2.3"]
                 [ring-middleware-format "0.7.0"]
                 [crypto-password "0.2.0"]
                 [reagent "0.6.0-rc" :exclusions [cljsjs/react]]
                 [cljsjs/material-ui "0.15.4-0"]
                 [cljs-react-material-ui "0.2.21"]]
  :plugins [[lein-environ "1.0.2"]
            [lein-gen "0.2.2"]
            [lein-cljsbuild "1.1.2"]]
  :generators [[duct/generators "0.5.10"]]
  :duct {:ns-prefix htfu}
  :main ^:skip-aot htfu.main
  :target-path "target/%s/"
  :resource-paths ["resources" "target/cljsbuild"]
  :prep-tasks [["javac"] ["cljsbuild" "once"] ["compile"]]
  :cljsbuild
  {:builds
   {:main {:jar true
           :source-paths ["src"]
           :compiler {:output-to "target/cljsbuild/htfu/public/js/main.js"
                      :optimizations :advanced}}}}
  :aliases {"gen"   ["generate"]
            "setup" ["do" ["generate" "locals"]]}
  :profiles
  {:dev  [:project/dev  :profiles/dev]
   :test [:project/test :profiles/test]
   :repl {:resource-paths ^:replace ["resources" "target/figwheel"]
          :prep-tasks     ^:replace [["javac"] ["compile"]]}
   :uberjar {:aot :all}
   :profiles/dev  {}
   :profiles/test {}
   :project/dev   {:dependencies [[reloaded.repl "0.2.1"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [eftest "0.1.1"]
                                  [kerodon "0.7.0"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [duct/figwheel-component "0.3.2"]
                                  [figwheel "0.5.0-6"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :env {:dev "true"
                         :port "3000"}}
   :project/test  {}})
