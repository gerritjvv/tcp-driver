(ns
  ^{:doc "Protocol interface and default implementation for routing and blacklisting hosts
          The defualt rounting is done randomly and the blacklisting uses a TTL Cache"}
  tcp-driver.routing.policy
  (:require [schema.core :as s]
            [fun-utils.cache :as cache]
            [clojure.set :as clj-set]
            [tcp-driver.io.conn :as tcp-conn])
  (:import (clojure.lang Atom)))


;;;;;;;;;;;;;;;;;
;;;;; Protocols and records

(defprotocol IRoute
  (-add-host! [this host])
  (-remove-host! [this host])
  (-blacklist! [this host])
  (-select-host [this]))


;;;;;;;;;;;;;
;;;; Private functions

(defn ensure-host-address-schema! [host]
  (s/validate tcp-conn/HostAddressSchema host))

;;hosts-at contains #{tcp-conn/HostAddressSchema}
;;black-listed-hosts-at a ttl cache that hosts-at but holds black-listed
;;select-f is a function

(defrecord DefaultRountingPolicy [hosts-at black-listed-hosts-cache select-f]
  IRoute
  (-add-host! [_ host]
    (ensure-host-address-schema! host)
    (swap! hosts-at conj host))

  (-remove-host! [_ host]
    (ensure-host-address-schema! host)
    (swap! hosts-at disj host))

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
   returns the host that is not blacklisted, select-f is used for this, note that by default rand-nth is used"
  [hosts
   & {:keys [select-f
             blacklist-expire] :or {select-f         rand-nth
                                    blacklist-expire 10000}}]
  {:pre [(s/validate [tcp-conn/HostAddressSchema] hosts)
         (fn? select-f)
         (number? blacklist-expire)]}

  (prn "Create with blacklist-expire " blacklist-expire)
  (->DefaultRountingPolicy
    (atom (into #{} hosts))
    (cache/create-cache :expire-after-write blacklist-expire)
    select-f))