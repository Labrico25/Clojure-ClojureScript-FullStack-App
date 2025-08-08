(ns main
  (:require [index :refer [start-server! stop-server!]])
  (:import index$stub)  
  (:gen-class))

(defn addshutdown []
  (let [^Runtime runtime (Runtime/getRuntime)]
    (.addShutdownHook runtime (Thread. ^Runnable (fn [] (stop-server!))))))


(defn -main []
  (addshutdown)  
  (start-server! index$stub)) 