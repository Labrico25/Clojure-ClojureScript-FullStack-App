(ns test-app  
  (:require [clojure.test :refer :all]
            [server :refer [start-server! stop-server! users]]
            [clojure.java.io :refer [delete-file]]
            [clj-http.client :as http]))


(defmacro log [& body]
  (list* 'do (into [] (map (fn [x] `(println ~x))) body)))


(defprotocol helper-prot
  (getProtectionDomain [this])
  (getCodeSource [this])
  (getLocation [this])
  (getPath [this]))


(def test-helper
  (reify Object
    helper-prot
    (getProtectionDomain [this] this)
    (getCodeSource [this] this)
    (getLocation [this] this)
    (getPath [this]
      (let [dir (str (System/getProperty "user.dir") "\\")]
        (str "/" dir)))))  


(start-server! test-helper)


(deftest todo-app-test
  (log (is (= (:status (http/get "http://localhost:8080" {})) 200))
       (is (= 200 (:status (http/get "http://localhost:8080/login" {}))))
       (is (= 200 (:status (http/get "http://localhost:8080/users/register" {}))))
       (let [resp (http/post "http://localhost:8080/save" {:form-params {:username "foo" :password "bar"}})]
         (is (and (= (:status resp) 200) (= (get-in @users ["foo" :password]) "bar"))))
       (let [resp (http/post "http://localhost:8080/verify" {:form-params {:username "foo" :password "bar"}})]
         (is (not= (http/get "http://localhost:8080") 
                   (http/get "http://localhost:8080" {:cookies (get resp :cookies)}))))
       (let [resp (http/get "http://localhost:8080/unsubscribe?confirmed=:Confirmed" {:cookies {"username" {:value "foo"} 
                                                                                                "password" {:value "bar"}
                                                                                                "Signed" {:value "true"}}})]
         (is (empty? @users)))))

(defn run-it [_]
  (let [_ (todo-app-test)]  
    (stop-server!)
    (Thread/sleep 20000)  
    (delete-file "./local-db")))  

