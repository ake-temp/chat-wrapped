(ns talk.app
  (:require ["./entry.css"]
            ["./chat_quiz.js" :as chat-quiz]
            ["./react_helper.js" :as r :refer [$ useAtom]]))



;; >> App Component

(defn App []
  ;; Subscribe to state changes
  (useAtom chat-quiz/state)
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

  ;; Initialize chat quiz
  (chat-quiz/init!))

(init!)
