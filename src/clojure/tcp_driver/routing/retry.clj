(ns
  ^{:doc "Retry interfaces and default counted retry protocol"}
  tcp-driver.routing.retry
  (:require [schema.core :as s]))


;;;;;;;;;;;;;;;;;;;
;;;;; Protocols and records

(defprotocol IRetry
  (-try-action [this f] "Retry f according to the policy, a failure is marked by an exception"))


;;;;;;;;;;;;;;;;;;
;;;;;; Private functions

(defn _try-catch "run f if exception returns the exception" [f]
  (try (f) (catch Throwable e e)))

(defn _retry-ntimes
  "Retry the function call limit times only if an exception is thrown"
  [f limit]
  (loop [n 1]
    (let [ret (_try-catch f)]
      (if (instance? Throwable ret)
        (if (< n (long limit))
          (recur (inc n))
          (throw (ex-info "Retry-exception" {:throwable ret :retries n})))
        ret))))

(defrecord DefaultRetryPolicy [^long retry-limit]
  IRetry
  (-try-action [_ f]
    (_retry-ntimes f retry-limit)))



;;;;;;;;;;;;;;;;;
;;;;;; Public Functions

(s/defn with-retry [retry-policy :- (s/pred #(satisfies? IRetry %))
                    f :- (s/pred fn?)]
  (-try-action retry-policy f))

(defn retry-policy [retry-limit]
  {:pre [(number? retry-limit)]}
  (->DefaultRetryPolicy retry-limit))