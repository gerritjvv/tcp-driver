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

### Pool Configuration

```clojure (tcp-pool/create-tcp-pool conf)```

Conf values are:

<table>
 <tr><td>Key</td><td>Value/Description</td></tr>
 <tr><td>:max-idle-per-key</td><td>Max connections idle for a pool to a particular server, default is 2</td></tr>
 <tr><td>:min-idle-per-key</td><td>Minimum connections idle for a pool to a particular server, default is 0</td></tr>
 <tr><td>:max-total</td><td>The maximum number of connections to open for all servers, default is 100</td></tr>
 <tr><td>:max-total-per-key</td><td>Same as :max-total but per key, default is 100</td></tr>
 <tr><td>:min-idle-per-key</td><td>Maximum idle connections per key, default is 0</td></tr>
 <tr><td>:close-pool-jvm-shutdown</td><td>If set to true, the pool will be closed for all keys and connections on JVM shutdown</td></tr>
</table>
