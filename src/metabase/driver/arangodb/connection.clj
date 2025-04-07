(ns metabase.driver.arangodb.connection)

(defonce db-registry (atom {}))

(defn- new-db-wrapper [params connection]
  {:params params :connection connection})

(defn- same-spec? [db db-params]
  (= (:params db) db-params))

(defmulti db-params->connection (fn [driver _params] driver))
(defmulti shutdown-db (fn [driver _connection] driver))

(defn- delayed-db-update [driver old-db new-params]
  (delay
    (when (and old-db (not (delay? old-db)))
      (shutdown-db driver (:connection old-db)))
    (new-db-wrapper
      new-params
      (db-params->connection driver new-params))))

(defn- thread-safe-db-registration [driver db-id db-params]
  (let [update-fn #(delayed-db-update driver % db-params)]
    (swap! db-registry update db-id update-fn)))

(defn- unwrap-connection [db]
  (-> (if (delay? db) (deref db) db)
      (:connection)))

(defn get-db-connection [driver db-id db-spec]
  (let [db (get @db-registry db-id)]
    (if-not (and db (not (delay? db)) (same-spec? db db-spec))
      (-> (thread-safe-db-registration driver db-id db-spec)
          (get db-id)
          (unwrap-connection))
      (unwrap-connection db))))
