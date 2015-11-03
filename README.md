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
        ;;use ret-msg to make it clear that io-f return value via tcp-driver/send-f
        ret-msg))))

;;write hi message and read the response from the echo server
(=  (send-io-f 
      #(read-msg 
        (write-msg % "HI") 1000))
     "HI")
```

## IO Stream Util functions

The namespace ```tcp-driver.io.stream``` provides helper functions to work with  
writing an reading data from the tcp connections returned by the driver

See: https://github.com/gerritjvv/tcp-driver/blob/master/doc/stream.md

## Driver Configuration

The ```tcp-driver.driver/create-default``` function takes several args   
```[hosts & {:keys [routing-conf pool-conf retry-limit] :or {retry-limit 10 routing-conf {} pool-conf {}}}]```

### Connection Pooling


The pool-conf arg must have the schema ```tcp-driver.io.pool/PoolConfSchema``` and can be used to 
configure the default tcp pool.

See: https://github.com/gerritjvv/tcp-driver/blob/master/doc/pool.md

### Routing Policy

The routing-conf arg must have the schema ```{:keys [select-f blacklist-expire]}```

The default select-f used is rand-nth.  
The function will receive a list of host map items and can select between any of them.  



### Retry Policy

The default retry policy is used in the ```tcp-driver.driver/create-default``` function,  
to configure a custom retry policy use the ```tcp-driver.driver/create``` function.  

See: https://github.com/gerritjvv/tcp-driver/blob/master/doc/retry.md


## License

Copyright © 2015 gerritjvv

Distributed under the Eclipse Public License either version 1.0
