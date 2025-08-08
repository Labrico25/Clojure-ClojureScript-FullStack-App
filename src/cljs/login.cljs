(ns login
  (:require-macros [cljs.core.async.macros :refer [go]])  
  (:require [reagent.core :as reagent]
            [helpers.two :refer [mapify base-url]]
            [reagent.dom.client :as dom]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [goog.style :as gstyle]))


(set! *warn-on-infer* true)

(let [link (js/document.createElement "link")
      title (js/document.createElement "title")]
  (set! (.-rel link) "icon")
  (set! (.-href link) "https://cdn.pixabay.com/photo/2017/06/10/07/18/list-2389219_1280.png")
  (set! (.-type link) "image/png")
  (.appendChild js/document.head link)
  (set! (.-textContent title) "Todo App")
  (.appendChild js/document.head title))  

(set! js/document.body.style.backgroundImage "url(https://i.imgur.com/fS6QDfn.png)")

(set! js/document.body.style.backgroundRepeat "no-repeat")

(set! js/document.body.style.backgroundSize "900px 500px")

(set! js/document.body.style.backgroundPosition "300px 100px")                          

(defn init-login []
  (let [^js root (dom/create-root (.getElementById js/document "MAIN"))]
    (dom/render root
              [:div {:style {:text-align "center"
                             :position "relative"
                             :top "100px"
                             :left "600px"
                             :height "500px"
                             :width "300px"
                             :background-color "white"
                             :border "2px solid black"}}
               [:button {:style {:position "absolute"
                                 :transform "translate(110px, 10px)"
                                 :background "none"
                                 :border "none"
                                 :cursor "pointer"
                                 :color "red"}
                         :on-click (fn [e]
                                     (.preventDefault e)
                                     (set! (.-href js/window.location)  
                                       (base-url "")))}
                "X"]
               [:div {:id "wrong-div"
                      :style {:position "relative"
                              :left "40px"
                              :top "10px"}}
                [:p {:style {:position "relative"
                              }} ]]
               [:div {:style {:position "relative"
                              :top "150px"
                              :left "10px"}}
                [:a {:href "/users/register"} "Don't have an account? Register now!"]]
               [:form {:on-submit (fn [event]
                                   (.preventDefault event)   
                                   (go  
                                    (let [form (js/FormData. (. event -target))
                                          entries (mapify (. form entries))
                                          response (<! (http/post (base-url "/verify")  
                                                                  {:form-params (into {} entries)}))]
                                      (if (:success response)
                                        (let [body (:body response)
                                              url (re-find #"url=.*?\"" body)]
                                          (set! js/window.location.href 
                                            (str/replace url #"url=|\"" "")))  
                                        (let [[target] (array-seq (.-children (.getElementById js/document "wrong-div")))]
                                          (set! (.-textContent target) "Wrong username or password!")
                                          (gstyle/setStyle target "background-color" "#ff3b3b")
                                          (gstyle/setStyle target "color" "white")
                                          (gstyle/setStyle target "width" "210px"))))))}  
                [:input {:name "username" 
                         :type "text"
                         :style {:position "relative"
                                 :top "40px"
                                 :right "10px"}}]
                [:input {:name "password" 
                         :type "password"
                         :style {:position "relative"
                                 :top "80px"
                                 :left "10px"}}]
                [:button {:type "submit"
                          :style {:position "relative"
                                  :top "80px"
                                  :left "10px"}} "next"]]])))
     

