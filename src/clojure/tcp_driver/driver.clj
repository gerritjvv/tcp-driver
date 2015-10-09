(ns
  ^{:doc "

  The idea is the access TCP client connections like any other product driver code would e.g the cassandra or mondodb driver.
  There are allot of situations where software in the past (my own experience) became unstable because the TCP connections
  were not written or treated with the equivalent importance as server connections.
  Writing the TCP connection as if it were a product driver sets a certain design mindset.

  This is the main entry point namespace for this project, the other namespaces are:

   tcp-driver.io.conn   -> TCP connection abstractions
   tcp-driver.io-pool   -> Connection pooling and creating object pools
   tcp-driver.io-stream -> reading and writing from TCP Connections

   The main idea is that a driver can point at 1,2 or more servers, for each server a Pool of Connections are maintained
   using a KeyedObjectPool  from the commons pool2 library.

   Pooling connections is done not only for performance but also make connection error handling easier, the connection
   is tested and retried before given to the application user, and if you have a connection at least at the moment
   of handoff you know that it is connection and ready to go.

  "}
  tcp-driver.driver

  (:require
    [schema.core :as s]
    [tcp-driver.io.pool :as tcp-pool]
    [tcp-driver.io.conn :as tcp-conn]
    [tcp-driver.routing.policy :as routing]))


;;;;;;;;;;;;;;
;;;;;; Schemas and Protocols

(def IRouteSchema (s/pred #(satisfies? routing/IRoute %)))

(def DriverRetSchema {:pool tcp-pool/IPoolSchema
                      :bootstrap-hosts [tcp-conn/HostAddressSchema]
                      :routing-policy IRouteSchema
                      :routing-env {:hosts s/Any}})


;;;;;;;;;;;;;;
;;;;;; Private functions

(defn routing-hosts
  "Extract and deref the routing hosts"
  [ctx]
  @(get-in ctx [:rounting-env :hosts]))

(defn select-host [{:keys [routing-policy rounting-env]}]
  (routing-policy rounting-env))

(defn select-send [ctx io-f timeout-ms]
  (let []))

;;;;;;;;;;;;;;;;
;;;;;; Public API

;; rounting-policy is a function to which we pass the rounting-env atom, which contains {:hosts (set [tcp-conn/HostAddressSchema]) } by default
(s/defn create [bootstrap-hosts :- [tcp-conn/HostAddressSchema]
                pool-conf       :- tcp-pool/PoolConfSchema
                routing-policy  :- IRouteSchema
                retry-policy    :- s/Any
                conf] :- DriverRetSchema
  {:pool (tcp-pool/create-tcp-pool pool-conf)
   :rounting-policy routing-policy
   :rounting-env (atom {:hosts (set bootstrap-hosts)})})


