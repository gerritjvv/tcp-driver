# Introduction to tcp-driver

The idea is the access TCP client connections like any other product driver code would e.g the cassandra or mondodb driver.<br/>

There are allot of situations where software in the past (my own experience) became unstable because the TCP connections
were not written or treated with the equivalent importance as server connections.

Writing the TCP connection as if it were a product driver sets a certain design mindset.

# Mindset and expectations:

## Making configurable and transparent:  

  * Single open close connections
  * Connection pooling and re-use.
  * Connection testing and reconnecting before use.
  * Reconnect and Retry on IO failure
  * Retry policy e.g default would be Back-off-retry policy
  * Automatic closing of open resources on application shutdown (subcribe to the Java Shutdown Hook)

## Multiple endpoints

  * Add/Remove enpoints dynamically.
  * Blacklisting enpoints (support blaclisting N milliseconds of a host where its still alive but not usable)
  * Support custom routing to select hosts based on custom logic, default should be random.

## IO Read timeout
  * support timeouts on blocking io reads.
  
## Throttling 
  
  * Support Rate limiting (fancy for some usecases but easy to do with the google guava lib).


## Async and multi threaded
 
  The code should support multi threading and async usage (not NIO). We can support multi threading by 
  allowing each thread to have its own connection from a Pool, thus not requiring locking or syncing if 
  the pool has enough connections. Synchronization is only required once the Pool runs out of free connections
  and threads need to compete for resources.

# What about NIO?

From my own experience in writing high volume performant client TCP IO code its my opinion that NIO doesn't add  
any value in terms of performance. It only adds complications. The reasons are:

  * client code requires back pressure, NIO is async and mostly adds connections to memmory to eventually
    give you an OOM.
  * error handling and feedback to client calling code is easier/cleaner and less bug prone via direct blocking IO.
  * for client code bocking IO can be as fast or even faster than NIO (when using poolable connections).


# Design


## Service Provider Interface (public api)

  conn = create [{custom-env conf routing rate-limiter retry-policy} bootstrap-end-points]
  send [ conn i-o-f data timeout-ms] : RESP  ;; the data is applied to the i-o-f
  send [ conn exec-service i-o-f data timeout-ms] : Promise[RESP]
  
  i-o-f [ conn data timeout-ms] : RESP

  stats = conn-stats [ conn ]
  
  close [ conn ]


### Routing
  IRouting
     select-host [ custom-env hosts ] : Host
     
## I/O Timeouts
  
  Support IO timeout see: https://github.com/gerritjvv/kafka-fast/blob/master/kafka-clj/java/kafka_clj/util/IOUtil.java#L37
  
  Provide support functions for common java primites (and short strings)  
  See: https://github.com/gerritjvv/kafka-fast/blob/master/kafka-clj/java/kafka_clj/util/IOUtil.java#L21  
  
  * read-bytes [reader bts-len timeout-ms] : byte-array
  * read-int [reader timeout-ms ] : int
  * read-long [reader timeout-ms ] : long
  * read-double [reader timeout-ms] : double
  * read-short [reader timeout-ms ] : short
  * read-float [reader timeout-ms ] : short
  * read-short-str [reader timeout-ms ] : String
  * read-bool [reader timeout-ms] : boolean
  
  * write-bytes [writer bts from len ]
  * write-int [writer int]
  * write-double ...
  .... for all primitives 
  * write-short-str [writer string]
  
  IReader
    -read-bytes [_ bts-len timeout-ms] : byte-array
    
  IWriter
    -write-bytes [ bts-len timeout-ms ]
  
# Implementation

 create-conn [ bootstrap-hosts pool-conf routing-obj retry-policy conf]
   { :pool (pool
     :conf conf
     :bootstrap-hosts bootstrap-hosts
     :retry-policy retry-policy
     :routing-policy routing-obj
     :routing-env {:hosts (atom (set boostrap-hosts))}
     
     
                
 add-host! [ ctx host host-conf]
    (assoc-stm! (get-in ctx [:routing-env :hosts]) host)
    
 remove-host! [ctx host host-conf]
   (dissoc-stm! (get-in ctx [:routing-env :hosts]) host)
       
 send-op [ctx host i-o-f data timeout-ms]
    try:
       conn = (pool/borrow (:pool ctx) host timeout-ms)
       try:
          return (i-o-f conn data timeout-ms)
       finally:
          (pool/return pool-inst conn)
                                 
     catch Exception e
           (pool/invalidate pool-inst conn)
           return e
       

 send [ ctx i-o-f data timeout-ms]
 
   hosts = (:bootstrap-hosts ctx)
   routing-policy (:routing-policy ctx)
   
   
   loop [retry-state  nil]
        host = (routing/select-host routing-policy (:routing-env ctx) hosts)
              
        if host == null
           throw NoHostAvailableException
           
        ret-val =  (send-op ctx host i-o-f data timeout-ms)
        
        if ret-val == Exception
           [retry-state retry-op] = (io-retry/retry-action! (:retry-policy ctx) (:conf ctx) conn host retry-state)
           if retry-op == :recur
             (recur retry-state)
           else
              (throw ret-val)
        else
           ret-val
           
    NS: rounting
      Protocol IRounting
        select-host 
        add-host
        remove-host
        
    NS: io-retry
    multi-method retry-action! identity
    
    defmulti retry-action! :retry           [_ _ _ _ retry-state] 
                                                [retry-state (if (available-tries? retry-state) :retry nil)]
                                                
    defmulti retry-action! :blacklist-retry [ctx conn host e retry-state]
                                                (routing/blacklist! (:routing-policy ctx) (:routing-env ctx) conn host)
                                                [retry-state (if (hosts-left? ... ) :retry :nil)]
                                                
    defmulti retry-action! :default [ _ _ _ e _ ] nil
    
                                                
                                             
           
      

Even though the implementation language is Clojure the public api should give first class support for:  
  
  * Java 1.8
  * Clojure
  
