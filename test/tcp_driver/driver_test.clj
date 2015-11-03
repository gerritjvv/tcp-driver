(ns
  ^{:doc "Test the full driver send and receive namespace"}
  tcp-driver.driver-test
  (:require
    [schema.core :as s]
    [tcp-driver.test.util :as test-util]
    [tcp-driver.io.stream :as tcp-stream]
    [tcp-driver.io.conn :as tcp-conn]
    [tcp-driver.driver :as tcp-driver]
    [clojure.test :refer :all]))


(defn write-msg [conn msg]
  (tcp-stream/write-short-str conn (str msg))
  conn)

(defn read-msg [conn timeout-ms]
  (tcp-stream/read-short-str conn timeout-ms))

(defn send-io-f [io-f]
  (test-util/with-echo-server
    (fn [server]
      (let [driver (tcp-driver/create-default [{:host "localhost" :port (:port server)}])
            ret-msg (tcp-driver/send-f
                      driver
                      io-f
                      10000)]

        (= ret-msg "HI")))))


(deftest test-send-receive
  []
  ;;write hi, then read it
  (send-io-f #(read-msg (write-msg % "HI") 1000)))