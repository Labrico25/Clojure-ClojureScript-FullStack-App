(ns registration
  (:require-macros [cljs.core.async.macros :refer [go]])  
  (:require [cljs-http.client :as http]
            [helpers.two :refer [mapify base-url]]
            [reagent.dom.client :as dom]
            [cljs.core.async :refer [<!]]
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

(defn layout []
  [:div {:style {:position "relative"
                 :left "600px"
                 :top "200px"
                 :height "200px"
                 :width "300px"
                 :background-color "white"
                 :border "2px solid black"}}
   [:button {:style {:position "absolute"
                     :transform "translate(265px, 10px)"
                     :background "none"
                     :border "none"
                     :cursor "pointer"
                     :color "red"}
             :on-click (fn []
                         (.back js/history))}  
    "X"]
   [:div {:id "error-reg"
          :style {:position "relative"
                  :top "10px"
                  :left "50px"}} [:p]]
   [:form {:on-submit (fn [req]
                       (.preventDefault req)   
                       (go
                         (let [formdata (js/FormData. (. req -target))
                               entries (mapify (. formdata entries))
                               response (<! (http/post (base-url "/save")
                                                       {:form-params (into {} entries)}))]
                           (if (:success response)
                             (set! js/window.location.href (base-url ""))
                             (let [[target] (array-seq (.-children (.getElementById js/document "error-reg")))]
                               (set! (.-textContent target) "Username already exists!")
                               (gstyle/setStyle target "background-color" "#ff3b3b")
                               (gstyle/setStyle target "color" "white")
                               (gstyle/setStyle target "width" "180px")  
                                 )))))}  
    [:label {:style {:position "relative"
                     :top "20px"
                     :left "20px"}} "Username:"]
    [:input {:name "username" 
             :type "text"
             :style {:position "relative"
                     :top "38px"
                     :right "10px"}}]
    [:label {:style {:position "relative"
                     :top "40px"
                     :left "20px"}} "Password:"]
    [:input {:name "password" 
             :type "text"
             :style {:position "relative"
                     :top "58px"
                     :right "10px"}}]
    [:button {:type "submit"
              :style {:position "relative"
                      :top "70px"
                      :left "120px"}} "Register!"]]])

(defn init-registration []
  (let [^js root (dom/create-root (.getElementById js/document "registration-element"))]
    (dom/render root [layout])))