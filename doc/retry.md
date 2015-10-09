# Connection Retry Policy 

## Overview

A IO Function can at any point throw an exception, the retry policy determines how  
on Exception should the function be called, if any or if the exception should be  
re thrown

## Default Retry Policy

A ```tcp-driver.routing.retry.DefaultRetryPolicy``` record is provided that implements  
```tcp-driver.routing.retry.IRetry``` protocol,  and calls the IO Functions if  
and exception is thrown, doing so N times, where N is provided as part of the creation  
of DefaultRetryPolicy.


## Usage

```clojure
(require '[tcp-driver.routing.retry :as retry])
 
(let [rpolicy (retry/retry-policy 3)
                          f (fn [] (prn "try-function and throw ex") (throw (Exception. "test")))]
                      (try
                        (retry/with-retry rpolicy f)
                        (catch Exception e
                          (prn (= (get (ex-data e) :retries) 3)))))

```