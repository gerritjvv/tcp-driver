# Connection pooling

The namespace ```tcp-driver.io.pool``` provides all the functions required to create a keyed  
connection pool. Each key is a host address of type ```{:host <host> :port <port>}``` and  
creates a sub pool of TCP connections.   

The default ```IPool``` implementation uses the apache commons object pool,  
see https://commons.apache.org/proper/commons-pool/  

## Usage

```clojure

(require '[tcp-driver.io.pool :as tcp-pool])
(require '[tcp-driver.io.conn :as tcp-conn])

(def pool (tcp-pool/create-tcp-pool {}))
(def host (tcp-conn/host-address "localhost" 8001))
(def timeout-ms 1000)


;;manual borrow and return
(let [conn (tcp-pool/borrow pool host timeout-ms)]
       ;;do something with conn
     (tcp-pool/return pool host))

;; or convenience function

(tcp-pool/try-conn pool host timeout-ms 
  (fn [conn] 
     ;;do something with conn
     ))
```

