(ns cdc-init.components.kafka
  "A component for connecting to and interacting with Apache Kafka."
  (:require [com.stuartsierra.component :as component]
            [clj-kafka.zk :as zk]
            [clj-kafka.new.producer :as kafka]
            [cdc-init.components.util :refer [extract-options-from-env]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants

(def consumer-group
  "cdc-init")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ENV var mappings

(def env-keys->option-names
  {:zookeeper "zookeeper.connect"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component

(defrecord Kafka [env]
  component/Lifecycle
  (start [this]
    (let [options (extract-options-from-env env env-keys->option-names)
          brokers (-> options zk/brokers zk/broker-list)
          config (merge options {"bootstrap.servers" brokers
                                 "group.id" consumer-group
                                 "auto.commit.enable" "false"})
          producer (kafka/producer config
                                   (kafka/string-serializer)
                                   (kafka/string-serializer))]
      (assoc this :config config :producer producer)))
  (stop [this]
    (if-let [p (:producer this)]
      (.close p))
    (dissoc this :producer)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn new-kafka
  "Returns a new, un-started, Kafka component.

  Requires an `:env` map containing a `:zookeeper` key/connection
  string value."
  []
  (component/using
   (map->Kafka {})
   [:env]))