(ns talk.chat-quiz
  (:require ["./ably.js" :as ably]
            ["./ui.js" :as ui]
            ["./react_helper.js" :as r :refer [$ useState useEffect useRef]]
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

;; Collage photos
(def collage-photos
  ["/random_images/photo1.jpeg"
   "/random_images/photo2.jpeg"
   "/random_images/photo3.jpeg"
   "/random_images/photo4.jpeg"
   "/random_images/photo5.jpeg"
   "/random_images/photo6.jpeg"])

;; Reactions with final counts
(def reaction-data
  [{:emoji "🔥" :count 476}
   {:emoji "❤" :count 452}
   {:emoji "🤣" :count 160}
   {:emoji "👍" :count 113}
   {:emoji "🤨" :count 53}
   {:emoji "😢" :count 50}
   {:emoji "😁" :count 45}
   {:emoji "💯" :count 26}
   {:emoji "🥰" :count 22}
   {:emoji "🎉" :count 19}])



;; >> Quiz Data - All Messages

(def quiz-messages
  [;; ===== PHASE 2: WRAPPED STATS INTRO =====

   ;; Chat creation - Wrapped group
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Chat was created on August 10th 2018 📆"}
   {:type :text :content "In that time we've had ~514,524 messages"}
   {:type :text :content "190 messages per day!"}
   {:type :message-counter :target 514524 :duration 3000}
   {:type :text :content "With 25,431 in 2025 alone"}
   {:type :message-counter :target 25431 :duration 2000}
   {:type :text :content "On January 6th 2020 a tragedy happened"}
   {:type :text :content "Chat history was purged"}
   {:type :purge-graph :total 514524 :purged 180397}
   {:type :text :content "Archeologists continue to wonder at the history lost"}
   {:type :text :content "These are the earliest messages we have:" :show-avatar true}

   ;; Earliest messages from users
   {:type :user-header :sender "Zack" :batch-with-next true}
   {:type :user-message :sender "Zack" :content "🙂" :style :emoji :show-avatar true}

   {:type :user-header :sender "Oliver" :batch-with-next true}
   {:type :user-message :sender "Oliver" :content "It's been a while tbh" :show-avatar true}

   {:type :user-header :sender "Deleted User" :batch-with-next true}
   {:type :user-message :sender "Deleted User" :content "Oh nice a purge" :show-avatar true}

   {:type :user-header :sender "Oliver" :batch-with-next true}
   {:type :user-message :sender "Oliver" :content "I hope noone has saved anything important in chat" :show-avatar true}

   {:type :user-header :sender "Jack Rowland" :batch-with-next true}
   {:type :user-message :sender "Jack Rowland" :content :trump-image :show-avatar true}

   ;; Back to Wrapped
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Here's a link in case you want to bring back the memories"}
   {:type :link :content "https://t.me/c/1360175818/180399" :show-avatar true}

   ;; Stats we do have
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "In the history we *do* have we've seen:"}
   {:type :text :content "1,866,926 Total words typed"}
   {:type :word-rotation}
   {:type :text :content "12,499 Total stickers sent"}
   {:type :sticker-cloud}
   {:type :text :content "10,199 Total photos sent"}
   {:type :photo-collage}
   {:type :text-with-reactions :content "1,684 Total reactions" :show-avatar true}

   ;; ===== ORIGINAL QUIZ CONTENT =====

   ;; Name selection
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Welcome to Chat: Wrapped! 🎉"}
   {:type :text :content "First, select your name:"}
   {:type :name-select :id "name-select" :show-avatar true}

   ;; Intro
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Now let's see how well you know the Chat with a quiz 📊" :show-avatar true}

   ;; Round 1
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Round 1" :style :header}
   {:type :text :content "Let's start off with some basic stats" :show-avatar true}

   ;; Q1: Most messages
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who has sent the most messages?" :show-avatar true}
   {:type :buttons
    :id "q1-messages"
    :options ["Jack Crowson" "Jack Rowland" "James" "Zack" "Lee" "Oliver"]}
   {:type :reveal
    :id "q1-messages"
    :answer "Jack Crowson"
    :show-avatar true
    :rankings [["Jack Crowson" 74441]
               ["Jack Rowland" 72521]
               ["James" 43574]
               ["Zack" 43102]
               ["Lee" 20940]
               ["Oliver" 18458]]}

   ;; Q2: Most popular sticker
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Which is the most popular sticker?" :show-avatar true}
   {:type :buttons
    :id "q2-sticker"
    :options ["😭🤦" "🤣" "💛" "😅" "🙏" "😭🙌" "🐧" "📞" "😆" "💙"]
    :labels ["Crying facepalm" "ROFL" "Yellow" "Sweat cry" "Hands together"
             "Crying hands" "Penguin" "Telephone" "Omegalol" "Blue"]}
   {:type :reveal
    :id "q2-sticker"
    :answer "💛"
    :show-avatar true
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
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "How many hours was the largest gap in messages?" :show-avatar true}
   {:type :buttons
    :id "q3-gap"
    :options ["4 hours" "22 hours" "45 hours" "83 hours"]}
   {:type :reveal :id "q3-gap" :answer "45 hours"}
   {:type :text :content "Can't even survive the weekend without a cheeky message" :show-avatar true}

   ;; Scores checkpoint
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Let's check in with the scores so far:" :style :header}
   {:type :scores :show-avatar true}

   ;; Round 2
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Round 2" :style :header}
   {:type :text :content "What do you really know about your chat buddies? 🤔"}
   {:type :text :content "Guess who these messages are from:" :show-avatar true}

   ;; Q4-7: Emoji guessing
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who's top emoji is:" :show-avatar true}
   {:type :text :content "😭" :style :emoji}
   {:type :buttons
    :id "q4-emoji1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q4-emoji1" :answer "Oliver" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who's top emoji is:" :show-avatar true}
   {:type :text :content "😂" :style :emoji}
   {:type :buttons
    :id "q5-emoji2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q5-emoji2" :answer "Zack" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who's top emoji is:" :show-avatar true}
   {:type :text :content "🎲" :style :emoji}
   {:type :buttons
    :id "q6-emoji3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q6-emoji3" :answer "Retrospectre" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who's top emoji is:" :show-avatar true}
   {:type :text :content "💀" :style :emoji}
   {:type :buttons
    :id "q7-emoji4"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q7-emoji4" :answer "Jaspreet" :show-avatar true}

   ;; Q8-10: Phrase guessing
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Now guess who says these phrases the most:" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"Haha\"" :style :quote}
   {:type :buttons
    :id "q8-phrase1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q8-phrase1" :answer "Jaspreet" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"Lol\"" :style :quote}
   {:type :buttons
    :id "q9-phrase2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q9-phrase2" :answer "James" :show-avatar true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"Pog\"" :style :quote}
   {:type :buttons
    :id "q10-phrase3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]}
   {:type :reveal :id "q10-phrase3" :answer "Jack C" :show-avatar true}

   ;; Final scores
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "And the final scores are..." :style :header}
   {:type :scores :final true :show-avatar true}

   ;; Outro
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Thanks for playing Chat: Wrapped! 🎊"}
   {:type :text :content "See you next year! 👋" :show-avatar true}])



;; >> State

;; Controller mode determined by URL param - not stored in state
(def is-controller?
  (let [params (js/URLSearchParams. (.-search js/location))]
    (some? (.get params "control"))))

(def default-state
  {:message-index 0
   :all-answers {}
   :scores {}
   :participants {}
   :my-name nil
   :name-submitted? false})

(defn save-state! []
  (js/localStorage.setItem "chat-quiz-state"
    (js/JSON.stringify (select-keys @state [:my-name :name-submitted? :message-index :all-answers]))))

(defn load-state []
  (when-let [saved (js/localStorage.getItem "chat-quiz-state")]
    (js/JSON.parse saved)))

(def state (atom (merge default-state (load-state))))

(defn reset-state! []
  (js/localStorage.removeItem "chat-quiz-state")
  (reset! state default-state))

(defn broadcast-reset! []
  (when (js/confirm "Reset quiz for all participants?")
    (ably/publish! RESET-CHANNEL "reset" {:timestamp (js/Date.now)})
    (reset-state!)))



;; >> Utilities

(defn hash-string [s]
  (reduce (fn [hash char]
            (let [h (+ (bit-shift-left hash 5) (- hash) (.charCodeAt char 0))]
              (bit-and h 0x7FFFFFFF)))
          0
          s))

(defn seeded-random [seed]
  (let [a 1664525
        c 1013904223
        m (js/Math.pow 2 32)]
    (mod (+ (* a seed) c) m)))

(defn shuffle-with-seed [coll seed]
  (let [initial-seed (hash-string (str seed))
        indexed (map-indexed vector coll)
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

;; Pure function - calculates score given answers map
(defn calculate-score-for [all-answers client-id]
  (reduce
    (fn [score msg]
      (if (and (= (:type msg) :reveal)
               (= (get-in all-answers [(:id msg) client-id]) (:answer msg)))
        (inc score)
        score))
    0
    quiz-messages))

;; Pure function - calculates all scores
(defn calculate-all-scores [participants all-answers]
  (reduce
    (fn [scores [client-id _]]
      (assoc scores client-id (calculate-score-for all-answers client-id)))
    {}
    participants))

;; Convenience wrapper that reads from state
(defn calculate-score [client-id]
  (calculate-score-for (:all-answers @state) client-id))

;; Updates scores in state (single swap)
(defn update-all-scores! []
  (swap! state (fn [s]
                 (assoc s :scores (calculate-all-scores (:participants s) (:all-answers s))))))

(defn get-sorted-scores []
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
  (get-in @state [:all-answers question-id ably/client-id]))

(defn submit-answer! [question-id answer]
  (swap! state #(-> %
                    (assoc-in [:all-answers question-id ably/client-id] answer)
                    (assoc :last-update (js/Date.now))))
  (save-state!)
  (ably/publish! QUIZ-CHANNEL "answer"
    {:client-id ably/client-id
     :question-id question-id
     :answer answer
     :timestamp (js/Date.now)}))

(defn process-answer [data]
  (let [{:keys [client-id question-id answer]} data]
    ;; Single swap: update answer and recalculate scores together
    (swap! state (fn [s]
                   (let [new-answers (assoc-in (:all-answers s) [question-id client-id] answer)]
                     (-> s
                         (assoc :all-answers new-answers)
                         (assoc :scores (calculate-all-scores (:participants s) new-answers))))))
    (save-state!)))



;; >> Controller Functions

(defn set-message-index! [idx]
  (swap! state assoc :message-index idx)
  (save-state!))

(defn post-next-message! []
  (let [idx (:message-index @state)
        total (count quiz-messages)
        new-idx (inc idx)]
    (when (<= new-idx total)
      (ably/publish! CHANNEL "index" {:index new-idx})
      (set-message-index! new-idx))))

(defn go-back-message! []
  (let [idx (:message-index @state)
        new-idx (dec idx)]
    (when (>= new-idx 0)
      (ably/publish! CHANNEL "index" {:index new-idx})
      (set-message-index! new-idx))))

(defn process-message [data]
  (let [{:keys [index]} data]
    (set-message-index! index)))



;; >> UI Components

;; Helper for avatar visibility
(defn avatar-class [show-avatar?]
  (str "w-8 h-8 rounded-full bg-green-600 flex items-center justify-center text-white text-sm font-bold"
       (when-not show-avatar? " invisible")))

;; Header bar
(defn header []
  (let [participant-count (count (:participants @state))]
    ($ "div" {:class "flex items-center gap-3 p-3 bg-[#1c1c1d] border-b border-gray-800 shrink-0 relative z-10"}
       ($ "div" {:class "flex-1"}
          ($ "div" {:class "text-white font-semibold"} "Chat: Wrapped")
          ($ "div" {:class "text-gray-400 text-sm"} participant-count " members"))
       ($ "div" {:class "w-10 h-10 rounded-full bg-green-600 flex items-center justify-center text-white font-bold"}
          "W"))))

;; Text message
(defn text-message [{:keys [content style show-avatar?]}]
  (let [sticker-url (when (= style :emoji) (get sticker-images content))]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {:class "min-w-0"}
          (if sticker-url
            ($ "img" {:src sticker-url :class "w-32 h-32 object-contain"})
            ($ "div" {:class (str "rounded-2xl px-4 py-2 inline-block "
                                  (case style
                                    :header "bg-purple-900/50 text-purple-200 font-bold text-lg"
                                    :emoji "bg-transparent text-6xl text-center py-4"
                                    :quote "bg-[#242424] text-gray-200 italic"
                                    "bg-[#242424] text-white"))}
               content))))))

;; Link message
(defn link-message [{:keys [content show-avatar?]}]
  ($ "div" {:class "flex items-end gap-2 mb-0.5"}
     ($ "div" {:class "w-8 shrink-0"}
        ($ "div" {:class (avatar-class show-avatar?)} "W"))
     ($ "div" {}
        ($ "div" {:class "rounded-2xl px-4 py-2 bg-[#242424]"}
           ($ "a" {:href content :target "_blank" :class "text-blue-400 hover:underline text-sm break-all"}
              content)))))

;; User colors
(def user-colors
  {"Zack" "text-blue-400"
   "Oliver" "text-purple-400"
   "Deleted User" "text-gray-500"
   "Jack Rowland" "text-orange-400"})

;; User message
(defn user-message [{:keys [sender content style show-avatar?]}]
  (let [photo-url (get profile-photos sender)
        is-image? (= content :trump-image)]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          (if photo-url
            ($ "img" {:src photo-url
                      :class (str "w-8 h-8 rounded-full object-cover" (when-not show-avatar? " invisible"))})
            ($ "div" {:class (str "w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center text-white text-sm font-bold"
                                  (when-not show-avatar? " invisible"))}
               (first sender))))
       ($ "div" {:class "min-w-0"}
          (if is-image?
            ($ "img" {:src "/trump_dabbing.jpeg" :class "rounded-xl max-w-64 max-h-64 object-cover"})
            ($ "div" {:class (str "rounded-2xl px-4 py-2 bg-[#242424] text-white inline-block "
                                  (when (= style :emoji) "text-6xl text-center"))}
               content))))))

;; Message counter with animated badge - uses useEffect for animation
(defn message-counter [{:keys [target duration show-avatar? is-last?]}]
  (let [badge-ref (useRef nil)
        [displayed set-displayed!] (useState (if is-last? 0 target))]

    (useEffect
      (fn []
        (when is-last?
          (let [start-time (js/Date.now)
                animate (fn animate []
                          (let [elapsed (- (js/Date.now) start-time)
                                progress (min 1 (/ elapsed duration))
                                eased (- 1 (js/Math.pow (- 1 progress) 3))
                                current (js/Math.floor (* eased target))]
                            (set-displayed! current)
                            (when (< progress 1)
                              (js/requestAnimationFrame animate))))]
            (animate)))
        js/undefined)
      #js [is-last?])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl p-4 bg-[#242424] inline-flex items-center justify-center"}
             ($ "div" {:class "relative inline-block"}
                ($ "img" {:src "/telegram_logo.png" :class "w-24 h-24 object-contain"})
                ($ "div" {:class "absolute -top-2 -right-2 bg-red-500 text-white rounded-full min-w-8 h-8 flex items-center justify-center text-sm font-bold px-2"
                          :ref badge-ref}
                   (.toLocaleString displayed))))))))

;; Purge graph - declarative with CSS transitions
(defn purge-graph [{:keys [total purged show-avatar? is-last?]}]
  (let [purge-percent (* 100 (/ purged total))
        remaining-percent (- 100 purge-percent)
        [animated? set-animated!] (useState (not is-last?))]

    (useEffect
      (fn []
        (when is-last?
          (js/setTimeout #(set-animated! true) 100))
        js/undefined)
      #js [is-last?])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl p-4 bg-[#242424] w-64"}
             ($ "div" {:class "text-xs text-gray-400 mb-2"} "Message History")
             ($ "div" {:class "h-48 bg-gray-700 rounded-lg overflow-hidden relative flex flex-col"}
                ($ "div" {:class "bg-blue-500 w-full transition-all duration-1000 ease-out"
                          :style {:height (if animated? (str remaining-percent "%") "100%")}})
                ($ "div" {:class "bg-red-500 w-full absolute bottom-0 transition-all duration-1000 ease-out"
                          :style {:height (if animated? (str purge-percent "%") "0%")}}))
             ($ "div" {:class "flex justify-between text-xs mt-2"}
                ($ "div" {:class "flex items-center gap-1"}
                   ($ "div" {:class "w-3 h-3 bg-blue-500 rounded"})
                   ($ "span" {:class "text-gray-300"} "Remaining"))
                ($ "div" {:class "flex items-center gap-1"}
                   ($ "div" {:class "w-3 h-3 bg-red-500 rounded"})
                   ($ "span" {:class "text-gray-300"} "Purged: " (.toLocaleString purged)))))))))

;; Word rotation animation
(def lorem-words
  ["the" "chat" "is" "absolutely" "bonkers" "mate" "did" "you" "see" "that"
   "game" "last" "night" "was" "insane" "honestly" "cannot" "believe" "it"
   "who" "wants" "to" "play" "some" "games" "later" "yum" "toil" "toil"
   "super" "fair" "wanna" "come" "over" "nice" "one" "lads" "brilliant"
   "actually" "hilarious" "send" "it" "please" "no" "way" "literally"
   "dying" "this" "is" "peak" "comedy" "hello" "everyone" "what" "are"
   "we" "thinking" "about" "getting" "for" "dinner" "tonight" "pizza"
   "sounds" "good" "to" "me" "lets" "do" "it" "gamestop" "to" "the" "moon"])

(defn word-rotation [{:keys [show-avatar?]}]
  (let [[word-idx set-word-idx!] (useState 0)
        words-to-show (take 20 (drop word-idx (cycle lorem-words)))
        display-text (str/join " " words-to-show)]

    (useEffect
      (fn []
        (let [interval-id (js/setInterval #(set-word-idx! inc) 150)]
          (fn [] (js/clearInterval interval-id))))
      #js [])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl px-4 py-3 bg-[#242424] w-72 h-16 overflow-hidden"}
             ($ "div" {:class "text-white text-sm leading-relaxed font-mono"}
                display-text))))))

;; Sticker cloud - stickers appearing with animation
(def sticker-positions
  [{:x 10 :y 10 :scale 0.8 :delay 0}
   {:x 60 :y 5 :scale 1.0 :delay 200}
   {:x 35 :y 40 :scale 0.9 :delay 400}
   {:x 5 :y 60 :scale 0.7 :delay 600}
   {:x 70 :y 45 :scale 0.85 :delay 800}
   {:x 40 :y 70 :scale 0.75 :delay 1000}
   {:x 15 :y 35 :scale 0.65 :delay 1200}
   {:x 65 :y 70 :scale 0.9 :delay 1400}
   {:x 50 :y 20 :scale 0.7 :delay 1600}
   {:x 25 :y 80 :scale 0.8 :delay 1800}])

(defn sticker-cloud [{:keys [show-avatar? is-last?]}]
  (let [sticker-list (vec (vals sticker-images))
        [visible-count set-visible-count!] (useState (if is-last? 0 (count sticker-positions)))]

    (useEffect
      (fn []
        (when is-last?
          (doseq [[idx pos] (map-indexed vector sticker-positions)]
            (js/setTimeout #(set-visible-count! (fn [c] (max c (inc idx)))) (:delay pos))))
        js/undefined)
      #js [is-last?])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl bg-[#242424] w-72 h-48 relative overflow-hidden"}
             (.map (to-array (map-indexed vector sticker-positions))
                   (fn [[idx pos]]
                     (let [sticker-url (nth sticker-list (mod idx (count sticker-list)))
                           visible? (< idx visible-count)]
                       ($ "img" {:key idx
                                 :src sticker-url
                                 :class "absolute transition-transform duration-500"
                                 :style {:left (str (:x pos) "%")
                                         :top (str (:y pos) "%")
                                         :width (str (* 48 (:scale pos)) "px")
                                         :height (str (* 48 (:scale pos)) "px")
                                         :transform (if visible? "scale(1)" "scale(0)")
                                         :transition-timing-function "cubic-bezier(0.175, 0.885, 0.32, 1.275)"}})))))))))

;; Photo collage
(defn photo-collage [{:keys [show-avatar?]}]
  ($ "div" {:class "flex items-end gap-2 mb-0.5"}
     ($ "div" {:class "w-8 shrink-0"}
        ($ "div" {:class (avatar-class show-avatar?)} "W"))
     ($ "div" {:class "flex-1 min-w-0"}
        ($ "div" {:class "rounded-2xl bg-[#242424] p-2"}
           ($ "div" {:class "grid grid-cols-3 gap-1"}
              (.map (to-array (map-indexed vector collage-photos))
                    (fn [[idx photo-url]]
                      ($ "img" {:key idx :src photo-url :class "w-full aspect-square object-cover rounded"}))))))))

;; Reaction ticker
(defn reaction-ticker [{:keys [show-avatar? is-last?]}]
  (let [duration 3000
        [progress set-progress!] (useState (if is-last? 0 1))]

    (useEffect
      (fn []
        (when is-last?
          (let [start-time (js/Date.now)
                animate (fn animate []
                          (let [elapsed (- (js/Date.now) start-time)
                                p (min 1 (/ elapsed duration))
                                eased (- 1 (js/Math.pow (- 1 p) 3))]
                            (set-progress! eased)
                            (when (< p 1)
                              (js/requestAnimationFrame animate))))]
            (animate)))
        js/undefined)
      #js [is-last?])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl px-4 py-3 bg-[#242424] inline-block"}
             ($ "div" {:class "flex flex-wrap gap-2 max-w-72"}
                (.map (to-array reaction-data)
                      (fn [{:keys [emoji count]}]
                        ($ "div" {:key emoji :class "flex items-center gap-1 bg-[#3a3a3a] rounded-full px-2 py-1"}
                           ($ "span" {:class "text-base"} emoji)
                           ($ "span" {:class "text-sm text-white font-medium"}
                              (js/Math.floor (* progress count))))))))))))

;; Text with reactions
(defn text-with-reactions [{:keys [content show-avatar? is-last?]}]
  (let [duration 3000
        [progress set-progress!] (useState (if is-last? 0 1))]

    (useEffect
      (fn []
        (when is-last?
          (let [start-time (js/Date.now)
                animate (fn animate []
                          (let [elapsed (- (js/Date.now) start-time)
                                p (min 1 (/ elapsed duration))
                                eased (- 1 (js/Math.pow (- 1 p) 3))]
                            (set-progress! eased)
                            (when (< p 1)
                              (js/requestAnimationFrame animate))))]
            (animate)))
        js/undefined)
      #js [is-last?])

    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {:class "min-w-0"}
          ($ "div" {:class "rounded-2xl px-4 py-3 bg-[#242424] inline-block"}
             ($ "div" {:class "text-white mb-2"} content)
             ($ "div" {:class "flex flex-wrap gap-2"}
                (.map (to-array reaction-data)
                      (fn [{:keys [emoji count]}]
                        ($ "div" {:key emoji :class "flex items-center gap-1 bg-[#3a3a3a] rounded-full px-2 py-1"}
                           ($ "span" {:class "text-base"} emoji)
                           ($ "span" {:class "text-sm text-white font-medium"}
                              (js/Math.floor (* progress count))))))))))))

;; Name selection grid
(defn name-select [{:keys [id is-last?]}]
  (let [my-name (:my-name @state)
        selected? (some? my-name)
        is-current? is-last?
        locked? (and selected? (not is-current?))]
    ($ "div" {:class "py-2 pl-10"}
       ($ "div" {:class "grid grid-cols-2 gap-2"}
          (.map (to-array participant-names)
                (fn [name]
                  (let [is-selected? (= my-name name)
                        photo-url (get profile-photos name)]
                    ($ "button"
                       {:key name
                        :class (str "flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all "
                                    (if is-selected?
                                      "bg-green-600 text-white"
                                      (if (or locked? is-controller?)
                                        "bg-[#242424] text-gray-400"
                                        "bg-[#242424] text-white hover:bg-[#2f2f2f]")))
                        :disabled (or locked? is-controller?)
                        :on-click (fn []
                                    (when-not (or locked? is-controller?)
                                      (swap! state assoc :my-name name :name-submitted? true)
                                      (save-state!)
                                      (ably/enter-presence! CHANNEL {:name name})))}
                       (if photo-url
                         ($ "img" {:src photo-url :class "w-8 h-8 rounded-full object-cover"})
                         ($ "div" {:class "w-8 h-8 rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs"}
                            (first name)))
                       ($ "span" {:class "truncate"} name))))))
       (when selected?
         ($ "div" {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
            (if is-current?
              ($ "span" {} "Selected: " my-name " · Tap to change")
              ($ "span" {} "Welcome, " my-name "!")))))))

;; Button grid for answers
(defn button-grid [{:keys [id options labels is-last? current-answer]}]
  (let [is-current? is-last?
        locked? (not is-current?)
        ;; Use passed current-answer or fetch fresh
        current-answer (or current-answer (my-answer id))
        answer-count (count (get (:all-answers @state) id {}))
        option-pairs (map-indexed (fn [idx opt] {:option opt :label (or (get labels idx) opt)}) options)
        shuffled-pairs (shuffle-with-seed option-pairs id)]
    ($ "div" {:class "py-2 pl-10"}
       ($ "div" {:class "grid grid-cols-2 gap-2"}
          (.map (to-array shuffled-pairs)
                (fn [{:keys [option label]}]
                  (let [selected? (= current-answer option)
                        sticker-url (get sticker-images option)]
                    ($ "button"
                       {:key option
                        :class (str "rounded-xl text-sm font-medium transition-all "
                                    (if sticker-url "p-2 " "px-4 py-3 ")
                                    (if selected?
                                      "bg-blue-600 text-white"
                                      (if (or locked? is-controller?)
                                        "bg-[#242424] text-gray-400"
                                        "bg-[#242424] text-white hover:bg-[#2f2f2f]")))
                        :disabled (or locked? is-controller?)
                        :on-click (fn [] (when-not (or locked? is-controller?) (submit-answer! id option)))}
                       (if sticker-url
                         ($ "img" {:src sticker-url :class "w-12 h-12 object-contain"})
                         label))))))
       (when (some? current-answer)
         ($ "div" {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
            (if is-current? "Tap to change" "Answer submitted!")))
       (when (or is-controller? (pos? answer-count))
         (let [participant-count (count (:participants @state))]
           ($ "div" {:class "text-center text-gray-500 text-sm mt-2"}
              answer-count " / " participant-count " answered"))))))

;; Number input
(defn number-input [{:keys [id placeholder is-last? current-answer]}]
  (let [is-current? is-last?
        input-answer (or current-answer (my-answer id))
        submitted? (some? input-answer)
        locked? (not is-current?)
        answer-count (count (get (:all-answers @state) id {}))]
    ($ "div" {:class "py-2 pl-10"}
       ($ "input" {:type "number"
                   :min 0
                   :class "w-full px-4 py-3 rounded-xl bg-[#242424] text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-center text-xl"
                   :placeholder placeholder
                   :disabled (or locked? is-controller?)
                   :default-value (or input-answer "")
                   :on-change (fn [e]
                                (when-not (or locked? is-controller?)
                                  (let [value (js/parseInt (.. e -target -value))]
                                    (when-not (js/isNaN value)
                                      (submit-answer! id value)))))})
       (when submitted?
         ($ "div" {:class (str "text-center text-sm mt-2 " (if is-current? "text-blue-400" "text-green-400"))}
            (if is-current? "Change your answer above" "Answer submitted!")))
       (when (or is-controller? (pos? answer-count))
         (let [participant-count (count (:participants @state))]
           ($ "div" {:class "text-center text-gray-500 text-sm mt-2"}
              answer-count " / " participant-count " answered"))))))

;; Reveal answer
(defn reveal-message [{:keys [id answer rankings text show-avatar?]}]
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
                     "text-gray-300")]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class (str "rounded-2xl px-4 py-3 inline-block " bg-class)}
             (if text
               ($ "div" {:class (str text-class " font-medium")} text)
               (if sticker-url
                 ($ "div" {:class "flex items-center gap-2"}
                    ($ "span" {:class text-class} "The answer was")
                    ($ "img" {:src sticker-url :class "w-10 h-10 object-contain"}))
                 ($ "div" {:class (str text-class " font-medium")}
                    "The answer was " ($ "span" {:class "font-bold"} answer) "!")))
             (when (and has-answer? correct?)
               ($ "div" {:class "text-green-400 text-sm mt-2"} "✓ Correct!"))
             (when (and has-answer? (not correct?))
               ($ "div" {:class "text-red-400 text-sm mt-2"} "✗ Wrong"))
             (when rankings
               ($ "div" {:class "text-sm text-gray-300 space-y-2 mt-2"}
                  (.map (to-array (map-indexed vector rankings))
                        (fn [[idx [nm cnt]]]
                          (let [emoji (first (str/split nm #" "))
                                ranking-sticker (get sticker-images emoji)]
                            ($ "div" {:key nm :class "flex justify-between items-center"}
                               ($ "div" {:class "flex items-center gap-2"}
                                  ($ "span" {} (str (inc idx) ". "))
                                  (if ranking-sticker
                                    ($ "img" {:src ranking-sticker :class "w-6 h-6 object-contain"})
                                    ($ "span" {} nm)))
                               ($ "span" {:class "text-gray-400"} cnt))))))))))))

;; Scores display
(defn scores-message [{:keys [final show-avatar?]}]
  (let [sorted-scores (get-sorted-scores)]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl px-4 py-3 bg-yellow-900/30 border border-yellow-700"}
             ($ "div" {:class "text-yellow-300 font-bold mb-3"}
                (if final "🏆 Final Scores" "📊 Current Scores"))
             ($ "div" {:class "space-y-2"}
                (.map (to-array (map-indexed vector sorted-scores))
                      (fn [[idx {:keys [name score]}]]
                        (let [medal (case idx 0 "🥇" 1 "🥈" 2 "🥉" nil)
                              photo-url (get profile-photos name)
                              is-winner? (and final (zero? idx))]
                          ($ "div" {:key name :class "flex justify-between items-center"}
                             ($ "div" {:class "flex items-center gap-2 text-white"}
                                (when medal ($ "span" {} medal))
                                ($ "div" {:class "relative"}
                                   (when is-winner?
                                     ($ "div" {:class "absolute -top-3 -right-1 text-sm rotate-12"} "👑"))
                                   (if photo-url
                                     ($ "img" {:src photo-url
                                               :class (str "rounded-full object-cover " (if is-winner? "w-8 h-8" "w-6 h-6"))})
                                     ($ "div" {:class (str "rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs "
                                                           (if is-winner? "w-8 h-8" "w-6 h-6"))}
                                        (first name))))
                                ($ "span" {} name))
                             ($ "span" {:class "font-bold text-yellow-400"} score)))))
                (when (empty? sorted-scores)
                  ($ "div" {:class "text-gray-400 italic"} "No scores yet"))))))))

;; Sender header
(defn sender-header [{:keys [sender show-avatar?]}]
  ($ "div" {:class "flex items-end gap-2 mb-0.5 mt-3"}
     ($ "div" {:class "w-8 shrink-0"}
        ($ "div" {:class (avatar-class show-avatar?)} "W"))
     ($ "div" {:class "text-green-400 text-sm font-medium"} sender)))

;; User header
(defn user-header [{:keys [sender show-avatar?]}]
  (let [photo-url (get profile-photos sender)
        name-color (get user-colors sender "text-cyan-400")]
    ($ "div" {:class "flex items-end gap-2 mb-0.5 mt-3"}
       ($ "div" {:class "w-8 shrink-0"}
          (if photo-url
            ($ "img" {:src photo-url
                      :class (str "w-8 h-8 rounded-full object-cover" (when-not show-avatar? " invisible"))})
            ($ "div" {:class (str "w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center text-white text-sm font-bold"
                                  (when-not show-avatar? " invisible"))}
               (first sender))))
       ($ "div" {:class (str name-color " text-sm font-medium")} sender))))

;; Render a single message
(defn render-message [msg msg-index group-info]
  ;; Merge msg and group-info into single props object for React
  (let [props (merge msg group-info)]
    (case (:type msg)
      :sender-header ($ sender-header props)
      :user-header ($ user-header props)
      :text ($ text-message props)
      :link ($ link-message props)
      :name-select ($ name-select props)
      :buttons ($ button-grid (merge props {:current-answer (my-answer (:id msg))}))
      :input ($ number-input (merge props {:current-answer (my-answer (:id msg))}))
      :reveal ($ reveal-message props)
      :scores ($ scores-message props)
      :user-message ($ user-message props)
      :message-counter ($ message-counter props)
      :purge-graph ($ purge-graph props)
      :word-rotation ($ word-rotation props)
      :sticker-cloud ($ sticker-cloud props)
      :photo-collage ($ photo-collage props)
      :reaction-ticker ($ reaction-ticker props)
      :text-with-reactions ($ text-with-reactions props)
      nil)))

;; Get messages to display based on message-index
(defn get-visible-messages [target-index]
  (loop [msgs quiz-messages
         step 0
         result []]
    (if (or (empty? msgs) (>= step target-index))
      result
      (let [msg (first msgs)
            batched? (:batch-with-next msg)]
        (recur (rest msgs)
               (if batched? step (inc step))
               (conj result msg))))))

;; Messages area
(defn messages-area []
  (let [idx (:message-index @state)
        messages (get-visible-messages idx)
        msg-count (count messages)]
    ($ "div" {:id "messages-container" :class "flex-1 overflow-y-auto p-4 relative z-10"}
       ($ "div" {:class "relative z-10"}
          (.map (to-array (map-indexed vector messages))
                (fn [[i msg]]
                  (let [is-last? (= i (dec msg-count))
                        show-avatar? (or is-last? (:show-avatar msg))]
                    ($ "div" {:key i :id (when is-last? "last-message")}
                       (render-message msg i {:show-avatar? show-avatar? :is-last? is-last?})))))))))

;; Count non-batched messages
(def total-steps
  (count (remove :batch-with-next quiz-messages)))

;; Get the next message to be shown
(defn get-next-step-message [current-index]
  (let [visible (get-visible-messages current-index)
        visible-count (count visible)]
    (get quiz-messages visible-count)))

;; Controller panel
(defn controller-panel []
  (let [idx (:message-index @state)
        total total-steps
        next-msg (get-next-step-message idx)]
    ($ "div" {:class "p-4 bg-[#1c1c1d] border-t border-gray-800 pb-safe shrink-0 relative z-10"}
       ($ "div" {:class "flex items-center justify-between mb-3"}
          ($ "div" {:class "flex items-center gap-3"}
             ($ "span" {:class "text-gray-400 text-sm"} idx " / " total)
             ($ "button" {:class "px-3 py-1 bg-red-900 text-red-300 rounded text-sm hover:bg-red-800"
                          :on-click broadcast-reset!}
                "Reset"))
          ($ "div" {:class "flex gap-2"}
             ($ "button" {:class "px-4 py-2 bg-gray-600 text-white rounded-lg font-medium hover:bg-gray-500 disabled:bg-gray-700 disabled:text-gray-500"
                          :disabled (zero? idx)
                          :on-click go-back-message!}
                "Back")
             ($ "button" {:class "px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-600"
                          :disabled (>= idx total)
                          :on-click post-next-message!}
                "Post")))
       (when next-msg
         ($ "div" {:class "p-3 bg-gray-800/50 rounded-lg border border-gray-700"}
            ($ "div" {:class "text-xs text-gray-500 mb-1"} "Next:")
            ($ "div" {:class "text-sm text-gray-300 truncate"}
               (case (:type next-msg)
                 :sender-header (str "Header: " (:sender next-msg))
                 :user-header (str "User: " (:sender next-msg))
                 :text (:content next-msg)
                 :buttons (str "Buttons: " (str/join ", " (take 3 (:options next-msg))) "...")
                 :input "Number input"
                 :reveal (str "Reveal: " (:answer next-msg))
                 :scores "Scores"
                 :link (:content next-msg)
                 :name-select "Name selection"
                 :message-counter "Message counter animation"
                 :purge-graph "Purge graph"
                 :word-rotation "Word rotation"
                 :sticker-cloud "Sticker cloud"
                 :photo-collage "Photo collage"
                 :reaction-ticker "Reaction ticker"
                 :text-with-reactions (str (:content next-msg) " + reactions")
                 :user-message (str (:sender next-msg) ": " (:content next-msg))
                 "...")))))))

;; Main UI
(defn chat-quiz-ui []
  ($ "div" {:class "h-dvh bg-[#0e0e0e] flex flex-col overflow-hidden relative"}
     ($ "div" {:class "fixed inset-0 opacity-10 pointer-events-none z-0"
               :style {:background-image "url('/background.svg')"
                       :background-repeat "repeat"
                       :filter "invert(1)"}})
     ($ header)
     ($ messages-area)
     (when is-controller?
       ($ controller-panel))
     ($ ui/connection-pill)))



;; >> Initialization

(defn init! []
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
            ;; Single swap: update participants and recalculate scores together
            (swap! state (fn [s]
                           (-> s
                               (assoc :participants participants)
                               (assoc :scores (calculate-all-scores participants (:all-answers s)))))))))))

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
        (js/requestAnimationFrame
          (fn []
            (js/setTimeout
              (fn []
                (when-let [last-msg (js/document.getElementById "last-message")]
                  (.scrollIntoView last-msg #js {:behavior "smooth" :block "end"})))
              100)))))))
