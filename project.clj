(defproject metabase/arangodb-driver "1.0.0"

  :min-lein-version "2.5.0"

  :dependencies [[com.arangodb/arangodb-java-driver "7.17.1"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.12.0"]
                                       [metabase/metabase-core "0.52.17"]]}
             :uberjar {:auto-clean    true
                       :aot           :all
                       :target-path   "target/%s"
                       :uberjar-name  "arangodb.metabase-driver.jar"}})
