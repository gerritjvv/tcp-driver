# tcp-driver

The idea is the access TCP client connections like any other product driver code would e.g the cassandra or mondodb driver.<br/>

There are allot of situations where software in the past (my own experience) became unstable because the TCP connections
were not written or treated with the equivalent importance as server connections.

Writing the TCP connection as if it were a product driver sets a certain design mindset.

For design decisions see: https://github.com/gerritjvv/tcp-driver/blob/master/doc/intro.md

[![Build Status](https://travis-ci.org/gerritjvv/tcp-driver.svg)](https://travis-ci.org/gerritjvv/tcp-driver)

## Usage

```clojure
(require '[tcp-driver.test.util :as test-util])
(require '[tcp-driver.io.stream :as tcp-stream])
(require '[tcp-driver.io.conn :as tcp-conn])
(require '[tcp-driver.driver :as tcp-driver])
(require '[clojure.test :refer :all])
    
;;write a short string to the connection 
(defn write-msg [conn msg]
  (tcp-stream/write-short-str conn (str msg))
  conn)

;;function that reads a short string
(defn read-msg [conn timeout-ms]
  (tcp-stream/read-short-str conn timeout-ms))

;;create a tcp driver using the default retries etc and 
;;send the passing io-f function to the driver
(defn send-io-f [io-f]
  (test-util/with-echo-server
    (fn [server]
      (let [driver (tcp-driver/create-default [{:host "localhost" :port (:port server)}])
            ret-msg (tcp-driver/send-f
                      driver
                      io-f
                      10000)]
        ;;use ret-msg to make it clear thta io-f return value via tcp-driver/send-f
        ret-msg))))

;;write hi message and read the response from the echo server
(=  (send-io-f 
      #(read-msg 
        (write-msg % "HI") 1000))
     "HI")
```

### Connection Pooling



## License

Copyright © 2015 gerritjvv

Distributed under the Eclipse Public License either version 1.0
