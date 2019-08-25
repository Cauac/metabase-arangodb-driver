(defproject metabase/arangodb-driver "0.1.0-SNAPSHOT"

  :min-lein-version "2.5.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.arangodb/arangodb-java-driver "6.0.0"]
                 [metabase-core "1.0.0-SNAPSHOT" :scope "provided"]]

  :profiles {:uberjar {:auto-clean    true
                       :aot           :all
                       :javac-options ["-target" "1.8", "-source" "1.8"]
                       :target-path   "target/%s"
                       :uberjar-name  "arangodb.metabase-driver.jar"}})
