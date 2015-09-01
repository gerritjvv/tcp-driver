(ns
  ^{:doc "Test the TCP connection implementation"}
  tcp-driver.io.conn-test
  (:require
    [tcp-driver.io.stream :as tcp-stream]
    [tcp-driver.io.conn :as tcp-conn]
    [tcp-driver.test.util :as test-util]
    [clojure.test :refer :all]))

(defn echo-handler [conn]
  (tcp-stream/write-bytes conn (tcp-stream/read-bytes conn 5000)))

(defn echo-server []
  (test-util/create-server echo-handler))


(deftest test-send-receive []
  (let [server (echo-server)
        conn (tcp-conn/create-tcp-conn {:host "localhost" :port (:port server)})]

    (try
      (tcp-stream/write-short-str conn "test-string")
      (is (= "test-string" (tcp-stream/read-short-str conn 5000)))
      (finally
        (tcp-conn/close! conn)))))