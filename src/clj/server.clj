(ns index
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [hiccup2.core :as h2]
            [hiccup.core :refer [html]]
            [clj-http.client :as client]
            [clojure.java.io :as jio]
            [mail-helper :refer [chat ->base64]]
            [next.jdbc :refer [get-connection execute!]]))


(defonce server (atom nil))


(defonce users (ref {}))


(def ^:redef jar)


(gen-class
   :name index$stub)

(defn jarloc [this]
  (alter-var-root #'jar (fn [_]  
                          (.. this (getProtectionDomain) 
                                   (getCodeSource) 
                                   (getLocation) 
                                   (getPath)))))


(def db 
  (delay 
    (get-connection {:jdbcUrl (format "jdbc:sqlite:file:%s" (str (subs (.replaceAll jar "/\\w+.jar" "/") 1) "local-db"))}))) 



(defn get-env
  ([] (System/getenv))
  ([variable] (System/getenv variable)))



(defmacro new-thread [& body]
  `(eval (let [thread# (Thread. (fn [] ~@body))]
           (.setUncaughtExceptionHandler thread# (proxy [Thread$UncaughtExceptionHandler] [] (uncaughtException [e# t#])))  
           (.start thread#)
           thread#)))



(defmacro thread-starter [simple-format time-formatter deadline-inst email lst]
  `(eval
     (new-thread
       (try  
         (loop []  
           (let [date# (java.util.Date.)
                 formatted-date# (.format ~simple-format date#)
                 date-inst# (java.time.LocalDateTime/parse formatted-date# ~time-formatter)]
             (cond
               (.isInterrupted (Thread/currentThread)) (throw (InterruptedException.))  
               (.isBefore ~deadline-inst date-inst#)                                                                   
               (chat "smtp.gmail.com"
                     465
                     "HELO [192.0.2.1]"
                     "AUTH LOGIN"
                     (->base64 (get-env "MY_USERNAME"))
                     (->base64 (get-env "MY_APPPASSWORD"))
                     "MAIL FROM: <todoapp@gmail.com>"
                     (format "RCPT TO: <%s>" ~email)
                     "DATA"
                     "From: \"Todo App\""
                     (format "To: \"You\" <%s>" ~email)
                     "Subject: Deadline"
                     ""
                     (format "%s" (:data ~lst))
                     "."
                     "QUIT")
               :else
               (recur))))
         (catch InterruptedException _#
           nil)))))


(defn remove-alert [{user :user id :id}]
  (dosync  
   (alter users
          (fn [usrs]
            (update-in usrs [(.replace user ";" "") :data]
                       (fn [alerts]
                         (into []
                               (keep-indexed (fn [idx alert]
                                               (if (= idx (read-string id))
                                                 (do (.interrupt alert) nil)
                                                 alert)))
                               alerts)))))))


(defn build-replace [n-list]
  (transduce (comp (map (fn [[k v]] (format "%s = \"%s\"" (name k) (.replaceAll (str v) "\"" "'"))))
                   (interpose ", "))
             str
             n-list))



(defn replace-alert [{id :id username :user n-list :n-list}]  
  (dosync  
   (alter users
          (fn [usrs]
            (update-in usrs [(.replace username ";" "") :data]
                       (fn [alerts]
                         (into []
                               (map-indexed (fn [idx alert]
                                              (if (= idx (read-string id))
                                                (do (.interrupt alert)
                                                  (let [data (read-string n-list)
                                                        email (:email data)
                                                        pattern "EEE MMM dd y HH:mm:ss zzzzZ (zzzz)"
                                                        simple-format (java.text.SimpleDateFormat. pattern)
                                                        time-formatter (java.time.format.DateTimeFormatter/ofPattern pattern)
                                                        deadline (:deadline data)
                                                        deadline-inst (when-not (or (empty? deadline) (= deadline "\"") (= deadline "\\")) 
                                                                        (java.time.LocalDateTime/parse deadline time-formatter))]
                                                    (if deadline-inst  
                                                      (thread-starter simple-format time-formatter deadline-inst email data)
                                                      (Thread.))))
                                                alert)))
                                alerts)))))))



(defn retrieve-from-db [user]
  (execute! @db [(format "SELECT * FROM %s" user)]))



(defn insert-to-db! [user data]  
  (run! #(execute! @db [(let [s (format "INSERT OR REPLACE INTO %s %s VALUES %s"
                                        user
                                        (into () (comp (map symbol) (interpose (symbol ","))) (keys %))
                                        (into () (comp (map (fn [x] (if (symbol? x) (format "%s" x) x))) (interpose (symbol ","))) (vals %)))]
                          (.replaceAll s "\\\\\"" "\"\""))])
        data))                                            



(defn home []
  (html
   [:body {:style {:background-image "url(https://i.imgur.com/fS6QDfn.png)"
                   :background-repeat "no-repeat"
                   :background-size "900px 500px"
                   :background-position "300px 100px"}}
    [:script "let link = document.createElement('link');
                 let title = document.createElement('title');
                 link.rel = 'icon';
                 link.href = 'https://cdn.pixabay.com/photo/2017/06/10/07/18/list-2389219_1280.png';
                 link.type = 'image/png';
                 document.head.appendChild(link);
                 title.textContent = 'Todo App';
                 document.head.appendChild(title);"]
    [:div {:id "MAIN"}
     '([:div {:style {:position "absolute"
                      :transform "translate(300px,50px)"}}
      [:p {:style {:font-size "80px"}} "Welcome to the todo website!"]]
      [:div {:id "login-box"}]
       [:div {:style {:position "absolute"
                      :transform "translate(500px,370px)"
                      :height "30px"
                      :width "500px"
                      :background-color "white"
                      :border "1px solid grey"}}
        [:button {:style {:position "absolute"
                          :transform "translate(230px,5.5px)"
                          :background "none"
                          :border "none"
                          :cursor "pointer"}
                  :onclick "(function () {
                          window.location.href = './login'})()"} "Login"]])]]))



(defn redirect [times url]
  (str  
   (h2/html
    [:meta {:http-equiv "refresh"
            :content (format "%s; url=%s" times url)}])))



(defn set-cookies [request] 
  {:status 200
   :body (redirect 1 (get-in request [:headers "referer"]))
   :headers {"Content-Type" "text/html; charset=UTF-8"}})



(defn mult-replace [string replacements]
  (format "%s"  
  (reduce-kv (fn [string reg rep]
               (.replaceAll string reg rep))
             string
             replacements)))



(def create-template
  "CREATE TABLE IF NOT EXISTS %s (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            list TEXT NOT NULL,
            created TEXT NOT NULL,
            last_modified TEXT NOT NULL,
            deadline TEXT NOT NULL,
            data TEXT NOT NULL,
            email TEXT NOT NULL)")



(defn your-page [req]
  (if (= (get-in req [:cookies "Signed" :value]) "true")
    (let [username (get-in req [:cookies "username" :value])] 
      (execute! @db [(format create-template username)])  
      {:status 200
       :body (str
              (html
                [:div {:id "my-page-wrapper"}
                 [:script {:src "https://unpkg.com/react@18/umd/react.production.min.js" :crossorigin true}]
                 [:script {:src "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" :crossorigin true}]
                 [:script (slurp (jio/resource "public/js/helpers.js"))]
                 [:script {:src "public/js/yourpage.js" :defer true}]]))
     :headers {"Content-Type" "text/html; charset=UTF-8"}
     :cookies {"username" {:value username :path "/" :max-age (* 120 20)} 
               "data" {:value
                       (mult-replace (str (retrieve-from-db username)) 
                                     {"#:.*?(\\{.*?)" "$1"
                                      "(:\\w+/)" ":"
                                      "'" "\\\\\""
                                      " (:\\w+)_(\\w+) " " $1-$2 " 
                                      "\"\"" "\\\\\""
                                      "\"(#inst) \\\\(.*?)\\\\\\\\" "$1 $2"})
                       :path "/" :max-age (* 120 20)}}})
    {:status 200
     :body (str (home))
     :headers {"Content-Type" "text/html; charset=UTF-8"}}))



(defn verify-creds [{:keys [username password] :as params} base-url]
  (if (= (get-in @users [username :password]) password) 
    {:status 200
     :body (redirect 0 base-url)
     :cookies {"username" {:value username :path "/" :max-age (* 120 60)}
               "password" {:value password :path "/" :max-age 20}
               "Signed" {:value true :path "/" :max-age (* 120 60)}}
     :headers {"Content-Type" "text/html; charset=UTF-8"}}     
    {:status 504}))  



(def page-cookie (wrap-cookies your-page))



(defn read-req-params [handler]
  (fn [req]
    (let [{params :params} req]
      (handler (assoc req
                      :params 
                      (into {} (map (fn [[k v]] [k (read-string v)])) params))))))



(def replacements {" (:\\w+)-(\\w+) " " $1_$2 " "(#inst) \"(.*?)\"," "\"$1 \\\\\"$2\\\\\"\","})



(compojure/defroutes routes
                     
  (compojure/ANY "/" req (fn [req] (page-cookie req)))
                     
  (compojure/GET "/login" req (if (= (get-in req [:cookies "Signed" :value]) "true")
                                       {:status 200
                                        :body "<script>window.location.href = window.location.origin</script>"
                                        :headers {"Content-Type" "text/html; charset=UTF-8"}}
                                       {:status 200
                                        :body (format "<div id=\"MAIN\">
                                                         <script src=\"https://unpkg.com/react@18/umd/react.production.min.js\" crossorigin></script>
                                                         <script src=\"https://unpkg.com/react-dom@18/umd/react-dom.production.min.js\" crossorigin></script>
                                                         <script>%s</script>
                                                         <script src=\"/public/js/login.js\" defer></script>
                                                       </div>"
                                                      (slurp (jio/resource "public/js/helpers.js")))
                                        :headers {"Content-Type" "text/html; charset=UTF-8"}}))
                     
  (compojure/GET "/favicon.ico" [] {:status 200})
                     
                     
  (compojure/GET "/public/js/yourpage.js" [] {:status 200
                                              :body (slurp (jio/resource "public/js/yourpage.js"))
                                              :headers {"Content-Type" "text/javascript"}})
                     
  (compojure/GET "/public/js/login.js" [] {:status 200
                                                 :body (slurp (jio/resource "public/js/login.js"))
                                                 :headers {"Content-Type" "text/javascript"}})
                     
                     
  (compojure/GET "/public/js/registration.js" [] {:status 200
                                                  :body (slurp (jio/resource "public/js/registration.js"))
                                                  :headers {"Content-Type" "text/javascript"}})
                     
  (compojure/GET "/users/register" req {:status 200
                                       :body (str
                                               (html (list [:div {:id "registration-element"}]
                                                            [:script {:src "https://unpkg.com/react@18/umd/react.production.min.js" :crossorigin true}]
                                                            [:script {:src "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" :crossorigin true}]
                                                              [:script (slurp (jio/resource "public/js/helpers.js"))]
                                                              [:script 
                                                                {:src (format 
                                                                       (str (str (name (:scheme req)) "://" (:server-name req) \: (:server-port req) \/) "%s")
                                                                       "public/js/registration.js")}])))
                                       :headers {"Content-Type" "text/html; charset=UTF-8"}})
                     
  (compojure/POST "/save" {params :params} (let [{:keys [username password]} params
                                                 current-users @users]
                                             (if (get current-users username)
                                               {:status 500 :headers {}}
                                               (do
                                                 (execute! @db [(format "INSERT OR REPLACE INTO users (username, password) VALUES %s"
                                                                    (let [s (str (list username password))]
                                                                      (.replaceAll s " " ",")))])                       
                                                 (dosync (alter users assoc username {:password password}))  
                                                 {:status 200 :headers {}}))))
                     
  (compojure/GET "/unsubscribe" []  (read-req-params
                                     (fn [req]   
                                       (if (= (get-in req [:params :confirmed]) :Confirmed)
                                         (let [user (get-in req [:cookies "username" :value])] 
                                           (execute! @db [(format "DROP TABLE IF EXISTS %s" user)])
                                           (execute! @db [(format "DELETE FROM users WHERE username = \"%s\"" user)])  
                                           (dosync
                                             (alter users
                                                    (fn [usrs]
                                                      (update-in usrs [user :data] #(mapv (memfn interrupt) %)))))
                                           (dosync
                                             (alter users dissoc user))
                                         {:status 200
                                          :body (format "<script>window.location.href = '%s/logout';</script>" 
                                                        (str (name (:scheme req)) "://" (:server-name req) \: (:server-port req)))
                                          :headers {"Content-Type" "text/html; charset=UTF-8"}})
                                       {:status 200
                                        :body (str
                                                (html
                                                  [:div {:style {:position "absolute"
                                                                 :transform "translate(650px, 200px)"}}
                                                   [:p "Are you sure you want to unregister?<br><b>This action cannot be undone.</b>"]
                                                   [:button {:id "cancel" :style {:position "absolute"
                                                                                  :transform "translate(20px, 0px)"}} "Go back"]
                                                   [:button {:id "confirm" :style {:position "absolute"
                                                                                   :transform "translate(85px, 0px)"}} "I confirm"]
                                                   [:script (slurp (jio/resource "public/js/unsubscribe.js"))]]))
                                        :headers {"Content-Type" "text/html; charset=UTF-8"}}))))
                     
  (compojure/GET "/logout" req (fn [{cookies :cookies :as req}]  
                                 {:status 200
                                  :body (redirect 1 (str (name (:scheme req)) "://" (:server-name req) \: (:server-port req)))
                                  :cookies (into {} (map (fn [[k v]] [k (assoc v :max-age -1)])) cookies)
                                  :headers {"Content-Type" "text/html; charset=UTF-8;"}}))
                                 
  (compojure/POST "/alerts" req (fn [req]
                                  (let [params (:params req)
                                        idx (:id params)
                                        username (:user params)
                                        data (read-string (:data params))
                                        email (:email data)
                                        pattern "EEE MMM dd y HH:mm:ss zzzzZ (zzzz)"
                                        simple-format (java.text.SimpleDateFormat. pattern)
                                        time-formatter (java.time.format.DateTimeFormatter/ofPattern pattern)
                                        deadline (:deadline data)
                                        deadline-inst (when-not (or (empty? deadline) (= deadline "\"") (= deadline "\\")) 
                                                        (java.time.LocalDateTime/parse deadline time-formatter))] 
                                     (->> (mult-replace (str \[ (assoc data :id idx) \]) replacements)
                                          (read-string)
                                          (insert-to-db! (.replaceAll username ";" "")))
                                     (dosync 
                                      (alter users
                                             (fn [usrs]
                                               (update-in usrs [(.replace username ";" "") :data] (fnil conj [])
                                                          (if deadline-inst
                                                            (thread-starter simple-format time-formatter deadline-inst email data)
                                                            (Thread.)))))))  
                                      {:status 201})) 
  (compojure/POST "/remove-alert" [] (fn [{params :params}]
                                       (let [{user :user id :id} params
                                             s (format "DELETE FROM %s WHERE id = %s" 
                                                       (.replaceAll user ";" "") 
                                                       (inc (read-string id)))]  
                                         (execute! @db [s])
                                         (execute! @db [(format "UPDATE %s
                                                                SET id = id - 1
                                                                WHERE id > %s" (.replaceAll user ";" "") (inc (read-string id)))]))  
                                       (remove-alert params)
                                       {:status 204}))
  (compojure/POST "/replace-alert" [] (fn [{params :params}]
                                        (let [{user :user id :id n-list :n-list} params
                                              s (-> (mult-replace n-list replacements)
                                                    (read-string)
                                                    (assoc :id (inc (read-string id)))
                                                    (str)
                                                    read-string
                                                    build-replace)] 
                                          (execute! @db [(format "UPDATE %s
                                                                 SET %s
                                                                 where id = %s"
                                                                 (.replaceAll user ";" "")
                                                                 s
                                                                 (inc (read-string id)))])) 
                                        (replace-alert params)
                                        {:status 201})) 
  (compojure/POST "/verify" [] (fn [req] 
                                 (verify-creds (:params req) 
                                               (str (name (:scheme req)) "://" (:server-name req) \: (:server-port req)))))
  (route/not-found {:status 200
                    :body "<script>location.href = window.location.origin</script>"
                    :headers {"Content-Type" "text/html; charset=UTF-8"}}))


(def app 
  (-> (fn [req] (routes req))
      wrap-cookies
      wrap-keyword-params
      wrap-params))


(def init
  (fn []
    (jetty/run-jetty (fn [req] (app req))
                     {:port 8080
                      :join? false})))

(defn get-users []
  (let [usrs (execute! @db ["SELECT * FROM users"])]  
    (dosync
      (alter users (fn [dest]
                     (let [assocs (mapcat (fn [mp]
                                            (list (:users/username mp)
                                                  {:password
                                                   (:users/password mp)}))
                                          usrs)]  
                       (if-not (empty? usrs)
                         (apply assoc dest assocs)
                         dest)))))))



(defn stop-server! []
  (when-let [server @server]
    (.close @db)  
    (.stop server))
  (reset! server nil))


(defn start-server! 
 ([]
  (if-let [_server @server]
    (do
      (stop-server!)
      (reset! server nil)  
      (reset! server (init)))
    (reset! server (init))))
 ([stub]
  (jarloc stub)
  (force db)  
  (execute! @db ["CREATE TABLE IF NOT EXISTS users (
                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                     username TEXT NOT NULL UNIQUE,
                     password TEXT NOT NULL);"])  
  (get-users)  
  (if-let [_server @server]
    (do
      (stop-server!)
      (reset! server nil)  
      (reset! server (init)))
    (reset! server (init)))))  
