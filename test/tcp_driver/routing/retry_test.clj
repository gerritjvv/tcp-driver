(ns tcp-driver.routing.retry-test
  (:require [tcp-driver.routing.retry :as retry])
  (:use clojure.test))


(deftest test-retry []
                    (let [rpolicy (retry/retry-policy 3)
                          f (fn [] (prn "try-function and throw ex") (throw (Exception. "test")))]

                      (try
                        (retry/with-retry rpolicy f)
                        (is false)                          ;should not be run
                        (catch Exception e
                          (is (= (get (ex-data e) :retries) 3))))))