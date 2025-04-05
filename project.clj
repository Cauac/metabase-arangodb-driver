(defproject metabase/arangodb-driver "0.1.0-SNAPSHOT"

  :min-lein-version "2.5.0"

  :dependencies [[com.arangodb/arangodb-java-driver "7.17.1"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.12.0"]
                                       [metabase/metabase-core "0.52.17"]]}
             :uberjar {:auto-clean    true
                       :aot           :all
                       :javac-options ["-target" "1.8", "-source" "1.8"]
                       :target-path   "target/%s"
                       :uberjar-name  "arangodb.metabase-driver.jar"}})
