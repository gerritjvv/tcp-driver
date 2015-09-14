(ns
  ^{:doc "TCP connection pools
          see: create-tcp-pool

          Pool keys:
            Note keys (not keywords) passed are in fact host addresses of the format {:host <host> :port <port>}
            The tcp-driver.io.conn/host-address can be used for convinience to return a record
            Any other key types will result in a runtime exception on connection creation"}
  tcp-driver.io.pool
  (:require
    [tcp-driver.io.conn :as tcp-conn]
    [schema.core :as s])
  (:import
    (org.apache.commons.pool2 KeyedObjectPool BaseKeyedPooledObjectFactory)
    (java.net SocketAddress)
    (org.apache.commons.pool2.impl GenericKeyedObjectPool GenericKeyedObjectPoolConfig)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;Protocols

(def PoolConfSchema {(s/optional-key :max-idle-per-key) s/Int
                     (s/optional-key :max-total) s/Int
                     (s/optional-key :max-total-per-key) s/Int
                     (s/optional-key :min-idle-per-key) s/Int})

(defprotocol IPool
  (-borrow [this key timeout-ms])
  (-return [this key obj])
  (-invalidate [this key obj])
  (-close [this])
  (-num-idle [this] [this key])
  (-num-active [this] [this key]))


(def IPoolSchema (s/pred (partial extends? IPool)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;Private



(defrecord KeyedTCPConnFactory [^GenericKeyedObjectPool pool]

  IPool
  (-borrow [_ key timeout-ms] (.borrowObject pool key (long timeout-ms)))
  (-return [_ key obj] (.returnObject pool key obj))
  (-invalidate [_ key obj] (.invalidateObject pool key obj))
  (-close [_] (.close pool))

  (-num-active [_] (.getNumActive pool))
  (-num-active [_ key] (.getNumActive pool key))

  (-num-idle [_] (.getNumIdle pool))
  (-num-idle [_ key] (.getNumIdle pool key)))


(defn keyed-pool-config
  "Create a pool config with block when exhausted set to true"
                        [{:keys [max-idle-per-key
                                 max-total
                                 max-total-per-key
                                 min-idle-per-key]

                          :or   {max-idle-per-key 2
                                 max-total 100
                                 max-total-per-key 100
                                 min-idle-per-key  0}}]

  (doto
    (GenericKeyedObjectPoolConfig.)
    (.setBlockWhenExhausted true)
    (.setTestOnBorrow true)
    (.setMaxIdlePerKey (int max-idle-per-key))
    (.setMaxTotal (int max-total))
    (.setMaxTotalPerKey (int max-total-per-key))
    (.setMinIdlePerKey (int min-idle-per-key))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;; Public API
;;;; Remember that all keys should be created by tcp-driver.io.conn/host-address

(defn borrow
  "
  Params:
   pool an instance of IPool
   address an instance of tcp-driver.io.conn.HostAddress or map of :host and :port
   timeout-ms long timeout in milliseconds
  Exceptions: NoSuchElementException, Exception"
  [pool address timeout-ms]
  {:pre [(s/validate tcp-conn/HostAddressSchema address)]}
  (-borrow pool address timeout-ms))

(defn return
  "Return conn to the pool
   Params:
     pool instanceof of IPool
     address an instance of tcp-driver.io.conn.HostAddress or map of :host and :port
     conn connection to return"
  [pool address conn]
  {:pre [(s/validate tcp-conn/HostAddressSchema address)]}
  (-return pool address conn))

(defn invalidate [pool address conn]
  {:pre [(s/validate tcp-conn/HostAddressSchema address)]}
  (-invalidate pool address conn))

(defn close [pool]
  (-close pool))

(defn num-idle
  ([pool] (-num-idle pool))
  ([pool key] (-num-idle pool key)))

(defn num-active
  ([pool] (-num-active pool))
  ([pool key] (-num-active pool key)))


(s/defn
  create-tcp-pool :- IPoolSchema
  [conf :- PoolConfSchema]
  ;;create a tcp pool factory where each key is the address to connect to
  (->KeyedTCPConnFactory (GenericKeyedObjectPool. (tcp-conn/tcp-conn-factory)
                                                  (keyed-pool-config conf))))


(defn try-conn
  "Get a connection, call (f conn) and in a finally clause return the connection to the pool"
  [pool host timeout-ms f]
  (let [conn (borrow pool host timeout-ms)]
    (try
      (f conn)
      (finally
        (return pool host conn)))))