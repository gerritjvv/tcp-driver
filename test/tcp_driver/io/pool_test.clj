(ns
  ^{:doc "Test the pool namespace"}
  tcp-driver.io.pool-test
  (:require
    [tcp-driver.io.pool :as tcp-pool]
    [tcp-driver.io.conn :as tcp-conn]
    [tcp-driver.test.util :as test-util]
    [clojure.test :refer :all]
    [tcp-driver.io.stream :as tcp-stream]))



(defn borrow-send-and-return []
  (test-util/with-echo-server
    (fn [server]
      (let [pool (tcp-pool/create-tcp-pool {:close-pool-jvm-shutdown true})
            host (tcp-conn/host-address "localhost" (:port server))

            resp-str (tcp-pool/try-conn pool host 1000
                                        (fn [conn]
                                          (tcp-stream/write-short-str conn "hi")
                                          (tcp-stream/read-short-str conn 20000)))]
        (is (= resp-str "hi"))))))

(deftest send-receive-testcase []
                               (borrow-send-and-return))