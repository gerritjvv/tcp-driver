(ns
  ^{:doc "Protocol interface and default implementation for routing and blacklisting hosts
          The default rounting is done randomly and the blacklisting uses a TTL Cache"}
  tcp-driver.routing.policy
  (:require [schema.core :as s]
    [fun-utils.cache :as cache]
    [clojure.set :as clj-set]
    [tcp-driver.io.conn :as tcp-conn]))


;;;;;;;;;;;;;;;;;
;;;;; Protocols and records

(defprotocol IRoute
             (-hosts [this])                                ;;return the hosts {:host :port} currently managed by the routing policy
             (-add-host! [this host])                       ;;"Called from outside the driver to notify addition of a host"
             (-remove-host! [this host])                    ;;"Called from outside the driver to notify removal of a host"
             (-blacklisted? [this host])                    ;;"True if the node is blacklisted"
             (-blacklist! [this host])                      ;;"Node should be blacklisted, called from outside of the driver to notify blacklisting of a host"
             (-on-error! [this host throwable])             ;;"Communicate exceptions back to the routing policy, default is to blacklist the host"
             (-select-host [this]))                         ;; "Driver calls to select  host from the routing policy"


;;;;;;;;;;;;;
;;;; Private functions

(defn ensure-host-address-schema! [host]
      (s/validate tcp-conn/HostAddressSchema host))

;;hosts-at contains #{tcp-conn/HostAddressSchema}
;;black-listed-hosts-at a ttl cache that hosts-at but holds black-listed
;;select-f is a function

(defrecord DefaultRountingPolicy [hosts-at black-listed-hosts-cache select-f]
           IRoute

           (-hosts [this] @hosts-at)

           (-on-error! [this host throwable]
                       (-blacklist! this host))

           (-add-host! [_ host]
                       (ensure-host-address-schema! host)
                       (swap! hosts-at conj host))

           (-remove-host! [_ host]
                          (ensure-host-address-schema! host)
                          (swap! hosts-at disj host))

           (-blacklisted? [_ host]
                          (ensure-host-address-schema! host)
                          (get black-listed-hosts-cache host))

           (-blacklist! [_ host]
                        (ensure-host-address-schema! host)
                        (assoc black-listed-hosts-cache host true))

           (-select-host [_]
                         (let [available-hosts (into [] (clj-set/difference @hosts-at (set (keys black-listed-hosts-cache))))]
                              (when (pos? (count available-hosts))
                                    (rand-nth available-hosts)))))


;;;;;;;;;;;;;;;;;
;;;;; Public API

(defn create-default-routing-policy
      "Create a rounting policy instance that manages blacklisted hosts and on select-host
       hosts : HostAddressSchema
       returns the host that is not blacklisted, select-f is used for this, note that by default rand-nth is used"
      [hosts
       & {:keys [select-f
                 blacklist-expire] :or {select-f         rand-nth
                                        blacklist-expire 10000}}]
      {:pre [(s/validate [tcp-conn/HostAddressSchema] hosts)
             (fn? select-f)
             (number? blacklist-expire)]}

      (->DefaultRountingPolicy
        (atom (into #{} hosts))
        (cache/create-cache :expire-after-write blacklist-expire)
        select-f))
