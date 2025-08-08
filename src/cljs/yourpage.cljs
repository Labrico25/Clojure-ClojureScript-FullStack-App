(ns yourpage
  (:require [reagent.dom.client :as dom]
            [cljs.reader :as reader]
            [reagent.core :as r]
            [goog.string :as gstring]
            [helpers.two :refer [base-url]]
            [cljs-http.client :as client]
            [clojure.string :as s]))


(set! *warn-on-infer* true)


(let [cookies (.-cookie js/document)
      user (str (re-find #"username.*?;" cookies))]  
  (def username (.replace user #"username=|;" "")))

(set! js/document.body.style.backgroundImage "url(https://i.imgur.com/fS6QDfn.png)")

(set! js/document.body.style.backgroundRepeat "no-repeat")

(set! js/document.body.style.backgroundSize "900px 500px")

(set! js/document.body.style.backgroundPosition "300px 100px")

(defonce your-data (r/atom []))

(def your-email (atom nil))  

(.then (js/cookieStore.get "data")
       (fn [x]
         (let [string (.-value x)]
           (when (not-empty string)
             (let [decoded (js/decodeURIComponent string)]  
               (reset! your-data (reader/read-string
                                  (s/replace (reduce #(str %1 (if (= %2 \+) " " %2)) decoded) #"(GMT)\s+(\d+)" "$1+$2"))))))))


(add-watch your-data :your-data (fn [k reference old-state new-state]
                                  (let [cookie-store (js/cookieStore.set #js {:name "data"
                                                                              :value (s/replace (str @reference) #"(GMT)\s+(\d+)" "$1+$2")
                                                                              :path "/"})]
                                    (doto cookie-store
                                      (.then (fn [] #_(.log js/console "Cookie added successfully")))
                                      (.catch (fn [e] #_(.log js/console "There was a problem adding cookie")))))))  

(.addEventListener js/window "unload" (fn [event]
                                        (.preventDefault event)        
                                        (js/cookieStore.set #js {:name "data"
                                                                 :value (s/replace (str @your-data) #"(GMT)\s+(\d+)" "$1+$2")
                                                                 :path "/"})))

(defonce modal-email-send-warning (r/atom false))

(defonce a-new-list (r/atom false))

(def DeadLine (atom nil))

(def typing-deadline (r/atom false))

(defn type-deadline [selected display-else]  
  (when @typing-deadline
    [:div {:id "deadline-div"
                 :style {:position "fixed"
                         :transform "translate(660px, 100px)"
                         :width "220px"
                         :min-height "80px"
                         :height "auto"
                         :background-color "#ff0000"
                         :z-index "1"}}
           [:p {:contentEditable true
                :style {:position "relative"
                        :background-color "#8b0000"
                        :left "10px"
                        :width "200px"
                        :color "white"
                        :margin-bottom "40px"}}]
           [:button {:style {:position "absolute"
                             :transform "translate(10px, -28px)"}
                     :on-click #(do % (reset! selected "no") (reset! display-else true) (reset! typing-deadline false))}
            "cancel"]
           [:button {:style {:position "absolute"
                             :transform "translate(170px, -28px)"}
                     :on-click (fn [event]  
                                 (let [this (.-target event)
                                       parent (.-offsetParent this)
                                       p-element (.querySelector parent "p")
                                       deadline (js/Date. (js/Date.parse (.-textContent p-element)))]  
                                   (if (not= (str deadline) "Invalid Date")
                                     (let []  
                                       (reset! DeadLine deadline)
                                       (reset! typing-deadline false)
                                       (reset! display-else true))
                                     (js/alert "Invalid Date format"))))}  
            "Set!"]]))

(defn modal-email-send []
  (let [selected (r/atom "")
        display-else (r/atom true)
        input-value (r/atom "")]
    (fn []
      (when @modal-email-send-warning
        [:<> [type-deadline selected display-else] 
         (when @display-else
           [:div {:style {:position "fixed"
                       :width "220px"
                       :height "250px"
                       :background-color "#333333"
                       :transform "translate(660px, 100px)"
                          :z-index "1"}}
            [:form {:on-submit (fn [event]
                              (.preventDefault event)
                              (let [this (.-target event)
                                    seld (.. this (querySelector "select") -value)
                                    email (.. this (querySelector "input") -value)]
                                (cond
                                  (and (= seld "") (= email "")) (js/alert "You must type an email and pick an option")
                                  (not (.endsWith email "@gmail.com")) (js/alert "Invalid email")  
                                  (= seld "") (js/alert "You must select an option")
                                  (= email "") (js/alert "You must type an email")
                                  :else (do (reset! your-email email) 
                                            (reset! selected "")
                                            (reset! a-new-list true)))))}
             [:input {:name "email"
                      :value @input-value
                      :on-change #(reset! input-value (.. % -target -value))  
                      :placeholder "type your email here"
                      :style {:position "absolute"
                              :transform "translate(30px, 30px)"}}]
             [:label {:style {:position "absolute"
                              :transform "translate(50px, 80px)"
                              :color "white"}} "Send email?"]
             [:select {:value @selected
                       :on-change (fn [event]
                                    (let [value (.. event -target -value)]
                                      (if (= value "yes")
                                        (do (reset! typing-deadline true)
                                            (reset! selected value)
                                            (reset! display-else false))
                                        (reset! selected value))))
                       :style {:position "absolute"
                               :transform "translate(50px, 130px)"}}
              [:option {:value ""} "Choose an option"]
              [:option {:value "yes"} "Yes"]
              [:option {:value "no"} "No"]]
             [:button {:on-click #(do (.preventDefault %) 
                                      (reset! selected "") 
                                      (reset! DeadLine nil) 
                                      (reset! input-value "") 
                                      (reset! modal-email-send-warning false))
                       :style {:position "absolute"
                               :transform "translate(30px, 180px)"}}
              "cancel"]
             [:button {:type "submit"
                       :style {:position "absolute"
                               :transform "translate(140px, 180px)"}} "Submit"]]])]))))

(defn the-new-list []
  (when @a-new-list
    (reset! modal-email-send-warning false)
    [:div {:style {:position "fixed"
                   :min-height "100px"
                   :height "auto"
                   :width "435px"
                   :background-color "#333333"
                   :transform "translate(500px, 100px)"
                   :z-index "1"}}
     [:p {:content-editable true
          :style {:position "relative"
                  :left "15px"
                  :margin-bottom "50px"
                  :background-color "white" 
                  :width "400px"}}]
     [:button {:style {:position "absolute"
                       :transform "translate(30px, -30px)"}
               :on-click (fn [event]
                           (reset! a-new-list false))}   
      "Cancel"]
     [:button {:style {:position "absolute"
                       :transform "translate(350px, -30px)"}
               :on-click (fn [event]
                           (let [this (.-target event)
                                 parent (.-offsetParent this)
                                 p-element (. parent (querySelector "p"))
                                 idx ((comp inc count) @your-data)
                                 data (.-textContent p-element)
                                 list-name (gensym "your-list")
                                 created (js/Date.)
                                 actual-list {:list list-name
                                              :created created
                                              :last-modified created
                                              :deadline (str @DeadLine)
                                              :data data
                                              :email @your-email}]  
                             (swap! your-data conj actual-list)  
                             (client/post (base-url "/alerts") {:form-params {:user username
                                                                              :id idx
                                                                              :data (str actual-list)}})  
                             (reset! DeadLine nil)  
                             (reset! a-new-list false)))}  
      "Create"]]))
  


(def th-style {:width "240px" :style {:background-color "#D0C4C0"}})

(def td-style {:height "100px" :width "240px" :background-color "#ffffff"})

(def index (atom -1))

(def data->modify (r/atom nil))

(defn mapvec-indexed [f coll]
  (into [] (map-indexed f) coll))  

(defn modifier-list []
  (when-let [idx @data->modify]
    [:div {:style {:position "fixed"
                   :min-height "100px"
                   :height "auto"
                   :width "435px"
                   :background-color "#333333"
                   :transform "translate(500px, 100px)"
                   :z-index "1"}}
     [:p {:content-editable true
          :style {:position "relative"
                  :left "15px"
                  :margin-bottom "50px"
                  :background-color "white" 
                  :width "400px"}}
      (let [data (:data (@your-data idx))]
        (when-not (or (= data "\"") (= data "\\"))
          data))]  
     [:button {:style {:position "absolute"
                       :transform "translate(30px, -30px)"}
               :on-click (fn [_] (reset! data->modify nil))}
      "cancel"]
     [:button {:style {:position "absolute"
                       :transform "translate(350px, -30px)"}
               :on-click (fn [event]
                           (let [this (.-target event)
                                 parent (.-offsetParent this)
                                 p-element (.querySelector parent "p")
                                 data (.-textContent p-element)]
                             (swap! your-data (comp vec #(map-indexed (fn [index data-map]
                                                                         (if (= index idx)
                                                                           (assoc data-map :data data :last-modified (str (js/Date.)))
                                                                           data-map)) %)))
                             (client/post (base-url "/replace-alert")
                                          {:form-params {:id idx
                                                         :user username
                                                         :n-list (str (@your-data idx))}})
                             (reset! data->modify nil)))}
      "modify"]]))

(defn modify-deadline [idx-button]
  (when @idx-button
    [:div {:style {:position "fixed"
                         :transform "translate(600px, -300px)"
                         :width "220px"
                         :min-height "80px"
                         :height "auto"
                         :background-color "#ff0000"
                         :z-index "1"}}
           [:p {:contentEditable true
                :style {:position "relative"
                        :background-color "#8b0000"
                        :left "10px"
                        :width "200px"
                        :color "white"
                        :margin-bottom "40px"}}]
           [:button {:style {:position "absolute"
                             :transform "translate(10px, -28px)"}
                     :on-click #(do % (reset! idx-button nil))}
            "cancel"]
           [:button {:style {:position "absolute"
                             :transform "translate(170px, -28px)"}
                     :on-click (fn [event]
                                 (let [this (.-target event)
                                       parent (.-offsetParent this)
                                       p-element (.querySelector parent "p")
                                       deadline (js/Date. (js/Date.parse (.-textContent p-element)))]
                                   (if (not= (str deadline) "Invalid Date")
                                     (do  
                                       (swap! your-data (comp vec #(map-indexed (fn [index data]
                                                                                  (if (= index (dec @idx-button))
                                                                                    (assoc data :deadline (str deadline))
                                                                                    data)) %)))
                                       (client/post (base-url "/replace-alert") 
                                                      {:form-params {:id (dec @idx-button) 
                                                                   :user username 
                                                                   :n-list (str (@your-data (dec @idx-button)))}})  
                                       (reset! idx-button nil))
                                     (js/alert "Invalid Date"))))}  
            "Set!"]]))

(defn remove-mail-entry! [your-data]
  (into [] 
        (map #(dissoc % :email))
        your-data))

(defn pass-index [f]
  (fn [event]
    (let [row (.. event -target -parentElement -parentElement)
          row-id (.-id row)
          idx (js/parseInt (.replace row-id "row-" ""))]
      (f idx))))

(def rename? (r/atom nil))

(defn modfy-name []
  (let [content (r/atom nil)]
    (fn []  
      (when-let [idx @rename?]
        [:div {:style {:position "fixed"
                       :min-height "100px"
                       :height "auto"
                       :width "435px"
                       :background-color "#333333"
                       :transform "translate(500px, 150px)"
                       :z-index "1"}}
         [:input {:id "modfy-name"
                  :type "text"
                  :value @content
                  :on-change (fn [e] (reset! content (.. e -target -value)))
                  :style {:position "absolute"
                          :transform "translate(15px, 20px)"
                          :margin-bottom "50px"
                          :background-color "white" 
                          :width "400px"}}]
         [:button {:style {:position "absolute"
                           :transform "translate(10px, 65px)"
                           :width "69.29px"
                           :height "21.2px"}
                   :on-click (fn [_]
                               (reset! content nil)  
                               (reset! rename? nil))}
          "cancel"]
         [:button {:style {:position "absolute"
                           :transform "translate(350px, 65px)"
                           :width "69.29px"
                           :height "21.2px"}
                   :on-click (fn [e]
                               (.preventDefault e)
                               (let [input-el (js/document.getElementById "modfy-name")
                                     new-content (.-textContent input-el)]  
                                 (swap! your-data (fn [data]
                                                    (->> data
                                                      (mapvec-indexed (fn [index data-map]
                                                                        (if (= index idx)
                                                                          (assoc data-map :list (str @content))
                                                                          data-map))))))
                                 (client/post (base-url "/replace-alert") {:form-params {:id idx 
                                                                                         :user username 
                                                                                         :n-list (str (@your-data idx))}})
                                 (reset! content nil)  
                                 (reset! rename? nil)))}
          "Rename!"]]))))
      

(defn populate-table [your-data]
  (reset! index -1)  
  (let [idx-button (r/atom nil)
        table-rows (fn [] 
                     (doall
                       (map (fn [data]
                              (swap! index inc)  
                              (let [priority [:list :created :last-modified :deadline]]
                                [:tr {:key @index :id (str "row-" @index) :style {:z-index 1}}
                                 (doall
                                   (concat
                                     (map (fn [k]
                                            (cond 
                                              (= k :list)  
                                              [:td {:key (gensym) 
                                                    :style td-style}
                                               [:p {:style {:position "absolute"
                                                            :transform "translate(20px, -35px)"
                                                            :height "20px"
                                                            :width "80px"
                                                            :overflow "hidden"}}
                                                (let [dat (str (data k))] (when-not (or (= dat "\"") (= dat "\\")) dat))]
                                               [:button {:style {:position "absolute"
                                                                 :transform "translate(10px, 10px)"
                                                                 :background "none"
                                                                 :border "none"
                                                                 :cursor "pointer"
                                                                 :color "blue"}
                                                         :on-click (pass-index (fn [idx] (reset! rename? (dec idx))))}
                                                "Rename"]
                                               [:button {:style {:position "absolute"
                                                                 :transform "translate(70px, 10px)"
                                                                 :background "none"
                                                                 :border "none"
                                                                 :cursor "pointer"
                                                                 :color "blue"}
                                                         :on-click (pass-index (fn [idx] (reset! data->modify (dec idx))))}
                                                "Modify"]]
                                              (= k :deadline)
                                              [:td {:key (gensym)
                                                    :style td-style}
                                               [:p (str (let [dat (data k)] (when-not (or (= dat "\"") (= dat "\\")) dat)))]
                                               [:button {:on-click (fn [event]
                                                                     (let [row (.. event -target -parentElement -parentElement)
                                                                           row-id (.-id row)
                                                                           row-idx (js/parseInt (.replace row-id "row-" ""))]
                                                                       (reset! idx-button row-idx)))}  
                                                "✎"]]
                                              :else  
                                              [:td {:key (gensym)
                                                    :style td-style}
                                               [:p {:style {:position "absolute"
                                                            :max-width "140px"
                                                            :transform "translate(0px, -63px)"}} (str (data k))]]))
                                          priority)
                                     [[:th {:key (gensym) :style td-style}]
                                      [:td {:key (gensym) :style td-style}
                                       [:button {:style {:position "absolute"
                                                         :transform "translate(35px,-20px)"}
                                                 :on-click (fn [event]
                                                             (.preventDefault event)
                                                             (let [row (.. event -target -parentElement -parentElement)
                                                                   row-idx (.-id row)
                                                                   idx (js/parseInt (.replace row-idx "row-" ""))]
                                                               (client/post (base-url "/remove-alert") {:form-params {:user username :id (dec idx)}})  
                                                               (swap! your-data (comp vec #(keep-indexed (fn [index item]
                                                                                                           (when-not (= index (dec idx))
                                                                                                             item))
                                                                                                         %)))))}
                                        "❌"]]]))]))
                          (remove-mail-entry! @your-data))))]
    [:<> [modify-deadline idx-button]  
     [:table {:id "user-table"}
      [:tbody
       (if (and @your-data (not-empty @your-data))
       (cons [:tr {:key (do (swap! index inc) @index)}
              [:th th-style "list"]
              [:th th-style "created"]
              [:th th-style "last modified"]
              [:th th-style "Deadline"]
              [:td th-style [:a {:href ""
                                 :on-click #(do (.preventDefault %) (reset! modal-email-send-warning true))
                                 :style {:text-decoration "none"
                                         :position "absolute"
                                         :transform "translate(10px, -10px)"}} "add new-list"]]
              [:th th-style]]
             (table-rows))
         [:tr {:key (do (swap! index inc) @index)}
              [:th th-style "list"]
              [:th th-style "created"]
              [:th th-style "last modified"]
              [:td th-style [:a {:href ""
                                 :on-click #(do (.preventDefault %) (reset! modal-email-send-warning true))
                                 :style {:text-decoration "none"
                                         :position "absolute"
                                         :transform "translate(30px, -10px)"}} "add new-list"]]
          [:th th-style]])]]]))

(def logout-background "https://tse3.mm.bing.net/th/id/OIP.6cP61YyDaNhT3cRAT72ynwHaFj?r=0&rs=1&pid=ImgDetMain&o=7&rm=3")

(defn yourpage-layout []
  [:div
   [:p {:style {:position "absolute"
                :transform "translate(500px, 0px)"}}
    [:strong "Note:"]
    "Expected deadline format is yyyy/mm/dd HH:mm:ss or yyyy-mm-dd HH:mm:ss"]
   [:div {:style {:position "absolute"
                  :width "1000px"
                  :height "50px"
                  :transform "translate(250px, 50px)"
                  :background-image (str "url(" logout-background ")")}}
    [:a {:href (base-url "/logout")
         :style {:position "absolute"
                 :text-decoration "none"
                 :transform "translate(250px, 10px)"}
         :on-click (fn [_]
                     (set! js/window.location.href (base-url "/logout")))} 
     
     [:strong {:style {:font-size "20px" :color "white"}} "logout"]]
    [:a {:href (base-url "/unsubscribe")
         :style {:position "absolute"
                 :text-decoration "none"
                 :transform "translate(750px, 10px)"}
         :on-click (fn [_]
                     (set! js/window.location.href (base-url "/unsubscribe")))}
     [:strong {:style {:font-size "20px" :color "white"}} "unsubscribe"]]]
   [modal-email-send]
   [the-new-list]
   [modifier-list]
   [modfy-name]
   [:div {:style {:position "absolute"
                  :width "800px"
                  :transform "translate(70px, 400px)"
                  :background-color "black"}}
    [populate-table your-data]]])

(defn init-yourpage []
  (let [^js root (dom/create-root (.getElementById js/document "my-page-wrapper"))] 
    (dom/render root [yourpage-layout])))