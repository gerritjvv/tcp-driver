(ns
  ^{:doc "TCP connection pools
          see: create-tcp-pool"}
  tcp-driver.io.pool
  (:require
    [tcp-driver.io.conn :as tcp-conn])
  (:import
    (org.apache.commons.pool2 KeyedObjectPool BaseKeyedPooledObjectFactory)
    (java.net SocketAddress)
    (org.apache.commons.pool2.impl GenericKeyedObjectPool GenericKeyedObjectPoolConfig)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;Protocols



(defprotocol IPool
  (-borrow [this key timeout-ms])
  (-return [this key obj])
  (-invalidate [this key obj])
  (-close [this])
  (-num-idle [this] [this key])
  (-num-active [this] [this key]))

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
   key an instance of tcp-driver.io.conn.HostAddress
   timeout-ms long timeout in milliseconds
  Exceptions: NoSuchElementException, Exception"
  [pool key timeout-ms]
  (-borrow pool key timeout-ms))

(defn return [pool key conn]
  (-return pool key conn))

(defn invalidate [pool key conn]
  (-invalidate pool key conn))

(defn close [pool]
  (-close pool))

(defn num-idle
  ([pool] (-num-idle pool))
  ([pool key] (-num-idle pool key)))

(defn num-active
  ([pool] (-num-active pool))
  ([pool key] (-num-active pool key)))

(defn
  ^IPool
  create-tcp-pool
  "
  Params:
   conf keys are: max-idle-per-key, max-total, max-total-per-key, min-idle-per-key
  Return a IPool instance using the configuration passed in"
  [conf]
  (->KeyedTCPConnFactory (GenericKeyedObjectPool. (tcp-conn/tcp-conn-factory)
                                                  (keyed-pool-config conf))))

