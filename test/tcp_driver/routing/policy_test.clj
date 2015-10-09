(ns
  ^{:doc "Test the tcp-driver.routing.policy default implementation"}
  tcp-driver.routing.policy-test
  (:require [tcp-driver.routing.policy :as routing]
            [tcp-driver.io.conn :as tcp-conn])
  (:use clojure.test))



(defn create-routing [host-names]
  (routing/create-default-routing-policy (mapv #(tcp-conn/host-address % 123) host-names)))

(deftest select-test []
                (is (routing/-select-host (create-routing ["a" "b"]))))

(deftest select-remove-add-test []
                                (let [policy (create-routing ["a" "b"])]
                                  (routing/-remove-host! policy (tcp-conn/host-address "a" 123))
                                  (routing/-remove-host! policy (tcp-conn/host-address "b" 123))

                                  (is (nil? (routing/-select-host policy)))

                                  (routing/-add-host! policy (tcp-conn/host-address "c" 123))

                                  (is (= (routing/-select-host policy) (tcp-conn/host-address "c" 123)))))



(deftest blacklist-test []
                   (let [policy (routing/create-default-routing-policy [(tcp-conn/host-address "a" 123)] :blacklist-expire 500)]

                     (is (routing/-select-host policy))

                     (routing/-blacklist! policy (tcp-conn/host-address "a" 123))

                     (is (nil? (routing/-select-host policy)))

                     (Thread/sleep 1000)

                     (is (routing/-select-host policy))))