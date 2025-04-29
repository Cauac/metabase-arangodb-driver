(ns metabase.driver.arangodb-test
  (:require [clojure.test :refer :all]
            [metabase.driver.arangodb :as driver]))

(deftest first-test
  (testing "Testing tests"
    (is (= 1 (parse-long "1")))))


(deftest result-metadata-test
  (testing "Testing columns names extraction from query results"
    (let [f #'driver/result-metadata]
      (testing "Testing a typical scenario. Each row is a map. Expecting a proper list of columns."
        (let [input {"id" 1
                     "name" "John"
                     "age" 50}
              expected {:cols [{:name "id"} {:name "name"} {:name "age"}]}]
          (is (= expected (f input))))
        (let [input {"column_1" [1 2]
                     "column-2" {}}
              expected {:cols [{:name "column_1"} {:name "column-2"}]}]
          (is (= expected (f input)))))
      (testing "Testing scenario when result isn't a map. Expected single 'value' column"
        (let [expected {:cols [{:name "value"}]}]
          (is (= expected (f nil)))
          (is (= expected (f 1)))
          (is (= expected (f "text")))
          (is (= expected (f ["array" "with" "values" 1.0]))))))))
