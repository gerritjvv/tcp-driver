(ns tcp-driver.test.util
  (:require [tcp-driver.io.conn :as tcp-conn])
  (:import (java.net ServerSocket SocketException)))

(defn ^ServerSocket server-socket []
  (ServerSocket. (int 0)))

(defn ^Long get-port [^ServerSocket socket]
  (Long. (.getLocalPort socket)))

(defn get-connection! [^ServerSocket socket]
  (.accept socket))

;;;;;;;;;;;;;;;;;;;;
;;;;;;;;; Public API

(defn create-server
  "Connects a server socket to an abritrary free port
  Params:
    handler-f called (handler-f  ^ITCPConn conn)
  Returns:
  {:server-socket ...
   :port the binded port
   :future an instance of (future that accepts and calls handler-f}"
  [handler-f]
  (let [socket (server-socket)]
    {
     :server-socket socket
     :port          (get-port socket)
     :future-loop   (future
                      (while (not (Thread/interrupted))
                        (try
                          (handler-f (tcp-conn/wrap-tcp-conn (get-connection! socket)))
                          (catch InterruptedException _ nil)
                          (catch SocketException _ nil)
                          (catch Exception e (do (prn e) (.printStackTrace e))))))}))

(defn stop-server [{:keys [server-socket future-loop]}]
  (.close ^ServerSocket server-socket)
  (future-cancel future-loop))