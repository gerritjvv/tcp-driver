(ns
  ^{:doc "TCP Connection abstractions and implementations
          see host-address and tcp-conn-factory"}
  tcp-driver.io.conn
  (:require [schema.core :as s])
  (:import
    (java.net InetAddress Socket SocketAddress InetSocketAddress)
    (org.apache.commons.pool2 BaseKeyedPooledObjectFactory PooledObject KeyedPooledObjectFactory)
    (org.apache.commons.pool2.impl  DefaultPooledObject)
    (java.io InputStream OutputStream)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;Protocol & Data

(def HostAddressSchema {:host s/Str :port s/Int s/Any s/Any})

(defrecord HostAddress [^String host ^int port])

(defprotocol ITCPConn
  (-input-stream [this])
  (-output-stream [this])
  (-close [this])
  (-valid? [this]))

(def ITCPConnSchema (s/pred (partial satisfies? ITCPConn)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;Private

(defrecord SocketConn [^Socket socket]
  ITCPConn
  (-input-stream [_] (.getInputStream socket))
  (-output-stream [_] (.getOutputStream socket))
  (-close [_] (.close socket))
  (-valid? [_] (and
                 (.isConnected socket)
                 (not (.isClosed socket)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;; Public API

(defn wrap-tcp-conn
  "Wrap the Socket in a ITCPConn"
  [^Socket socket]
  (->SocketConn socket))

(defn create-tcp-conn [{:keys [host port]}]
  {:pre [(string? host) (number? port)]}
  (->SocketConn
    (doto (Socket.)
      (.connect (InetSocketAddress. (str host) (int port)))
      (.setKeepAlive true))))


(defn
  ^InputStream
  input-stream [conn]
  (-input-stream conn))

(defn
  ^OutputStream
  output-stream [conn]
  (-output-stream conn))

(defn close! [conn]
  (-close conn))

(defn valid? [conn]
  (-valid? conn))

(defn ^HostAddress host-address
  "Creates a host address instance using host and port"
  [host port]
  {:pre [(string? host) (number? port)]}
  (->HostAddress host port))

(defn ^KeyedPooledObjectFactory tcp-conn-factory
  "Return a keyed pool factory that return ITCPConn instances
   The keys used should always be instances of HostAddress or implement host and port keys

   post-create-fn: is called after the connection has been created
   pre-destroy-fn is called before the connection is destroyed"
  ([]
    (tcp-conn-factory identity identity))
  ([post-create-fn pre-destroy-fn]
    (let [post-create-fn' (if post-create-fn post-create-fn :conn)
          pre-destroy-fn' (if pre-destroy-fn pre-destroy-fn :conn)]

         (proxy
           [BaseKeyedPooledObjectFactory]
           []
           (create [address]
                   (s/validate HostAddressSchema address)

                   (let [conn (create-tcp-conn address)]
                        (post-create-fn' {:address address :conn conn})))

           (wrap [v] (DefaultPooledObject. v))

           (destroyObject [address ^PooledObject v]
                          (let [conn (.getObject v)]
                               (pre-destroy-fn' {:address address :conn conn})
                               (close! conn)))

           (validateObject [_ ^PooledObject v]
                           (valid? (.getObject v)))))))