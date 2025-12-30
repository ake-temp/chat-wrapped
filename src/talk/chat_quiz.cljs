(ns talk.chat-quiz
  (:require ["./ably.js" :as ably]
            ["./ui.js" :as ui]
            [clojure.string :as str]))



;; >> Configuration

(def CHANNEL "chat-quiz")
(def QUIZ-CHANNEL "chat-quiz-answers")
(def RESET-CHANNEL "chat-quiz-reset")

(def participant-names
  ["Jack Crowson" "Jack Rowland" "James" "Zack" "Lee" "Oliver"
   "Retrospectre" "Samir" "Jaspreet" "Liam" "Georgia" "Mariana"
   "Jack McMillan" "Alice" "Jess" "Aidan"])

;; Profile photo mappings (name -> filename)
(def profile-photos
  {"Jack Crowson" "/profile_photos/Jack_Crowson.jpg"
   "James" "/profile_photos/James.jpg"
   "Zack" "/profile_photos/Zack_Pollard.jpg"
   "Lee" "/profile_photos/Lee_Bain.jpg"
   "Oliver" "/profile_photos/Oliver_Marshall.jpg"
   "Retrospectre" "/profile_photos/Retrospectre.jpg"
   "Jaspreet" "/profile_photos/Jaspreet_Crowson.jpg"
   "Liam" "/profile_photos/Liam_Moloney.jpg"
   "Georgia" "/profile_photos/Georgia.jpg"
   "Mariana" "/profile_photos/Mariana.jpg"
   "Jess" "/profile_photos/Jess_Brookfield.jpg"})

;; Sticker mappings (emoji -> filename)
(def sticker-images
  {"💛" "/stickers/yellow.webp"
   "😭🤦" "/stickers/crying_facepalm.webp"
   "🤣" "/stickers/rofl.webp"
   "🙏" "/stickers/hands_together.webp"
   "😅" "/stickers/sweat_cry.webp"
   "😆" "/stickers/omegalol.webp"
   "😭🙌" "/stickers/crying_hands_in_air.webp"
   "💙" "/stickers/blue.webp"
   "📞" "/stickers/telephone.webp"
   "🐧" "/stickers/penguin.webp"})



;; >> Quiz Data - All Messages

;; Message types:
;; :text - Simple text message
;; :buttons - 2xN grid of buttons for selection
;; :input - Text input for number entry
;; :reveal - Show answer with ranked list
;; :scores - Show current leaderboard

(def quiz-messages
  [;; Name selection
   {:type :text :content "Welcome to Chat: Wrapped! 🎉"}
   {:type :text :content "First, select your name:"}
   {:type :name-select :id "name-select"}

   ;; Intro
   {:type :text :content "Now let's see how well you know the Chat with a quiz 📊"}

   ;; Round 1
   {:type :text :content "Round 1" :style :header}
   {:type :text :content "Let's start off with some basic stats"}

   ;; Q1: Most messages
   {:type :text :content "Who has sent the most messages?"}
   {:type :buttons
    :id "q1-messages"
    :options ["Jack Crowson" "Jack Rowland" "James" "Zack" "Lee" "Oliver"]}
   {:type :reveal
    :id "q1-messages"
    :answer "Jack Crowson"
    :rankings [["Jack Crowson" 74441]
               ["Jack Rowland" 72521]
               ["James" 43574]
               ["Zack" 43102]
               ["Lee" 20940]
               ["Oliver" 18458]]}

   ;; Q2: Most popular sticker
   {:type :text :content "Which is the most popular sticker?"}
   {:type :buttons
    :id "q2-sticker"
    :options ["😭🤦" "🤣" "💛" "😅" "🙏" "😭🙌" "🐧" "📞" "😆" "💙"]
    :labels ["Crying facepalm" "ROFL" "Yellow" "Sweat cry" "Hands together"
             "Crying hands" "Penguin" "Telephone" "Omegalol" "Blue"]}
   {:type :reveal
    :id "q2-sticker"
    :answer "💛"
    :rankings [["💛 Yellow" 1444]
               ["😭🤦 Crying facepalm" 1436]
               ["🤣 ROFL" 1378]
               ["🙏 Hands together" 698]
               ["😅 Sweat cry" 673]
               ["😆 Omegalol" 663]
               ["😭🙌 Crying hands" 541]
               ["💙 Blue" 485]
               ["📞 Telephone" 480]
               ["🐧 Penguin" 393]]}

   ;; Q3: Longest gap
   {:type :text :content "How many hours was the largest gap in messages?"}
   {:type :buttons
    :id "q3-gap"
    :options ["4 hours" "22 hours" "45 hours" "83 hours"]}
   {:type :reveal :id "q3-gap" :answer "45 hours"}
   {:type :text :content "Can't even survive the weekend without a cheeky message"}

   ;; Scores checkpoint
   {:type :text :content "Let's check in with the scores so far:" :style :header}
   {:type :scores}

   ;; Round 2
   {:type :text :content "Round 2" :style :header}
   {:type :text :content "Next let's learn about the different chatters"}

   ;; Emoji questions
   {:type :text :content "Who's favourite emoji is this?"}
   {:type :text :content "😉" :style :emoji}
   {:type :buttons
    :id "q4-emoji1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q4-emoji1" :answer "Oliver"}

   {:type :text :content "Who's favourite emoji is this?"}
   {:type :text :content "😛" :style :emoji}
   {:type :buttons
    :id "q5-emoji2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q5-emoji2" :answer "Zack"}

   {:type :text :content "Who's favourite emoji is this?"}
   {:type :text :content "💀" :style :emoji}
   {:type :buttons
    :id "q6-emoji3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q6-emoji3" :answer "Retrospectre"}

   {:type :text :content "Who's favourite emoji is this?"}
   {:type :text :content "😂" :style :emoji}
   {:type :buttons
    :id "q7-emoji4"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q7-emoji4" :answer "Jaspreet"}

   ;; Catchphrases
   {:type :text :content "Next, did you know that you have catch phrases?"}
   {:type :text :content "Who's catch phrases are these?"}

   {:type :text :content "yum"}
   {:type :text :content "toil toil toil"}
   {:type :buttons
    :id "q8-phrase1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q8-phrase1" :answer "Jack R" :text "That's right, it was Jack Rowland"}

   {:type :text :content "Who's catch phrases are these?"}
   {:type :text :content "matey"}
   {:type :text :content "super fair"}
   {:type :buttons
    :id "q9-phrase2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q9-phrase2" :answer "Samir" :text "It was Samir!"}

   {:type :text :content "Who's catch phrases are these?"}
   {:type :text :content "wanna play"}
   {:type :text :content "fakes"}
   {:type :buttons
    :id "q10-phrase3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q10-phrase3" :answer "James" :text "Who else but James"}

   {:type :text :content "Now for a hard one:"}
   {:type :text :content "Who's catch phrase is this?"}
   {:type :text :content "Tiananmen Square"}
   {:type :buttons
    :id "q11-phrase4"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q11-phrase4" :answer "Liam" :text "The answer was of course… Liam!"}

   ;; Scores checkpoint
   {:type :text :content "Let's check in with scores now" :style :header}
   {:type :scores}

   ;; Round 3
   {:type :text :content "Round 3" :style :header}
   {:type :text :content "A lot has happened over the years"}
   {:type :text :content "And you can see it marked in the history of our chat"}

   ;; Events questions
   {:type :text :content "What happened on the day with the most messages?"}
   {:type :buttons
    :id "q12-event1"
    :options ["A YuGiOh tournament" "A Wedding" "Gamestop" "The birth of a child"]}
   {:type :reveal :id "q12-event1" :answer "Gamestop"
    :text "The answer was of course: Gamestop in January 2021"}
   {:type :text :content "Why would you expect anything else?"}
   {:type :text :content "There were 1402 messages on that day"}
   {:type :link :content "https://t.me/c/1360175818/289350"}

   {:type :text :content "2021 was a long time ago though, much more exciting things have happened this year"}
   {:type :text :content "What happened on the day with the most messages in 2025?"}
   {:type :buttons
    :id "q13-event2"
    :options ["A YuGiOh Tournament" "A Wedding" "Gamestop (again)" "The birth of a child"]}
   {:type :reveal :id "q13-event2" :answer "A YuGiOh Tournament"
    :text "In February this year 662 messages were sent about a YuGiOh Tournament"}
   {:type :link :content "https://t.me/c/1360175818/492975"}
   {:type :text :content "Never change"}

   {:type :text :content "Ok, next"}
   {:type :text :content "What happened the day the most photos were shared?"}
   {:type :buttons
    :id "q14-event3"
    :options ["Holiday Photos" "A Wedding" "MTG Cards" "The birth of a child"]}
   {:type :reveal :id "q14-event3" :answer "Holiday Photos"
    :text "It was uncomfortably close but Holiday Photos wins"}
   {:type :text :content "This was the trip to Tenerife"}
   {:type :link :content "https://t.me/c/1360175818/367592"}

   {:type :text :content "What conversation had the most participants?"}
   {:type :buttons
    :id "q15-event4"
    :options ["Toil posting" "A Wedding" "Gamestop (for real)" "The birth of a child"]}
   {:type :reveal :id "q15-event4" :answer "The birth of a child"
    :text "This was genuinely a tie between the birth of Leo and Amelia"}
   {:type :link :content "https://t.me/c/1360175818/505946"}
   {:type :link :content "https://t.me/c/1360175818/507465"}
   {:type :text :content "Thank god we've found something normal"}

   ;; Final scores
   {:type :text :content "And our final scores:" :style :header}
   {:type :scores :final true}
   {:type :text :content "Thanks to everyone for participating!"}])



;; >> State

(def default-state
  {:message-index 0           ;; Current message index (how many messages to show)
   :all-answers {}            ;; {question-id -> {client-id -> answer}}
   :scores {}                 ;; {client-id -> score}
   :participants {}           ;; {client-id -> {:name "..."}}
   :my-name nil
   :name-submitted? false
   :is-controller? false})

(defn save-state! []
  (js/localStorage.setItem "chat-quiz-state"
    (js/JSON.stringify (select-keys @state [:my-name :name-submitted? :message-index :all-answers]))))

(defn load-state []
  (when-let [saved (js/localStorage.getItem "chat-quiz-state")]
    (js/JSON.parse saved)))

(def state (atom (merge default-state (load-state))))

(defn reset-state! []
  (js/localStorage.removeItem "chat-quiz-state")
  (reset! state (assoc default-state
                       :is-controller? (:is-controller? @state))))

(defn broadcast-reset! []
  "Broadcast reset signal to all clients (controller only)"
  (when (js/confirm "Reset quiz for all participants?")
    (ably/publish! RESET-CHANNEL "reset" {:timestamp (js/Date.now)})
    (reset-state!)))



;; >> Utilities

(defn debounce [f delay-ms]
  (let [timeout (atom nil)]
    (fn [& args]
      (when-let [t @timeout]
        (js/clearTimeout t))
      (reset! timeout
        (js/setTimeout #(apply f args) delay-ms)))))

(defn hash-string [s]
  "Simple string hash function"
  (reduce (fn [hash char]
            (let [h (+ (bit-shift-left hash 5) (- hash) (.charCodeAt char 0))]
              (bit-and h 0x7FFFFFFF)))
          0
          s))

(defn seeded-random [seed]
  "Simple LCG pseudo-random number generator"
  (let [a 1664525
        c 1013904223
        m (js/Math.pow 2 32)]
    (mod (+ (* a seed) c) m)))

(defn shuffle-with-seed [coll seed]
  "Deterministic shuffle based on seed string"
  (let [initial-seed (hash-string (str seed))
        indexed (map-indexed vector coll)
        ;; Generate a random value for each item
        with-random (loop [items indexed
                           current-seed initial-seed
                           result []]
                      (if (empty? items)
                        result
                        (let [next-seed (seeded-random current-seed)]
                          (recur (rest items)
                                 next-seed
                                 (conj result [(first items) next-seed])))))]
    (->> with-random
         (sort-by second)
         (map (fn [[[_ item] _]] item)))))



;; >> Scoring

(defn calculate-score [client-id]
  "Calculate score for a player based on their answers"
  (let [all-answers (:all-answers @state)]
    (reduce
      (fn [score msg]
        (if (and (= (:type msg) :reveal)
                 (= (get-in all-answers [(:id msg) client-id]) (:answer msg)))
          (inc score)
          score))
      0
      quiz-messages)))

(defn update-all-scores! []
  "Recalculate scores for all participants"
  (let [participants (:participants @state)
        new-scores (reduce
                     (fn [scores [client-id _]]
                       (assoc scores client-id (calculate-score client-id)))
                     {}
                     participants)]
    (swap! state assoc :scores new-scores)))

(defn get-sorted-scores []
  "Get scores sorted by value descending"
  (let [scores (:scores @state)
        participants (:participants @state)]
    (->> scores
         (map (fn [[client-id score]]
                {:client-id client-id
                 :name (get-in participants [client-id :name] "Unknown")
                 :score score}))
         (sort-by :score >))))



;; >> Answer Processing

(defn my-answer [question-id]
  "Get my answer for a question"
  (get-in @state [:all-answers question-id ably/client-id]))

(defn submit-answer! [question-id answer]
  "Submit an answer for a question"
  ;; Update locally immediately for responsiveness
  (swap! state assoc-in [:all-answers question-id ably/client-id] answer)
  (save-state!)
  ;; Also broadcast to others
  (ably/publish! QUIZ-CHANNEL "answer"
    {:client-id ably/client-id
     :question-id question-id
     :answer answer
     :timestamp (js/Date.now)}))

(defn process-answer [data]
  "Process incoming answer from Ably"
  (let [{:keys [client-id question-id answer]} data]
    (swap! state assoc-in [:all-answers question-id client-id] answer)
    (save-state!)
    (update-all-scores!)))



;; >> Controller Functions

(defn set-message-index! [idx]
  "Set the message index and save"
  (swap! state assoc :message-index idx)
  (save-state!))

(defn post-next-message! []
  "Post the next message (controller only)"
  (let [idx (:message-index @state)
        total (count quiz-messages)
        new-idx (inc idx)]
    (when (<= new-idx total)
      (ably/publish! CHANNEL "index" {:index new-idx})
      (set-message-index! new-idx))))

(defn go-back-message! []
  "Go back one message (controller only)"
  (let [idx (:message-index @state)
        new-idx (dec idx)]
    (when (>= new-idx 0)
      (ably/publish! CHANNEL "index" {:index new-idx})
      (set-message-index! new-idx))))

(defn process-message [data]
  "Process incoming index update from Ably"
  (let [{:keys [index]} data]
    (set-message-index! index)))

(defn sync-state! []
  "Sync state to Ably presence"
  (ably/update-presence! CHANNEL {:message-index (:message-index @state)}))



;; >> UI Helpers

(defn is-last-message? [msg-index]
  "Returns true if the given message index is the last visible message"
  (= msg-index (dec (:message-index @state))))



;; >> UI Components

;; Header bar
(defn header []
  (let [participant-count (count (:participants @state))]
    [:div {:class "flex items-center gap-3 p-3 bg-[#1c1c1d] border-b border-gray-800 shrink-0 relative z-10"}
     ;; Title and subtitle
     [:div {:class "flex-1"}
      [:div {:class "text-white font-semibold"} "Chat: Wrapped"]
      [:div {:class "text-gray-400 text-sm"} participant-count " members"]]
     ;; Icon (right side)
     [:div {:class "w-10 h-10 rounded-full bg-green-600 flex items-center justify-center text-white font-bold"}
      "W"]]))

;; Text message
(defn text-message [{:keys [content style]} {:keys [first-in-group? last-in-group?]}]
  (let [sticker-url (when (= style :emoji) (get sticker-images content))]
    [:div {:class (str "flex items-end gap-2 " (if last-in-group? "mb-2" "mb-0.5"))}
     ;; Icon space (only show icon on last message of group)
     [:div {:class "w-8 shrink-0"}
      (when last-in-group?
        [:div {:class "w-8 h-8 rounded-full bg-green-600 flex items-center justify-center text-white text-sm font-bold"}
         "W"])]
     [:div {:class "min-w-0"}
      ;; Sender name (only on first message of group)
      (when first-in-group?
        [:div {:class "text-green-400 text-sm font-medium mb-1 ml-1"} "Wrapped"])
      (if sticker-url
        ;; Show sticker image
        [:img {:src sticker-url
               :class "w-32 h-32 object-contain"}]
        ;; Show text
        [:div {:class (str "rounded-2xl px-4 py-2 inline-block "
                           (case style
                             :header "bg-purple-900/50 text-purple-200 font-bold text-lg"
                             :emoji "bg-transparent text-6xl text-center py-4"
                             :quote "bg-[#242424] text-gray-200 italic"
                             "bg-[#242424] text-white"))}
         content])]]))

;; Link message
(defn link-message [{:keys [content]} {:keys [first-in-group? last-in-group?]}]
  [:div {:class (str "flex items-end gap-2 " (if last-in-group? "mb-2" "mb-0.5"))}
   ;; Icon space
   [:div {:class "w-8 shrink-0"}
    (when last-in-group?
      [:div {:class "w-8 h-8 rounded-full bg-green-600 flex items-center justify-center text-white text-sm font-bold"}
       "W"])]
   [:div
    (when first-in-group?
      [:div {:class "text-green-400 text-sm font-medium mb-1 ml-1"} "Wrapped"])
    [:div {:class "rounded-2xl px-4 py-2 bg-[#242424]"}
     [:a {:href content
          :target "_blank"
          :class "text-blue-400 hover:underline text-sm break-all"}
      content]]]])

;; Name selection grid (user interaction - no Wrapped icon)
(defn name-select [{:keys [id]} msg-index]
  (let [is-controller? (:is-controller? @state)
        my-name (:my-name @state)
        selected? (some? my-name)
        is-current? (is-last-message? msg-index)
        locked? (and selected? (not is-current?))]
    [:div {:class "py-2 pl-10"}
     [:div {:class "grid grid-cols-2 gap-2"}
      (for [name participant-names]
        (let [is-selected? (= my-name name)
              photo-url (get profile-photos name)]
          ^{:key name}
          [:button
           {:class (str "flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all "
                        (if is-selected?
                          "bg-green-600 text-white"
                          (if (or locked? is-controller?)
                            "bg-[#242424] text-gray-400"
                            "bg-[#242424] text-white hover:bg-[#2f2f2f]")))
            :disabled (or locked? is-controller?)
            :on-click #(when-not (or locked? is-controller?)
                         (swap! state assoc :my-name name :name-submitted? true)
                         (save-state!)
                         (ably/enter-presence! CHANNEL {:name name}))}
           ;; Profile photo or initial
           (if photo-url
             [:img {:src photo-url
                    :class "w-8 h-8 rounded-full object-cover"}]
             [:div {:class "w-8 h-8 rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs"}
              (first name)])
           [:span {:class "truncate"} name]]))]
     (when selected?
       [:div {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
        (if is-current?
          [:span "Selected: " my-name " · Tap to change"]
          [:span "Welcome, " my-name "!"])])]))

;; Button grid for answers (user interaction - no Wrapped icon)
(defn button-grid [{:keys [id options labels]} my-answer msg-index]
  (let [is-controller? (:is-controller? @state)
        is-current? (is-last-message? msg-index)
        locked? (not is-current?)
        answer-count (count (get (:all-answers @state) id {}))
        ;; Create pairs of [option, label] and shuffle them
        option-pairs (map-indexed (fn [idx opt] {:option opt :label (or (get labels idx) opt)}) options)
        shuffled-pairs (shuffle-with-seed option-pairs id)]
    [:div {:class "py-2 pl-10"}
     [:div {:class "grid grid-cols-2 gap-2"}
      (for [{:keys [option label]} shuffled-pairs]
        (let [selected? (= my-answer option)
              sticker-url (get sticker-images option)]
          ^{:key option}
          [:button
           {:class (str "rounded-xl text-sm font-medium transition-all "
                        (if sticker-url "p-2 " "px-4 py-3 ")
                        (if selected?
                          "bg-blue-600 text-white"
                          (if (or locked? is-controller?)
                            "bg-[#242424] text-gray-400"
                            "bg-[#242424] text-white hover:bg-[#2f2f2f]")))
            :disabled (or locked? is-controller?)
            :on-click #(when-not (or locked? is-controller?) (submit-answer! id option))}
           (if sticker-url
             [:img {:src sticker-url :class "w-12 h-12 object-contain"}]
             label)]))]
     (when (some? my-answer)
       [:div {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
        (if is-current? "Tap to change" "Answer submitted!")])
     (when (or is-controller? (pos? answer-count))
       (let [participant-count (count (:participants @state))]
         [:div {:class "text-center text-gray-500 text-sm mt-2"}
          answer-count " / " participant-count " answered"]))]))

;; Number input (user interaction - no Wrapped icon)
(defn number-input [{:keys [id placeholder]} my-answer msg-index]
  (let [is-controller? (:is-controller? @state)
        is-current? (is-last-message? msg-index)
        submitted? (some? my-answer)
        locked? (not is-current?)
        answer-count (count (get (:all-answers @state) id {}))]
    [:div {:class "py-2 pl-10"}
     [:input {:type "number"
              :min 0
              :class "w-full px-4 py-3 rounded-xl bg-[#242424] text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-center text-xl"
              :placeholder placeholder
              :disabled (or locked? is-controller?)
              :default-value (or my-answer "")
              :on-change #(when-not (or locked? is-controller?)
                            (let [value (js/parseInt (.. % -target -value))]
                              (when-not (js/isNaN value)
                                (submit-answer! id value))))}]
     (when submitted?
       [:div {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
        (if is-current? "Change your answer above" "Answer submitted!")])
     (when (or is-controller? (pos? answer-count))
       (let [participant-count (count (:participants @state))]
         [:div {:class "text-center text-gray-500 text-sm mt-2"}
          answer-count " / " participant-count " answered"]))]))

;; Reveal answer
(defn reveal-message [{:keys [id answer rankings text]} {:keys [first-in-group? last-in-group?]}]
  (let [my-ans (my-answer id)
        correct? (= my-ans answer)
        has-answer? (some? my-ans)
        sticker-url (get sticker-images answer)
        bg-class (if has-answer?
                   (if correct?
                     "bg-green-900/30 border border-green-700"
                     "bg-red-900/30 border border-red-700")
                   "bg-[#242424] border border-gray-700")
        text-class (if has-answer?
                     (if correct? "text-green-300" "text-red-300")
                     "text-gray-300")
        answer-line (if sticker-url
                      [:div {:class "flex items-center gap-2"}
                       [:span {:class text-class} "The answer was"]
                       [:img {:src sticker-url :class "w-10 h-10 object-contain"}]]
                      [:div {:class (str text-class " font-medium")} "The answer was " [:span {:class "font-bold"} answer] "!"])]
    [:div {:class (str "flex items-end gap-2 " (if last-in-group? "mb-2" "mb-0.5"))}
     [:div {:class "w-8 shrink-0"}
      (when last-in-group?
        [:div {:class "w-8 h-8 rounded-full bg-green-600 flex items-center justify-center text-white text-sm font-bold"} "W"])]
     [:div
      (when first-in-group?
        [:div {:class "text-green-400 text-sm font-medium mb-1 ml-1"} "Wrapped"])
      [:div {:class (str "rounded-2xl px-4 py-3 inline-block " bg-class)}
       (if text
         [:div {:class (str text-class " font-medium")} text]
         answer-line)
       (when (and has-answer? correct?)
         [:div {:class "text-green-400 text-sm mt-2"} "✓ Correct!"])
       (when (and has-answer? (not correct?))
         [:div {:class "text-red-400 text-sm mt-2"} "✗ Wrong"])
       (when rankings
         [:div {:class "text-sm text-gray-300 space-y-2 mt-2"}
          (for [[idx [nm cnt]] (map-indexed vector rankings)]
            (let [emoji (first (str/split nm #" "))
                  ranking-sticker (get sticker-images emoji)]
              ^{:key nm}
              [:div {:class "flex justify-between items-center"}
               [:div {:class "flex items-center gap-2"}
                [:span (str (inc idx) ". ")]
                (if ranking-sticker
                  [:img {:src ranking-sticker :class "w-6 h-6 object-contain"}]
                  [:span nm])]
               [:span {:class "text-gray-400"} cnt]]))])]]]))

;; Scores display
(defn scores-message [{:keys [final]} {:keys [first-in-group? last-in-group?]}]
  (let [sorted-scores (get-sorted-scores)]
    [:div {:class (str "flex items-end gap-2 " (if last-in-group? "mb-2" "mb-0.5"))}
     [:div {:class "w-8 shrink-0"}
      (when last-in-group?
        [:div {:class "w-8 h-8 rounded-full bg-green-600 flex items-center justify-center text-white text-sm font-bold"} "W"])]
     [:div
      (when first-in-group?
        [:div {:class "text-green-400 text-sm font-medium mb-1 ml-1"} "Wrapped"])
      [:div {:class "rounded-2xl px-4 py-3 bg-yellow-900/30 border border-yellow-700"}
       [:div {:class "text-yellow-300 font-bold mb-3"}
        (if final "🏆 Final Scores" "📊 Current Scores")]
       [:div {:class "space-y-2"}
        (for [[idx {:keys [name score]}] (map-indexed vector sorted-scores)]
          (let [medal (case idx 0 "🥇" 1 "🥈" 2 "🥉" nil)
                photo-url (get profile-photos name)
                is-winner? (and final (zero? idx))]
            ^{:key name}
            [:div {:class "flex justify-between items-center"}
             [:div {:class "flex items-center gap-2 text-white"}
              (when medal [:span medal])
              [:div {:class "relative"}
               (when is-winner?
                 [:div {:class "absolute -top-3 -right-1 text-sm rotate-12"} "👑"])
               (if photo-url
                 [:img {:src photo-url :class (str "rounded-full object-cover " (if is-winner? "w-8 h-8" "w-6 h-6"))}]
                 [:div {:class (str "rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs " (if is-winner? "w-8 h-8" "w-6 h-6"))}
                  (first name)])]
              [:span name]]
             [:span {:class "font-bold text-yellow-400"} score]]))
        (when (empty? sorted-scores)
          [:div {:class "text-gray-400 italic"} "No scores yet"])]]]]))

;; Message sender types
(defn message-sender [msg]
  "Returns :wrapped for system messages, :user for user interactions"
  (case (:type msg)
    (:name-select :buttons :input) :user
    :wrapped))

;; Render a single message
(defn render-message [msg msg-index group-info]
  (case (:type msg)
    :text [text-message msg group-info]
    :link [link-message msg group-info]
    :name-select [name-select msg msg-index]
    :buttons [button-grid msg (my-answer (:id msg)) msg-index]
    :input [number-input msg (my-answer (:id msg)) msg-index]
    :reveal [reveal-message msg group-info]
    :scores [scores-message msg group-info]
    nil))

;; Messages area
(defn messages-area []
  (let [idx (:message-index @state)
        messages (vec (take idx quiz-messages))
        msg-count (count messages)]
    [:div {:id "messages-container"
           :class "flex-1 overflow-y-auto p-4 relative z-10"}
     [:div {:class "relative z-10"}
      (for [[i msg] (map-indexed vector messages)]
        (let [sender (message-sender msg)
              prev-sender (when (pos? i) (message-sender (get messages (dec i))))
              next-sender (when (< i (dec msg-count)) (message-sender (get messages (inc i))))
              first-in-group? (or (zero? i) (not= sender prev-sender))
              last-in-group? (or (= i (dec msg-count)) (not= sender next-sender))
              group-info {:first-in-group? first-in-group? :last-in-group? last-in-group?}]
          ^{:key i}
          [render-message msg i group-info]))]]))

;; Controller panel
(defn controller-panel []
  (let [idx (:message-index @state)
        total (count quiz-messages)
        next-msg (get quiz-messages idx)]
    [:div {:class "p-4 bg-[#1c1c1d] border-t border-gray-800 pb-safe shrink-0 relative z-10"}
     [:div {:class "flex items-center justify-between mb-3"}
      [:div {:class "flex items-center gap-3"}
       [:span {:class "text-gray-400 text-sm"} idx " / " total]
       [:button
        {:class "px-3 py-1 bg-red-900 text-red-300 rounded text-sm hover:bg-red-800"
         :on-click broadcast-reset!}
        "Reset"]]
      [:div {:class "flex gap-2"}
       [:button
        {:class "px-4 py-2 bg-gray-600 text-white rounded-lg font-medium hover:bg-gray-500 disabled:bg-gray-700 disabled:text-gray-500"
         :disabled (zero? idx)
         :on-click go-back-message!}
        "Back"]
       [:button
        {:class "px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-600"
         :disabled (>= idx total)
         :on-click post-next-message!}
        "Post"]]]
     ;; Preview next message
     (when next-msg
       [:div {:class "p-3 bg-gray-800/50 rounded-lg border border-gray-700"}
        [:div {:class "text-xs text-gray-500 mb-1"} "Next:"]
        [:div {:class "text-sm text-gray-300 truncate"}
         (case (:type next-msg)
           :text (:content next-msg)
           :buttons (str "Buttons: " (str/join ", " (take 3 (:options next-msg))) "...")
           :input "Number input"
           :reveal (str "Reveal: " (:answer next-msg))
           :scores "Scores"
           :link (:content next-msg)
           "...")]])]))

;; >> Main UI

(defn chat-quiz-ui []
  (let [is-controller? (:is-controller? @state)]
    [:div {:class "h-dvh bg-[#0e0e0e] flex flex-col overflow-hidden relative"}
     ;; Background layer with inverted SVG
     [:div {:class "fixed inset-0 opacity-10 pointer-events-none z-0"
            :style {:background-image "url('/background.svg')"
                    :background-repeat "repeat"
                    :filter "invert(1)"}}]
     [header]
     [messages-area]
     (when is-controller?
       [controller-panel])
     [ui/connection-pill]]))



;; >> Initialization

(defn init! [is-controller?]
  ;; Set controller mode
  (when is-controller?
    (swap! state assoc :is-controller? true))

  ;; Re-render when ably connection status changes
  (add-watch ably/state ::connection
    (fn [_ _ _ _]
      (swap! state identity)))

  ;; Enter presence if name submitted
  (when (:name-submitted? @state)
    (ably/enter-presence! CHANNEL {:name (:my-name @state)}))

  ;; Watch presence for participants
  (ably/on-presence-change! CHANNEL
    (fn []
      (ably/get-presence-members CHANNEL
        (fn [members]
          (let [participants (reduce
                               (fn [acc member]
                                 (let [client-id (.-clientId member)
                                       data (.-data member)]
                                   (assoc acc client-id {:client-id client-id
                                                         :name (:name data)})))
                               {}
                               members)]
            (swap! state assoc :participants participants)
            (update-all-scores!))))))

  ;; Subscribe to messages
  (ably/subscribe! CHANNEL process-message)

  ;; Subscribe to answers
  (ably/subscribe! QUIZ-CHANNEL process-answer)

  ;; Subscribe to reset channel
  (ably/subscribe! RESET-CHANNEL (fn [_] (reset-state!)))

  ;; Auto-scroll to bottom when message index changes
  (add-watch state ::auto-scroll
    (fn [_ _ old new]
      (when (> (:message-index new) (:message-index old))
        ;; Use requestAnimationFrame to ensure DOM has updated
        (js/requestAnimationFrame
          (fn []
            (js/setTimeout
              (fn []
                (when-let [container (js/document.getElementById "messages-container")]
                  (.scrollTo container #js {:top (.-scrollHeight container)
                                            :behavior "smooth"})))
              100)))))))
