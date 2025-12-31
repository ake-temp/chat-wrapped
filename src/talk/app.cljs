(ns talk.app
  (:require ["./entry.css"]
            ["./ably.js" :as ably]
            ["./chat_quiz.js" :as chat-quiz]
            ["./react_helper.js" :as r :refer [$ useAtom]]))


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

(defn App []
  ;; Subscribe to state changes
  (useAtom chat-quiz/state)
  (useAtom ably/state)
  ;; Render the quiz UI
  ($ chat-quiz/chat-quiz-ui))



;; >> Init

(defonce root (atom nil))

(defn init! []
  ;; Create React root if not exists
  (when-not @root
    (reset! root (r/createRoot (js/document.getElementById "app"))))

  ;; Render
  (.render @root ($ App))

  ;; Initialize services
  (ably/init! ABLY_API_KEY)
  (chat-quiz/init!))

(init!)
