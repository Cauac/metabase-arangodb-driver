(ns metabase.driver.arangodb
  (:require [metabase.driver :as driver]
            [metabase.driver.arangodb.connection :as conn]
            [metabase.lib.metadata :as lib.metadata]
            [metabase.query-processor.store :as qp.store]
            [metabase.query-processor.reducible :as qp.reducible])
  (:import [com.arangodb.entity ArangoDBVersion CollectionEntity]
           [com.arangodb.model CollectionsReadOptions]
           [com.arangodb ArangoCursor ArangoDB$Builder ArangoDatabase]))

(driver/register! :arangodb)

(defmethod driver/database-supports? [:arangodb :basic-aggregations] [_ _ _] false)

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
;;; |                                               Database sync                                                    |
;;; +----------------------------------------------------------------------------------------------------------------+
;;;
;;; Metabase sync interface: https://github.com/metabase/metabase/blob/master/src/metabase/sync/interface.clj
;;; Metabase supported types: https://github.com/metabase/metabase/blob/master/src/metabase/types.cljc
;;;
(defmethod driver/can-connect? :arangodb [_ db-details]
  (-> (conn/get-db-connection :arangodb :connection-test db-details)
      (.exists)))

(defmethod driver/dbms-version :arangodb [_ db-model]
  (let [db (conn/get-db-connection db-model)
        ^ArangoDBVersion server-info (.getVersion db)]
    {:version (.getVersion server-info)
     :flavour (.toString (.getLicense server-info))}))

(defn- collection->table-description [^CollectionEntity col]
  {:name (.getName col)
   :schema nil})

(defmethod driver/describe-database :arangodb [_ db-model]
  (let [db (conn/get-db-connection db-model)
        filter (-> (CollectionsReadOptions.)
                   (.excludeSystem true))
        tables (->> (.getCollections db filter)
                    (map collection->table-description))]
    {:tables (set tables)}))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                               Query execution                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn- result-metadata [first-row]
  {:cols (mapv (fn [col-name] {:name col-name}) (.keySet first-row))})

(defn- reducible-rows [^ArangoCursor cursor first-row]
  (let [has-returned-first-row? (volatile! false)
        restored-iterator (fn []
                            (if-not @has-returned-first-row?
                              (do (vreset! has-returned-first-row? true)
                                  (vals first-row))
                              (when (.hasNext cursor)
                                (vals (.next cursor)))))]
    (qp.reducible/reducible-rows restored-iterator)))

(defn- reduce-results [^ArangoCursor cursor respond]
  (if-let [first-row (when (.hasNext cursor) (.next cursor))]
    (respond (result-metadata first-row) (reducible-rows cursor first-row))
    (respond {} [])))

(defmethod driver/execute-reducible-query :arangodb [_ query _context respond]
  (let [query-str (get-in query [:native :query])
        db-model (lib.metadata/database (qp.store/metadata-provider))
        ^ArangoDatabase db (conn/get-db-connection db-model)]
    (with-open [^ArangoCursor cursor (.query db query-str nil)]
      (reduce-results cursor respond))))
