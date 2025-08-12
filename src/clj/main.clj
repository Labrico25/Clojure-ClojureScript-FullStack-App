(ns main
  (:require [server :refer [start-server! stop-server!]])
  (:import server$stub)  
  (:gen-class))

(defn addshutdown []
  (let [^Runtime runtime (Runtime/getRuntime)]
    (.addShutdownHook runtime (Thread. ^Runnable (fn [] (stop-server!))))))


(defn -main []
  (addshutdown)  
  (start-server! server$stub)) 
