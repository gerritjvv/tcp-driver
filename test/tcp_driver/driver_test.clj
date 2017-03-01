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

(defn send-io-f [io-f & {:keys [post-create-fn pre-destroy-fn]}]
      (test-util/with-echo-server
        (fn [server]
            (let [driver (tcp-driver/create-default [{:host "localhost" :port (:port server)}] :pool-conf {:post-create-fn post-create-fn :pre-destroy-fn pre-destroy-fn})
                  ret-msg (tcp-driver/send-f
                            driver
                            io-f
                            10000)]
                 ;;use ret-msg to make it clear thta io-f return value via tcp-driver/send-f
                 (tcp-driver/close driver)
                 ret-msg))))


(deftest test-send-receive
         []
         ;;write hi, then read it
         (is (= (send-io-f #(read-msg (write-msg % "HI") 1000))
                "HI")))

(deftest test-validate-fns
         []
         (let [post-create (atom nil)]

              (is (= (send-io-f #(read-msg (write-msg % "HI") 1000)
                                :post-create-fn (fn [ctx]
                                                    (swap! post-create (constantly (System/nanoTime)))
                                                    (:conn ctx)))
                     "HI"))


              (is (number? @post-create))))