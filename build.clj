(ns build  
  (:require [clojure.tools.build.api :as build]
            [next.jdbc :refer [get-connection]]))

(set! *warn-on-reflection* true)



(def class-dir "target/classes")



(def uber-file "target/TodoApp.jar")



(def ^java.security.KeyStore keystore (java.security.KeyStore/getInstance "JKS"))



(def ^String keystore-path "resources/public/store/mykeystore.jks")



(def ^chars keystore-password (.toCharArray "changeit"))



(def basis (delay (build/create-basis {:project "deps.edn"})))



(defn- make-keystore-dir [^java.io.File file then]
  (let [parentdir (.getParentFile file)]
    (when-not (.exists parentdir)
      (.mkdirs parentdir)  
      (.load keystore nil keystore-password)))  
  (then))



(defn- get-session-certs [^javax.net.ssl.SSLSocket socket push-certs]
  (let [^javax.net.ssl.SSLSession session (.getSession socket)]
    (push-certs (.getPeerCertificates session))))  

(defn- get-needed-certs-for-conn [^"[Ljavax.net.ssl.TrustManager;" managers]
  (let [^javax.net.ssl.SSLContext ctx (doto (javax.net.ssl.SSLContext/getInstance "TLS") 
                                        (.init nil managers nil))
        ^javax.net.ssl.SSLSocketFactory factory (.getSocketFactory ctx)]
    (fn [^String host ^Long port]
      (let [socket (doto ^javax.net.ssl.SSLSocket (.createSocket factory host port)
                     (.startHandshake))]
        (get-session-certs socket
                           (fn [^"[Ljava.security.cert.Certificate;" certs]
                             (doseq [cert (seq certs)]
                               (doto keystore
                                 (.setCertificateEntry (str (gensym)) cert)
                                 (.store (java.io.FileOutputStream. keystore-path) keystore-password)))))))))




(def persist-session-certs-for
  (get-needed-certs-for-conn (into-array [(proxy [javax.net.ssl.X509TrustManager] []
                                            (checkServerTrusted [^"[Ljava.security.cert.X509Certificate;" certs authtype])
                                            (getAcceptedIssuers []
                                              (make-array java.security.cert.X509Certificate 0)))])))




(def persist (delay (persist-session-certs-for "smtp.gmail.com" 465)))




(defn clean [paths]
  (doseq [path paths]
    (build/delete {:path path}))) 


  

(defn uber [_]  
  (clean ["target" "resources/public/store"])  
  (make-keystore-dir (java.io.File. "resources/public/store/mykeystore.jks")
                     (fn [] @persist))  
  (build/copy-dir {:src-dirs ["src/clj" "resources" "src/cljs"]
                   :target-dir class-dir})
  (build/compile-clj {:src-dirs ["src"]
                      :basis @basis
                      :class-dir class-dir})
  (build/uber {:class-dir class-dir
               :basis @basis
               :main "main"
               :uber-file uber-file}))