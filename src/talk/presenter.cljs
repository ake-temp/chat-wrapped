(ns talk.presenter
  (:require ["./ably.js" :as ably]))


;; >> State

(def default-state {:slide-index 0
                    :active-question nil
                    :audience-count 0})

(defn save-state! []
  (js/localStorage.setItem "presenter-state"
    (js/JSON.stringify (select-keys @state [:slide-index :active-question]))))

(defn load-state []
  (when-let [saved (js/localStorage.getItem "presenter-state")]
    (let [parsed (js/JSON.parse saved)]
      {:slide-index (or (:slide-index parsed) 0)
       :active-question (:active-question parsed)})))

(def state (atom (merge default-state (load-state))))



;; >> Questions Data

(def questions
  [{:id "q1"
    :text "How familiar are you with Clojure?"
    :kind :scale
    :options {:min 1 :max 10}}
   {:id "q2"
    :text "What's your favorite programming paradigm?"
    :kind :choice
    :options ["Functional" "Object-Oriented" "Procedural" "Other"]}
   {:id "q3"
    :text "What feature would you like to see next?"
    :kind :text}])

(def CHANNEL "presenter")



;; >> Presence Helpers

(defn get-state [callback]
  (ably/get-presence-members CHANNEL
    (fn [members]
      (callback (when (seq members) (.-data (first members)))))))

(defn on-state-change! [callback]
  (ably/on-presence-change! CHANNEL callback))

;; >> Sync state to presenter channel

(defn sync-state! []
  (save-state!)
  (ably/update-presence! CHANNEL {:slide-index (:slide-index @state)
                                   :active-question (:active-question @state)}))



;; >> Control Actions

(defn next-slide! []
  (swap! state update :slide-index inc)
  (sync-state!))

(defn prev-slide! []
  (swap! state update :slide-index #(max 0 (dec %)))
  (sync-state!))

(defn activate-question! [question]
  (swap! state assoc :active-question question)
  (sync-state!))

(defn close-question! []
  (swap! state assoc :active-question nil)
  (sync-state!))

(defn reset-state! []
  (js/localStorage.removeItem "presenter-state")
  (swap! state merge {:slide-index 0 :active-question nil})
  (sync-state!))



;; >> UI Components

(defn button [attrs & children]
  (into [:button (merge {:class "px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 active:bg-blue-800 disabled:bg-gray-400"}
                        attrs)]
        children))

(defn question-button [q disabled?]
  (let [active? (= (:id q) (:id (:active-question @state)))]
    [button {:class (if active?
                      "px-4 py-2 bg-green-600 text-white rounded-lg disabled:bg-gray-400"
                      "px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400")
             :on-click (if active?
                         close-question!
                         #(activate-question! q))
             :disabled disabled?}
     (if active? "Close: " "Open: ") (:text q)]))

(defn connection-pill []
  (let [status (ably/connection-status)]
    (when-not (ably/connected?)
      [:div {:class "fixed bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 bg-yellow-500 text-white rounded-full text-sm font-medium shadow-lg"}
       "Connection status: " status])))

(defn presenter-ui []
  (let [disabled? (not (ably/connected?))]
    [:div {:class "p-4 space-y-6 max-w-md mx-auto"}
     [:h1 {:class "text-2xl font-bold"} "Presenter Controls"]

     [:div {:class "text-lg"}
      "Audience connected: " (:audience-count @state)]

     [:div {:class "space-y-2"}
      [:h2 {:class "text-xl font-semibold"} "Slides"]
      [:div {:class "flex gap-2"}
       [button {:on-click prev-slide! :disabled disabled?} "Prev"]
       [:span {:class "px-4 py-2"} "Slide " (:slide-index @state)]
       [button {:on-click next-slide! :disabled disabled?} "Next"]]]

     [:div {:class "space-y-2"}
      [:h2 {:class "text-xl font-semibold"} "Questions"]
      (for [q questions]
        ^{:key (:id q)}
        [question-button q disabled?])]

     [:div {:class "pt-4 border-t"}
      [button {:class "px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400"
               :on-click reset-state!
               :disabled disabled?}
       "Reset"]]

     [connection-pill]]))



;; >> Init

(defn init! []

  ;; Re-render when ably connection status changes
  (add-watch ably/state ::connection
    (fn [_ _ _ _]
      (swap! state identity)))

  ;; Enter presenter presence with initial state
  (ably/enter-presence! CHANNEL {:slide-index (:slide-index @state)
                                  :active-question (:active-question @state)})

  ;; Watch audience count
  (ably/on-presence-change! "audience"
    (fn []
      (ably/get-presence-members "audience"
        (fn [members]
          (swap! state assoc :audience-count (count members)))))))
