(ns talk.display
  (:require ["./ably.js" :as ably]
            ["./presenter.js" :as presenter]))


;; >> State

(def state (atom {:slide-index 0
                  :active-question nil
                  :votes {}  ;; {client-id -> vote}
                  :audience-count 0}))

;; >> Vote Processing

(defn process-vote [vote]
  (let [client-id (:client-id vote)
        question-id (:question-id vote)]
    ;; Only process votes for the active question
    (when (= question-id (:id (:active-question @state)))
      (swap! state assoc-in [:votes client-id] vote))))

;; >> Aggregation

(defn get-votes []
  (vals (:votes @state)))

(defn scale-stats []
  (let [votes (get-votes)
        values (map :value votes)]
    (when (seq values)
      {:count (count values)
       :average (/ (reduce + values) (count values))
       :distribution (frequencies values)})))

(defn choice-stats []
  (frequencies (map :value (get-votes))))

(defn text-responses []
  (map :value (get-votes)))

;; >> UI Components

(defn scale-results-ui [question]
  (let [stats (scale-stats)
        audience (:audience-count @state)]
    [:div {:class "space-y-4"}
     [:h2 {:class "text-3xl font-bold text-center"} (:text question)]
     (if stats
       [:div {:class "text-center space-y-2"}
        [:div {:class "text-6xl font-bold"}
         (.toFixed (:average stats) 1)]
        [:div {:class "text-xl text-gray-600"}
         "Average from " (:count stats) " of " audience " votes"]
        [:div {:class "flex justify-center gap-1 mt-4"}
         (for [n (range (get-in question [:options :min])
                        (inc (get-in question [:options :max])))]
           (let [cnt (get (:distribution stats) n 0)]
             ^{:key n}
             [:div {:class "text-center"}
              [:div {:class "bg-blue-600 w-8"
                     :style {:height (str (* cnt 20) "px")}}]
              [:div {:class "text-sm"} n]]))]]
       [:div {:class "text-xl text-gray-500 text-center"}
        "Waiting for votes... (0 of " audience ")"])]))

(defn choice-results-ui [question]
  (let [stats (choice-stats)
        total (reduce + (vals stats))
        audience (:audience-count @state)]
    [:div {:class "space-y-4"}
     [:h2 {:class "text-3xl font-bold text-center"} (:text question)]
     [:div {:class "text-center text-gray-600 mb-4"}
      total " of " audience " voted"]
     [:div {:class "space-y-2"}
      (for [opt (:options question)]
        (let [cnt (get stats opt 0)
              pct (if (pos? total) (* 100 (/ cnt total)) 0)]
          ^{:key opt}
          [:div {:class "flex items-center gap-2"}
           [:div {:class "w-32 text-right"} opt]
           [:div {:class "flex-1 bg-gray-200 rounded h-8"}
            [:div {:class "bg-blue-600 h-8 rounded"
                   :style {:width (str pct "%")}}]]
           [:div {:class "w-16"} cnt]]))]]))

(defn text-results-ui [question]
  (let [responses (text-responses)
        audience (:audience-count @state)]
    [:div {:class "space-y-4"}
     [:h2 {:class "text-3xl font-bold text-center"} (:text question)]
     [:div {:class "text-center text-gray-600"}
      (count responses) " of " audience " responded"]
     [:div {:class "max-h-96 overflow-y-auto space-y-2"}
      (for [[idx resp] (map-indexed vector responses)]
        ^{:key idx}
        [:div {:class "p-2 bg-gray-100 rounded"} resp])]]))

(defn question-results-ui [question]
  (case (:kind question)
    :scale [scale-results-ui question]
    :choice [choice-results-ui question]
    :text [text-results-ui question]
    [:div "Unknown question type"]))

(defn slide-ui []
  [:div {:class "w-screen h-screen flex items-center justify-center bg-gray-100"}
   [:div {:class "text-center"}
    [:h1 {:class "text-6xl font-bold"} "Slide " (:slide-index @state)]
    [:p {:class "text-xl text-gray-600 mt-4"}
     (:audience-count @state) " audience members connected"]]])

(defn connection-pill []
  (let [status (ably/connection-status)]
    (when-not (ably/connected?)
      [:div {:class "fixed bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 bg-yellow-500 text-white rounded-full text-sm font-medium shadow-lg"}
       "Connection status: " status])))

(defn display-ui []
  [:div {:class "w-screen h-screen"}
   (if-let [q (:active-question @state)]
     [:div {:class "w-full h-full flex items-center justify-center bg-white p-8"}
      [question-results-ui q]]
     [slide-ui])
   [connection-pill]])

;; >> Init

(defn init! []

  ;; Re-render when ably connection status changes
  (add-watch ably/state ::connection
    (fn [_ _ _ _]
      (swap! state identity)))

  ;; Watch audience count
  (ably/on-presence-change! "audience"
    (fn []
      (ably/get-presence-members "audience"
        (fn [members]
          (swap! state assoc :audience-count (count members))))))

  ;; Watch presenter state
  (presenter/on-state-change!
    (fn []
      (presenter/get-state
        (fn [presenter-state]
          (when presenter-state
            (let [new-question (:active-question presenter-state)
                  current-question (:active-question @state)]
              ;; Clear votes when question changes
              (when (not= (:id new-question) (:id current-question))
                (swap! state assoc :votes {}))
              (swap! state assoc
                     :slide-index (:slide-index presenter-state)
                     :active-question new-question)))))))

  ;; Subscribe to votes
  (ably/subscribe! "votes" process-vote))
