#IO Stream Support

The namespace ```tcp-driver.io.stream``` provides all the functions required to open/write/read  
to and from an ```InputStream``` or ```OutputStream```.
 
The java class ```tcpdriver.io.IOUtil``` implements the backend of the stream functions for efficiency.  

## Blocking IO

Note that for client communication blocking IO is the most convenient and also simplest to reason about,  
providing automatic back pressure. The only issue with blocking reads is that there are no timeouts implemented  
in the API, this library implements timeouts on blocking reads without any background threads.  

Have a look at the java class ```tcpdriver.io.IOUtil``` to see how its implemented using avialble and partial reads.   

## Timeouts

All stream read operations have a timeout in milliseconds argument, and will throw a ```TimeoutException``` if
the required bytes could not be read from the connection's ```InputStream``` in that time.



## Example

```clojure

(require '[tcp-driver.io.stream :as tcp-stream])

;;get a connection either directly or from a pool
;;write a short string
(tcp-stream/write-short-str conn "hi")

;;read a short string
(tcp-stream/read-short-str conn 1000)

```


