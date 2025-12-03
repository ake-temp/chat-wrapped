(ns talk.app
  (:require ["reagami" :as reagami]
            ["./entry.css"]
            ["./ably.js" :as ably]
            ["./presenter.js" :as presenter]
            ["./audience.js" :as audience]
            ["./display.js" :as display]))


;; >> Configuration
(def ABLY_API_KEY "1tATiA.Rlyw7w:ZAjKA1HQea3nu2jtBE0-u_WVMXWtF4ONodw_MZn4kc0")



;; >> Router

(defn get-role []
  (let [params (js/URLSearchParams. (.-search js/location))]
    (cond
      (.get params "display") :display
      (.get params "control") :presenter
      :else :audience)))

(def role (get-role))



;; >> App Component

(defn app []
  (case role
    :display [display/display-ui]
    :presenter [presenter/presenter-ui]
    :audience [audience/audience-ui]))



;; >> Render Loop

(defn render []
  (reagami/render (js/document.getElementById "app") [app]))

;; Get the right state atom based on role
(def current-state
  (case role
    :display display/state
    :presenter presenter/state
    :audience audience/state))

(add-watch current-state ::render (fn [_ _ _ _] (render)))



;; >> Init

(defn init! []
  (render)
  (ably/init! ABLY_API_KEY)
  (case role
    :display (display/init!)
    :presenter (presenter/init!)
    :audience (audience/init!)))

(init!)
