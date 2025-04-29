(ns metabase.driver.arangodb
  (:require [metabase.driver :as driver]
            [metabase.driver.arangodb.connection :as conn]
            [metabase.lib.metadata :as lib.metadata]
            [metabase.query-processor.store :as qp.store]
            [metabase.query-processor.reducible :as qp.reducible])
  (:import [com.arangodb.entity ArangoDBVersion CollectionEntity]
           [com.arangodb.model CollectionsReadOptions]
           [com.arangodb ArangoCursor ArangoDB$Builder ArangoDatabase]
           [java.util Iterator Map]
           [org.apache.commons.collections4 IteratorUtils]
           [org.apache.commons.collections4.iterators PeekingIterator]))

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

(defn- row->columns-list [row]
  (if (instance? Map row)
    (.keySet row)
    ["value"]))

(defn- row->extraction-fn [row]
  (if (instance? Map row)
    vals
    vector))

(defn- columns->result-metadata
  "Wraps provided columns list into data structure required for Metabase"
  [columns-list]
  {:cols (mapv (fn [c] {:name c}) columns-list)})

(defn- result-metadata [row]
  (-> (row->columns-list row)
      (columns->result-metadata)))

(defn ^PeekingIterator peeking-iterator
  "Wraps the provided iterator with one-element lookahead ability"
  [^Iterator i]
  (IteratorUtils/peekingIterator i))

(defn- reducible-rows [^Iterator iterator extraction-fn]
  (let [row-thunk (fn []
                    (when (.hasNext iterator)
                      (extraction-fn (.next iterator))))]
    (qp.reducible/reducible-rows row-thunk)))

(defn- handle-results [respond ^Iterator iterator]
  (let [iterator (peeking-iterator iterator)]
    (if-let [first-row (.peek iterator)]
      (respond
        (result-metadata first-row)
        (->> (row->extraction-fn first-row)
             (reducible-rows iterator)))
      (respond {} []))))

(defmethod driver/execute-reducible-query :arangodb [_ query _context respond]
  (let [query-str (get-in query [:native :query])
        db-model (lib.metadata/database (qp.store/metadata-provider))
        ^ArangoDatabase db (conn/get-db-connection db-model)]
    (with-open [^ArangoCursor cursor (.query db query-str nil)]
      (handle-results respond cursor))))
