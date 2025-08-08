(ns mail-helper
  (:require [clojure.java.io :refer [resource]])  
  (:import (javax.net.ssl SSLContext SSLSocketFactory SSLSocket TrustManagerFactory)
           (java.security KeyStore)
           (java.net Socket)
           (java.util Scanner Base64)
           (java.io PrintWriter)))

(set! *warn-on-reflection* true)

(defn scan [^SSLSocket in]
  (Scanner.
    (.getInputStream in)))

(defn read-in [^Scanner scanned]
  (when (.hasNextLine scanned)
    (recur scanned)))  

(defn ->base64 [^String string]
  (let [encoder (Base64/getEncoder)]
    (.encodeToString encoder (.getBytes string))))

(defn connect [^String host ^Integer port]
  (let [socket (Socket. host port)
        ^KeyStore keystore (doto (KeyStore/getInstance "JKS")
                             (.load (.openStream (resource "public/store/mykeystore.jks")) (.toCharArray "changeit")))
        ^TrustManagerFactory trustManagerFactory (doto (TrustManagerFactory/getInstance "SunX509")
                                                   (.init keystore)) 
        ^SSLContext ctx (doto (SSLContext/getInstance "TLS")
                          (.init nil (.getTrustManagers trustManagerFactory) nil))
        ^SSLSocketFactory factory (.getSocketFactory ctx)
        ^SSLSocket upgraded (.createSocket factory
                                           socket
                                           (.. socket getInetAddress getHostAddress)
                                           (.getPort socket)
                                           true)
        in (scan upgraded)
        out (PrintWriter. (.getOutputStream upgraded) true)]
    (.startHandshake upgraded)  
    [socket in out]))

(defn send-mail [in ^PrintWriter out communication]
  (doseq [^String comm communication]
    (. out (println comm) flush)
    (doto (Thread. (fn [] (read-in in)))
      (.setUncaughtExceptionHandler (proxy [Thread$UncaughtExceptionHandler] []
                                      (uncaughtException [e t])))
      (.start))))  

(defn chat [host port & communication]
  (let [[socket in out] (connect host port)]
    (send-mail in out communication)
    (Thread/sleep 40000)  
    (.close ^Socket socket)))  