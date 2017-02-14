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
    [clojure.tools.logging :refer [error]]
    [tcp-driver.io.pool :as tcp-pool]
    [tcp-driver.io.conn :as tcp-conn]
    [tcp-driver.routing.policy :as routing]
    [tcp-driver.routing.retry :as retry]))


;;;;;;;;;;;;;;
;;;;;; Schemas and Protocols

(def IRouteSchema (s/pred #(satisfies? routing/IRoute %)))

(def IRetrySchema (s/pred #(satisfies? retry/IRetry %)))

(def IPoolSchema (s/pred #(satisfies? tcp-pool/IPool %)))

(def DriverRetSchema {:pool           tcp-pool/IPoolSchema
                      :routing-policy IRouteSchema
                      :retry-policy   IRetrySchema})


;;;;;;;;;;;;;;
;;;;;; Private functions

(defn throw-no-connection! []
      (throw (RuntimeException. "No connection is available to perform the send")))

(defn select-send!
      "ctx - DriverRetSchema
       io-f - function that takes a connection and on error throws an exception
       timeout-ms - connection timeout"
      [ctx io-f timeout-ms]
      {:pre [ctx io-f timeout-ms]}
      (loop [i 0]
            (if-let [host (routing/-select-host (:routing-policy ctx))]

                    (let [pool (:pool ctx)]

                         ;;;try the io-f, if an exception then only if we haven't tried (count hosts) already
                         ;;;loop and retry, its expected that the routing policy blacklist or remove the host on error

                         (let [res (try

                                     (if-let [conn (tcp-pool/borrow pool host timeout-ms)]

                                             (try
                                               (io-f conn)
                                               (finally
                                                 (tcp-pool/return pool host conn)))

                                             (throw-no-connection!))

                                     (catch Exception t
                                       (routing/-on-error! (:routing-policy ctx) host t)
                                       (ex-info (str "Error while connecting to " host) {:throwable t :host host :retries i :hosts (routing/-hosts (:routing-policy ctx))})))]

                              (if (instance? Throwable res)
                                (do
                                  (error res)
                                  (if (< i (count (routing/-hosts (:routing-policy ctx))))
                                    (recur (inc i))
                                    (throw res)))
                                res)))

                    (throw-no-connection!))))

(defn retry-select-send!
      "Send with the retry-policy, select-send! will be retried depending on the retry policy"
      [{:keys [retry-policy] :as ctx} io-f timeout-ms]
      {:pre [retry-policy]}
      (retry/with-retry retry-policy #(select-send! ctx io-f timeout-ms)))


;; routing-policy is a function to which we pass the routing-env atom, which contains {:hosts (set [tcp-conn/HostAddressSchema]) } by default
(s/defn create [pool :- IPoolSchema
                routing-policy :- IRouteSchema
                retry-policy :- IRetrySchema
                ] :- DriverRetSchema
        {:pool           pool
         :routing-policy routing-policy
         :retry-policy   retry-policy})

;;;;;;;;;;;;;;;;
;;;;;; Public API

(defn send-f
      "
       Apply the io-f with a connection from the connection pool selected based on
         the retry policy, and retried if exceptions in the io-f based on the retry policy
       ctx - returned from create
       io-f - function that should accept the tcp-driver.io.conn/ITCPConn
       timeout-ms - the timeout for connection borrow"
      [ctx io-f timeout-ms]
      (retry-select-send! ctx io-f timeout-ms))


(defn create-default
      "Create a driver with the default settings for tcp-pool, routing and retry-policy
       hosts: a vector or seq of {:host :port} maps
       return: DriverRetSchema

       Routing policy: The default routing policy will select hosts at random and on any exception blacklist a particular host.
                       To add/remove/blacklist a node use the public functions add-host, remove-host and blacklist-host in this namespace.
      "
      ^{:arg-lists [routing-conf pool-conf retry-limit]}
      [hosts & {:keys [routing-conf pool-conf retry-limit] :or {retry-limit 10 routing-conf {} pool-conf {}}}]
      {:pre [
             (s/validate tcp-pool/PoolConfSchema pool-conf)
             (s/validate [tcp-conn/HostAddressSchema] hosts)
             (number? retry-limit)
             ]}
      (create
        (tcp-pool/create-tcp-pool pool-conf)
        (apply routing/create-default-routing-policy hosts (mapcat identity routing-conf))
        (retry/retry-policy retry-limit)))

(defn close
      "Close the driver connection pool"
      ^{:arg-lists [pool]}
      [{:keys [pool]}]
      (tcp-pool/close pool))

(defn add-host [{:keys [routing-policy]} host]
      {:pre [(s/validate tcp-conn/HostAddressSchema host)]}
      (routing/-add-host! routing-policy host))

(defn remove-host [{:keys [routing-policy]} host]
      {:pre [(s/validate tcp-conn/HostAddressSchema host)]}
      (routing/-remove-host! routing-policy host))

(defn blacklist-host [{:keys [routing-policy]} host]
      {:pre [(s/validate tcp-conn/HostAddressSchema host)]}
      (routing/-blacklist! routing-policy host))


