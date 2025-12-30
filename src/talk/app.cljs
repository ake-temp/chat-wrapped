(ns talk.app
  (:require ["reagami" :as reagami]
            ["./entry.css"]
            ["./ably.js" :as ably]
            ["./chat_quiz.js" :as chat-quiz]))


;; >> Configuration
(def ABLY_API_KEY "1tATiA.Rlyw7w:ZAjKA1HQea3nu2jtBE0-u_WVMXWtF4ONodw_MZn4kc0")



;; >> Router

(defn get-role []
  (let [params (js/URLSearchParams. (.-search js/location))]
    (if (.get params "control")
      :presenter
      :audience)))

(def role (get-role))



;; >> App Component

(defn app []
  [chat-quiz/chat-quiz-ui])



;; >> Render Loop

(defn render []
  (reagami/render (js/document.getElementById "app") [app]))

(add-watch chat-quiz/state ::render (fn [_ _ _ _] (render)))



;; >> Init

(defn init! []
  (render)
  (ably/init! ABLY_API_KEY)
  (chat-quiz/init! (= role :presenter)))

(init!)
