(ns metabase.driver.arangodb
  (:require [metabase.driver :as driver]
            [metabase.driver.arangodb.connection :as conn]
    ;[toucan.db :as db]
            [metabase.util :as metabase-utils]
            [metabase.models.database :refer [Database]])
  (:import (com.arangodb.entity ArangoDBVersion CollectionEntity)
           (com.arangodb.model CollectionsReadOptions)
           [java.util Map]
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

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                               Connection                                                       |
;;; +----------------------------------------------------------------------------------------------------------------+
;;;
;;; More connection options here:
;;; https://docs.arangodb.com/stable/develop/drivers/java/reference-version-7/driver-setup/#configuration
;;;
(defmethod conn/db-params->connection :arangodb [_ params]
  (let [{:keys [host port dbname user password]} params
        server (.build (doto (ArangoDB$Builder.)
                         (.host host port)
                         (.user user)
                         (.password password)))]
    (.db server dbname)))

(defmethod conn/shutdown-db :arangodb [_ ^ArangoDatabase connection]
  (.shutdown (.arango connection)))

;;; +----------------------------------------------------------------------------------------------------------------+

(defmethod driver/can-connect? :arangodb [_ db-details]
  (-> (conn/get-db-connection :arangodb :connection-test db-details)
      (.exists)))

(defmethod driver/dbms-version :arangodb [_ db-model]
  (let [db (conn/get-db-connection :arangodb (:id db-model) (:details db-model))
        ^ArangoDBVersion server-info (.getVersion db)]
    {:version (.getVersion server-info)
     :flavour (.toString (.getLicense server-info))}))

(defn- collection->table-description [^CollectionEntity col]
  {:schema nil
   :name (.getName col)
   :type "TABLE"})

(defmethod driver/describe-database :arangodb [_ db-model]
  (let [db (conn/get-db-connection :arangodb (:id db-model) (:details db-model))
        filter (-> (CollectionsReadOptions.)
                   (.excludeSystem true))
        tables (->> (.getCollections db filter)
                    (map collection->table-description))]
    {:tables (set tables)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod driver/execute-query :arangodb [_ query-params]
  (let [db-connection (get-connection (:database query-params))
        query-str (get-in query-params [:native :query])
        results (query db-connection query-str)
        columns (map name (keys (first results)))
        rows (map vals results)]
    {:columns columns
     :rows    rows}))