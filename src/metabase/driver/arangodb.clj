(ns metabase.driver.arangodb
  (:require [metabase.driver :as driver]
            [toucan.db :as db]
            [metabase.util :as metabase-utils]
            [metabase.models.database :refer [Database]])
  (:import [java.util Map]
           [com.arangodb ArangoDB$Builder ArangoDatabase ArangoDBException]))

(driver/register! :arangodb)

(def db-connections (atom {}))

(defn- database-details [database-id]
  (:details
    (db/select-one [Database :id :engine :details] :id database-id)))

(defn- db-connection [{:keys [host port dbname user password]}]
  (let [connection (.build (doto (ArangoDB$Builder.)
                             (.host host port)
                             (.user user)
                             (.password password)))]
    (.db connection dbname)))

(defn- get-connection [database]
  (let [id (metabase-utils/get-id database)]
    (or (get @db-connections id)
        (let [db-conn (-> (database-details id)
                          (db-connection))]
          (swap! db-connections assoc id db-conn)
          db-conn))))

(defn- query-as-single-column [^ArangoDatabase db ^String query]
  (reduce
    #(conj %1 {"column" %2}) [] (.query db query nil nil String)))

(defn- query [^ArangoDatabase db ^String query]
  (try
    (vec (.query db query nil nil Map))
    (catch ArangoDBException _
      (query-as-single-column db query))))

(defmethod driver/supports? [:arangodb :basic-aggregations] [_ _] false)

(defmethod driver/describe-database :arangodb [_ _]
  {:tables #{}})

(defmethod driver/can-connect? :arangodb [_ db-details]
  (-> (db-connection db-details)
      (.exists)))

(defmethod driver/execute-query :arangodb [_ query-params]
  (let [db-connection (get-connection (:database query-params))
        query-str (get-in query-params [:native :query])
        results (query db-connection query-str)
        columns (map name (keys (first results)))
        rows (map vals results)]
    {:columns columns
     :rows    rows}))