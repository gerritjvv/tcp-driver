(ns
  ^{:doc "Helper utilities with Input/Output Streams and common tasks like reading/writing java primitives
          using blockign IO but supporting timeouts without the need to create background threads"}
  tcp-driver.io.stream
  (:require [tcp-driver.io.conn :as tcp-conn])
  (:import (tcpdriver.io IOUtil)
           (java.io OutputStream)))


;;;;;;;;;;;;;;;;;;;;;;
;;;;; Public API

(defn available ^long [conn]
  (long (.available (tcp-conn/input-stream conn))))

(defn read-bytes ^"[B"
  ([conn ^long len ^long timeout-ms]
    (IOUtil/readBytes (tcp-conn/input-stream conn) len timeout-ms))
  ([conn ^long timeout-ms]
   (read-bytes conn (available conn) timeout-ms)))

(defn read-byte ^long
[ conn ^long timeout-ms]
  (long (IOUtil/readByte (tcp-conn/input-stream conn) timeout-ms)))

(defn read-int ^long
  [ conn ^long timeout-ms]
  (long (IOUtil/readInt (tcp-conn/input-stream conn) timeout-ms)))

(defn read-long ^long
  [ conn ^long timeout-ms]
  (IOUtil/readLong (tcp-conn/input-stream conn) timeout-ms))

(defn read-short ^long
  [ conn ^long timeout-ms]
  (long (IOUtil/readShort (tcp-conn/input-stream conn) timeout-ms)))

(defn read-float ^double
[ conn ^long timeout-ms]
  (double (IOUtil/readFloat (tcp-conn/input-stream conn) timeout-ms)))

(defn read-double ^double
[ conn ^long timeout-ms]
  (IOUtil/readDouble (tcp-conn/input-stream conn) timeout-ms))

(defn read-short-str ^String
[ conn ^long timeout-ms]
  (IOUtil/readShortString (tcp-conn/input-stream conn) timeout-ms))

(defn flush-out [conn]
  (.flush ^OutputStream (tcp-conn/output-stream conn)))

(defn write-bytes
  ([conn ^"[B" bts ^long from ^long len]
   (IOUtil/write (tcp-conn/output-stream conn) bts from len))
  ([ conn ^"[B" bts]
   (IOUtil/write (tcp-conn/output-stream conn) bts)))

(defn write-byte
  [conn ^long v]
  (IOUtil/write (tcp-conn/output-stream conn) (byte v)))

(defn write-int
  [conn ^long v]
  (IOUtil/write (tcp-conn/output-stream conn) (int v)))

(defn write-short
  [ conn ^long v]
  (IOUtil/write (tcp-conn/output-stream conn) (short v)))

(defn write-long
  [ conn ^long v]
  (IOUtil/write (tcp-conn/output-stream conn) v))

(defn write-float
  [ conn ^double v]
  (IOUtil/write (tcp-conn/output-stream conn) (float v)))

(defn write-double
  [ conn ^double v]
  (IOUtil/write (tcp-conn/output-stream conn) v))

(defn write-short-str
  [ conn ^String v]
  (IOUtil/writeShortString (tcp-conn/output-stream conn) v))

