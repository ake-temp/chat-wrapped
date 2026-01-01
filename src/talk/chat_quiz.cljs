(ns talk.chat-quiz
  (:require ["./react_helper.js" :as r :refer [$ useState useEffect useRef]]
            [clojure.string :as str]))



;; >> Configuration

;; Detect base path from URL at runtime (strip trailing slash)
(def base-path
  (let [pathname (.-pathname js/location)]
    (if (str/ends-with? pathname "/")
      (subs pathname 0 (dec (count pathname)))
      pathname)))

(def participant-names
  ["Jack Crowson" "Jack Rowland" "James" "Zack" "Lee" "Oliver"
   "Retrospectre" "Samir" "Jaspreet" "Liam" "Georgia" "Mariana"
   "Jack McMillan" "Alice" "Jess" "Aidan"])

;; Real quiz answers from live session
;; Client ID -> Name mapping (update names once identified):
;; Player 1 (11 pts): 1767213434253-781069
;; Player 2 (11 pts): 1767213444853-804932
;; Player 3 (11 pts): 1767213422567-842519
;; Player 4 (11 pts): 1767213449507-526339
;; Player 5 (10 pts): 1767213447890-794511
;; Player 6 (10 pts): 1767213422814-439619
;; Player 7 (9 pts): 1767213452264-130133
;; Player 8 (9 pts): 1767213423557-952555
;; Player 9 (8 pts): 1767213562049-414570
;; Player 10 (6 pts): 1767213466512-641167
(def static-quiz-answers
  {"q1-messages" {"1767213452264-130133" "Jack Crowson"
                  "1767213562049-414570" "Jack Crowson"
                  "1767213434253-781069" "Jack Crowson"
                  "1767213423557-952555" "Jack Rowland"
                  "1767213444853-804932" "Jack Crowson"
                  "1767213466512-641167" "Jack Crowson"
                  "1767213422567-842519" "Jack Crowson"
                  "1767213447890-794511" "Jack Crowson"
                  "1767213449507-526339" "Jack Crowson"
                  "1767213422814-439619" "Jack Crowson"}
   "q2-sticker" {"1767213444853-804932" "💛"
                 "1767213562049-414570" "💛"
                 "1767213422567-842519" "💛"
                 "1767213423557-952555" "💛"
                 "1767213447890-794511" "💛"
                 "1767213466512-641167" "💛"
                 "1767213452264-130133" "💛"
                 "1767213422814-439619" "💛"
                 "1767213449507-526339" "💛"
                 "1767213434253-781069" "💛"}
   "q3-gap" {"1767213422567-842519" "45 hours"
             "1767213423557-952555" "45 hours"
             "1767213466512-641167" "22 hours"
             "1767213452264-130133" "22 hours"
             "1767213434253-781069" "45 hours"
             "1767213444853-804932" "45 hours"
             "1767213449507-526339" "45 hours"
             "1767213447890-794511" "45 hours"
             "1767213562049-414570" "45 hours"
             "1767213422814-439619" "22 hours"}
   "q4-emoji1" {"1767213422814-439619" "Zack"
                "1767213423557-952555" "Zack"
                "1767213422567-842519" "Retrospectre"
                "1767213466512-641167" "Zack"
                "1767213444853-804932" "Lee"
                "1767213562049-414570" "Samir"
                "1767213447890-794511" "Samir"
                "1767213449507-526339" "Oliver"
                "1767213434253-781069" "Jaspreet"
                "1767213452264-130133" "Liam"}
   "q5-emoji2" {"1767213444853-804932" "Lee"
                "1767213452264-130133" "Zack"
                "1767213422814-439619" "Zack"
                "1767213449507-526339" "Lee"
                "1767213423557-952555" "Lee"
                "1767213434253-781069" "Zack"
                "1767213466512-641167" "Zack"
                "1767213562049-414570" "Lee"
                "1767213447890-794511" "Oliver"
                "1767213422567-842519" "Jaspreet"}
   "q6-emoji3" {"1767213434253-781069" "Jack C"
                "1767213444853-804932" "Retrospectre"
                "1767213423557-952555" "Jack C"
                "1767213422814-439619" "Retrospectre"
                "1767213422567-842519" "Retrospectre"
                "1767213447890-794511" "Jack C"
                "1767213466512-641167" "Jack C"
                "1767213449507-526339" "Jack C"
                "1767213562049-414570" "Liam"
                "1767213452264-130133" "Liam"}
   "q7-emoji4" {"1767213444853-804932" "Jaspreet"
                "1767213562049-414570" "Jaspreet"
                "1767213423557-952555" "Jaspreet"
                "1767213466512-641167" "Jack R"
                "1767213434253-781069" "Jaspreet"
                "1767213452264-130133" "Jaspreet"
                "1767213449507-526339" "Jaspreet"
                "1767213447890-794511" "Jaspreet"
                "1767213422814-439619" "Jaspreet"
                "1767213422567-842519" "Lee"}
   "q8-phrase1" {"1767213444853-804932" "Jack R"
                 "1767213422567-842519" "Jack R"
                 "1767213449507-526339" "Jack R"
                 "1767213422814-439619" "Jack R"
                 "1767213452264-130133" "Jack R"
                 "1767213466512-641167" "Jack C"
                 "1767213447890-794511" "Jack R"
                 "1767213423557-952555" "Jack R"
                 "1767213562049-414570" "Jack R"
                 "1767213434253-781069" "Jack R"}
   "q9-phrase2" {"1767213444853-804932" "Samir"
                 "1767213423557-952555" "Samir"
                 "1767213422567-842519" "Samir"
                 "1767213449507-526339" "Samir"
                 "1767213434253-781069" "Samir"
                 "1767213422814-439619" "Samir"
                 "1767213466512-641167" "Oliver"
                 "1767213562049-414570" "Samir"
                 "1767213452264-130133" "Samir"
                 "1767213447890-794511" "Samir"}
   "q10-phrase3" {"1767213422567-842519" "James"
                  "1767213423557-952555" "James"
                  "1767213466512-641167" "Retrospectre"
                  "1767213562049-414570" "Jack C"
                  "1767213434253-781069" "James"
                  "1767213444853-804932" "Retrospectre"
                  "1767213452264-130133" "Jack C"
                  "1767213449507-526339" "James"
                  "1767213447890-794511" "James"
                  "1767213422814-439619" "James"}
   "q11-phrase4" {"1767213452264-130133" "Retrospectre"
                  "1767213466512-641167" "Retrospectre"
                  "1767213447890-794511" "Jack C"
                  "1767213562049-414570" "Liam"
                  "1767213422567-842519" "Lee"
                  "1767213423557-952555" "Jack C"
                  "1767213444853-804932" "Liam"
                  "1767213449507-526339" "Jack C"
                  "1767213434253-781069" "Jack C"
                  "1767213422814-439619" "Lee"}
   "q12-event1" {"1767213466512-641167" "Gamestop"
                 "1767213423557-952555" "Gamestop"
                 "1767213422567-842519" "Gamestop"
                 "1767213449507-526339" "Gamestop"
                 "1767213562049-414570" "A YuGiOh tournament"
                 "1767213422814-439619" "A YuGiOh tournament"
                 "1767213452264-130133" "Gamestop"
                 "1767213447890-794511" "Gamestop"
                 "1767213444853-804932" "Gamestop"
                 "1767213434253-781069" "A YuGiOh tournament"}
   "q13-event2" {"1767213466512-641167" "A YuGiOh Tournament"
                 "1767213422567-842519" "A YuGiOh Tournament"
                 "1767213422814-439619" "A YuGiOh Tournament"
                 "1767213423557-952555" "A YuGiOh Tournament"
                 "1767213449507-526339" "A YuGiOh Tournament"
                 "1767213434253-781069" "A YuGiOh Tournament"
                 "1767213444853-804932" "A YuGiOh Tournament"
                 "1767213452264-130133" "A YuGiOh Tournament"
                 "1767213562049-414570" "The birth of a child"
                 "1767213447890-794511" "A YuGiOh Tournament"}
   "q14-event3" {"1767213447890-794511" "MTG Cards"
                 "1767213422814-439619" "MTG Cards"
                 "1767213562049-414570" "MTG Cards"
                 "1767213449507-526339" "MTG Cards"
                 "1767213422567-842519" "Holiday Photos"
                 "1767213434253-781069" "Holiday Photos"
                 "1767213452264-130133" "MTG Cards"
                 "1767213423557-952555" "MTG Cards"
                 "1767213444853-804932" "MTG Cards"
                 "1767213466512-641167" "MTG Cards"}
   "q15-event4" {"1767213447890-794511" "The birth of a child"
                 "1767213423557-952555" "The birth of a child"
                 "1767213422567-842519" "The birth of a child"
                 "1767213449507-526339" "The birth of a child"
                 "1767213422814-439619" "The birth of a child"
                 "1767213434253-781069" "The birth of a child"
                 "1767213466512-641167" "The birth of a child"
                 "1767213452264-130133" "The birth of a child"
                 "1767213562049-414570" "The birth of a child"
                 "1767213444853-804932" "The birth of a child"}})

;; Wrapped profile data from vibe.md analysis
(def wrapped-profiles
  {"Jack Crowson"
   {:vibe "The Chat Engine"
    :vibe-desc "Ranked #1 in message volume with 74,441 messages."
    :total-messages 74441
    :messages-2025 8201
    :avg-length 5.1
    :emoji-count 1150
    :fav-emoji "👀"
    :fav-emoji-count 146
    :sticker-count 6821
    :fav-sticker "🤣"
    :fav-sticker-count 1128
    :morning-pct 8.6
    :late-night-pct 4.1
    :questions 4187
    :links 643
    :reactions-received 192
    :sarcasm 0.75
    :traits ["Prolific" "Brief" "Sticker Fan" "Starter"]}

   "Jack Rowland"
   {:vibe "Conversation Catalyst"
    :vibe-desc "Starts 34.7% of conversations - always getting things going."
    :total-messages 72521
    :messages-2025 3903
    :avg-length 5.6
    :emoji-count 418
    :fav-emoji "👀"
    :fav-emoji-count 32
    :sticker-count 4300
    :fav-sticker "💛"
    :fav-sticker-count 689
    :morning-pct 11.6
    :late-night-pct 3.1
    :questions 2795
    :links 955
    :reactions-received 261
    :sarcasm 0.72
    :traits ["Prolific" "Sticker Fan" "Starter"]}

   "James"
   {:vibe "Volume King"
    :vibe-desc "Ranked #3 in message volume with 43,574 messages. Often online late (11% late night)."
    :total-messages 43574
    :messages-2025 3929
    :avg-length 6.1
    :emoji-count 313
    :fav-emoji "✅"
    :fav-emoji-count 74
    :sticker-count 483
    :fav-sticker "🤔"
    :fav-sticker-count 82
    :morning-pct 3.2
    :late-night-pct 10.9
    :questions 2600
    :links 710
    :reactions-received 193
    :sarcasm 0.76
    :traits ["Prolific" "Night Owl" "Starter"]}

   "Zack"
   {:vibe "Prolific Poster"
    :vibe-desc "Ranked #4 in message volume with 43,102 messages."
    :total-messages 43102
    :messages-2025 1512
    :avg-length 6.4
    :emoji-count 816
    :fav-emoji "😛"
    :fav-emoji-count 180
    :sticker-count 323
    :fav-sticker "💛"
    :fav-sticker-count 81
    :morning-pct 3.5
    :late-night-pct 7.5
    :questions 3270
    :links 671
    :reactions-received 180
    :sarcasm 0.77
    :traits ["Prolific" "Starter"]}

   "Lee"
   {:vibe "Message Machine"
    :vibe-desc "Ranked #5 in message volume with 20,940 messages."
    :total-messages 20940
    :messages-2025 908
    :avg-length 7.1
    :emoji-count 461
    :fav-emoji "😭"
    :fav-emoji-count 47
    :sticker-count 156
    :fav-sticker "💛"
    :fav-sticker-count 28
    :morning-pct 5.5
    :late-night-pct 8.9
    :questions 2377
    :links 187
    :reactions-received 125
    :sarcasm 0.73
    :traits ["Balanced"]}

   "Oliver"
   {:vibe "The Flood"
    :vibe-desc "Ranked #6 in message volume with 18,458 messages. Highly sarcastic (score: 0.82)."
    :total-messages 18458
    :messages-2025 451
    :avg-length 6.3
    :emoji-count 1407
    :fav-emoji "😉"
    :fav-emoji-count 205
    :sticker-count 154
    :fav-sticker "🤔"
    :fav-sticker-count 31
    :morning-pct 6.0
    :late-night-pct 5.8
    :questions 2642
    :links 223
    :reactions-received 111
    :sarcasm 0.82
    :traits ["Balanced"]}

   "Retrospectre"
   {:vibe "The Wordsmith"
    :vibe-desc "Ranked #3 for message length with 7.5 words per message on average. Often online late (10% late night)."
    :total-messages 9773
    :messages-2025 3126
    :avg-length 7.5
    :emoji-count 475
    :fav-emoji "💀"
    :fav-emoji-count 93
    :sticker-count 44
    :fav-sticker "💛"
    :fav-sticker-count 6
    :morning-pct 6.8
    :late-night-pct 10.2
    :questions 906
    :links 78
    :reactions-received 71
    :sarcasm 0.76
    :traits ["Wordsmith" "Night Owl"]}

   "Samir"
   {:vibe "Sticker Royalty"
    :vibe-desc "Top sticker user with 1.1% of messages being stickers."
    :total-messages 8327
    :messages-2025 1881
    :avg-length 6.3
    :emoji-count 7
    :fav-emoji "☆"
    :fav-emoji-count 2
    :sticker-count 94
    :fav-sticker "💛"
    :fav-sticker-count 11
    :morning-pct 5.9
    :late-night-pct 3.9
    :questions 404
    :links 40
    :reactions-received 133
    :sarcasm 0.74
    :traits ["Sticker Fan"]}

   "Jaspreet"
   {:vibe "Essay Writer"
    :vibe-desc "Ranked #4 for message length with 7.4 words per message on average. Highly sarcastic (score: 0.85)."
    :total-messages 6316
    :messages-2025 667
    :avg-length 7.4
    :emoji-count 871
    :fav-emoji "😂"
    :fav-emoji-count 174
    :sticker-count 57
    :fav-sticker "💛"
    :fav-sticker-count 9
    :morning-pct 6.9
    :late-night-pct 7.8
    :questions 1192
    :links 83
    :reactions-received 140
    :sarcasm 0.85
    :traits ["Wordsmith" "Expressive"]}

   "Liam"
   {:vibe "Visual Communicator"
    :vibe-desc "Top sticker user with 1.0% of messages being stickers."
    :total-messages 4322
    :messages-2025 496
    :avg-length 6.5
    :emoji-count 12
    :fav-emoji "🙂"
    :fav-emoji-count 6
    :sticker-count 45
    :fav-sticker "💛"
    :fav-sticker-count 10
    :morning-pct 5.0
    :late-night-pct 6.9
    :questions 346
    :links 50
    :reactions-received 107
    :sarcasm 0.77
    :traits ["Balanced"]}

   "Georgia"
   {:vibe "Thoughtful One"
    :vibe-desc "Ranked #2 for message length with 8.2 words per message on average. Heavy emoji user (0.50/msg). Highly sarcastic (score: 0.91)."
    :total-messages 337
    :messages-2025 166
    :avg-length 8.2
    :emoji-count 168
    :fav-emoji "😊"
    :fav-emoji-count 17
    :sticker-count 0
    :fav-sticker nil
    :fav-sticker-count 0
    :morning-pct 8.6
    :late-night-pct 3.6
    :questions 113
    :links 9
    :reactions-received 52
    :sarcasm 0.91
    :traits ["Wordsmith" "Expressive" "Sarcastic"]}

   "Mariana"
   {:vibe "Emoji Royalty"
    :vibe-desc "Ranked #3 in emoji usage with 0.33 emoji per message. Highly sarcastic (score: 0.89)."
    :total-messages 108
    :messages-2025 86
    :avg-length 7.2
    :emoji-count 36
    :fav-emoji "😂"
    :fav-emoji-count 11
    :sticker-count 0
    :fav-sticker nil
    :fav-sticker-count 0
    :morning-pct 7.4
    :late-night-pct 1.9
    :questions 26
    :links 0
    :reactions-received 37
    :sarcasm 0.89
    :traits ["Expressive"]}

   "Jack McMillan"
   {:vibe "The Sticker"
    :vibe-desc "Top sticker user with 13.0% of messages being stickers."
    :total-messages 100
    :messages-2025 31
    :avg-length 4.4
    :emoji-count 1
    :fav-emoji "😎"
    :fav-emoji-count 1
    :sticker-count 13
    :fav-sticker "🟠"
    :fav-sticker-count 4
    :morning-pct 0.0
    :late-night-pct 1.0
    :questions 4
    :links 0
    :reactions-received 8
    :sarcasm 0.79
    :traits ["Brief" "Sticker Fan"]}

   "Alice"
   {:vibe "Sarcasm Champion"
    :vibe-desc "Ranked #2 in sarcasm score (0.93) - certified roaster."
    :total-messages 73
    :messages-2025 61
    :avg-length 7.4
    :emoji-count 6
    :fav-emoji "🎉"
    :fav-emoji-count 2
    :sticker-count 0
    :fav-sticker nil
    :fav-sticker-count 0
    :morning-pct 1.4
    :late-night-pct 4.1
    :questions 2
    :links 1
    :reactions-received 36
    :sarcasm 0.93
    :traits ["Selective" "Sarcastic"]}

   "Jess"
   {:vibe "Expressive One"
    :vibe-desc "Ranked #1 in emoji usage with 0.92 emoji per message. Highly sarcastic (score: 0.90)."
    :total-messages 26
    :messages-2025 11
    :avg-length 5.2
    :emoji-count 24
    :fav-emoji "🥳"
    :fav-emoji-count 3
    :sticker-count 0
    :fav-sticker nil
    :fav-sticker-count 0
    :morning-pct 7.7
    :late-night-pct 0.0
    :questions 4
    :links 0
    :reactions-received 10
    :sarcasm 0.90
    :traits ["Selective" "Brief" "Expressive" "Sarcastic"]}

   "Aidan"
   {:vibe "The Verbose"
    :vibe-desc "Ranked #1 for message length with 20.5 words per message on average. Never uses emoji. Highly sarcastic (score: 0.99)."
    :total-messages 2
    :messages-2025 2
    :avg-length 20.5
    :emoji-count 0
    :fav-emoji nil
    :fav-emoji-count 0
    :sticker-count 0
    :fav-sticker nil
    :fav-sticker-count 0
    :morning-pct 50.0
    :late-night-pct 0.0
    :questions 0
    :links 0
    :reactions-received 1
    :sarcasm 0.99
    :traits ["Selective" "Wordsmith" "Zero Emoji" "Sarcastic"]}})

;; Profile photo mappings (name -> filename)
(def profile-photos
  {"Jack Crowson" (str base-path "/profile_photos/Jack_Crowson.jpg")
   "James" (str base-path "/profile_photos/James.jpg")
   "Zack" (str base-path "/profile_photos/Zack_Pollard.jpg")
   "Lee" (str base-path "/profile_photos/Lee_Bain.jpg")
   "Oliver" (str base-path "/profile_photos/Oliver_Marshall.jpg")
   "Retrospectre" (str base-path "/profile_photos/Retrospectre.jpg")
   "Jaspreet" (str base-path "/profile_photos/Jaspreet_Crowson.jpg")
   "Liam" (str base-path "/profile_photos/Liam_Moloney.jpg")
   "Georgia" (str base-path "/profile_photos/Georgia.jpg")
   "Mariana" (str base-path "/profile_photos/Mariana.jpg")
   "Jess" (str base-path "/profile_photos/Jess_Brookfield.jpg")})

;; Sticker mappings (emoji -> filename)
(def sticker-images
  {"💛" (str base-path "/stickers/yellow.webp")
   "😭🤦" (str base-path "/stickers/crying_facepalm.webp")
   "🤣" (str base-path "/stickers/rofl.webp")
   "🙏" (str base-path "/stickers/hands_together.webp")
   "😅" (str base-path "/stickers/sweat_cry.webp")
   "😆" (str base-path "/stickers/omegalol.webp")
   "😭🙌" (str base-path "/stickers/crying_hands_in_air.webp")
   "💙" (str base-path "/stickers/blue.webp")
   "📞" (str base-path "/stickers/telephone.webp")
   "🐧" (str base-path "/stickers/penguin.webp")})

;; Collage photos (random selection)
(def collage-photos
  [(str base-path "/random_images/photo1.jpeg")
   (str base-path "/random_images/photo2.jpeg")
   (str base-path "/random_images/photo3.jpeg")
   (str base-path "/random_images/photo4.jpeg")
   (str base-path "/random_images/photo5.jpeg")
   (str base-path "/random_images/photo6.jpeg")])

;; Holiday photos (Tenerife trip)
(def holiday-photos
  [(str base-path "/holiday_photos/holiday1.jpeg")
   (str base-path "/holiday_photos/holiday2.jpeg")
   (str base-path "/holiday_photos/holiday3.jpeg")
   (str base-path "/holiday_photos/holiday4.jpeg")
   (str base-path "/holiday_photos/holiday5.jpeg")
   (str base-path "/holiday_photos/holiday6.jpeg")])

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
  [;; ===== WRAPPED STATS INTRO =====

   ;; Chat creation - Wrapped group
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Chat was created on August 10th 2018 📆"}
   {:type :text :content "In that time we've sent over 514,000 messages"}
   {:type :text :content "190 messages per day!" :batch-with-next true}
   {:type :message-counter :target 514524 :duration 3000}
   {:type :text :content "With 25,431 in 2025 alone" :batch-with-next true}
   {:type :message-counter :target 25431 :duration 2000}
   {:type :text :content "On January 1st 2020 a tragedy happened"}
   {:type :text :content "Chat history was purged"}
   {:type :purge-graph :total 514524 :purged 180397}
   {:type :text :content "Archeologists continue to wonder at the history lost"}
   {:type :text :content "These are the earliest messages we have:" :show-avatar true}

   ;; Earliest messages from users
   {:type :user-header :sender "Zack" :batch-with-next true}
   {:type :emoji :content "🙂" :sender "Zack" :show-avatar true}

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
   {:type :text :content "Here's a link in case you want to bring back the memories:"}
   {:type :link :content "https://t.me/c/1360175818/180399" :show-avatar true}

   ;; Stats we do have
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "In the history we *do* have we've seen:"}
   {:type :text :content "1,866,926 Total words typed" :batch-with-next true}
   {:type :word-rotation}
   {:type :text :content "12,499 Total stickers sent" :batch-with-next true}
   {:type :sticker-cloud}
   {:type :text :content "10,199 Total photos sent" :batch-with-next true}
   {:type :photo-collage}
   {:type :text-with-reactions :content "1,684 Total reactions" :show-avatar true}

   ;; ===== NAME SELECTION =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Now presenting, your personal Wrapped Profile!"}
   {:type :text :content "First, who are you?"}
   {:type :name-select :id "name-select" :show-avatar true}

   ;; ===== WRAPPED PROFILES =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :wrapped-intro :show-avatar true}
   {:type :wrapped-profiles :show-avatar true :show-stats false}

   ;; Quiz intro
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Seems a little empty..."}
   {:type :text :content "Let's see if we can fill it up a bit"}
   {:type :text :content "And see how well you know the Chat with a quiz 📊" :show-avatar true}

   ;; ===== ROUND 1 =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Round 1" :text-style :header}
   {:type :text :content "Let's start off with some basic stats" :show-avatar true}

   ;; Q1: Most messages
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who has sent the most messages?" :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q1-messages"
    :options ["Jack Crowson" "Jack Rowland" "James" "Zack" "Lee" "Oliver"]
    :show-photos true}
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
   {:type :text :content "Surprised? Anyone?" :show-avatar true}

   ;; Q2: Most popular sticker
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Which is the most popular sticker?" :show-avatar true :batch-with-next true}
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
   {:type :text :content "A classic" :show-avatar true}

   ;; Q3: Longest gap
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "How many hours was the largest gap in messages?" :show-avatar true :batch-with-next true}
   {:type :buttons :id "q3-gap" :options ["4 hours" "22 hours" "45 hours" "83 hours"]}
   {:type :reveal :id "q3-gap" :answer "45 hours" :text "The longest gap was 45 hours!"}
   {:type :text :content "Can't even survive the weekend without a cheeky message" :show-avatar true}

   ;; Scores checkpoint
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Let's check in with the scores so far:" :show-avatar true}
   {:type :scores :questions ["q1-messages" "q2-sticker" "q3-gap"]}

   ;; ===== ROUND 2 =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Round 2" :text-style :header}
   {:type :text :content "Next let's learn about the different chatters" :show-avatar true}

   ;; Q4: Emoji 😉
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Whose favourite emoji is this?" :show-avatar true :batch-with-next true}
   {:type :emoji :content "😉" :batch-with-next true}
   {:type :buttons
    :id "q4-emoji1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q4-emoji1" :answer "Oliver" :text "The answer was Oliver!" :show-avatar true}

   ;; Q5: Emoji 😛
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "What about this one?" :show-avatar true :batch-with-next true}
   {:type :emoji :content "😛" :batch-with-next true}
   {:type :buttons
    :id "q5-emoji2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q5-emoji2" :answer "Zack" :text "The answer was Zack!" :show-avatar true}

   ;; Q6: Emoji 💀
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Who uses this the most?" :show-avatar true :batch-with-next true}
   {:type :emoji :content "💀" :batch-with-next true}
   {:type :buttons
    :id "q6-emoji3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q6-emoji3" :answer "Retrospectre" :text "The answer was Retrospectre!" :show-avatar true}

   ;; Q7: Emoji 😂
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "And finally..." :show-avatar true :batch-with-next true}
   {:type :emoji :content "😂" :batch-with-next true}
   {:type :buttons
    :id "q7-emoji4"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q7-emoji4" :answer "Jaspreet" :text "The answer was Jaspreet!" :show-avatar true}

   ;; Catch phrases intro
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Everyone has their signature phrases..."}
   {:type :text :content "But can you recognise them?" :show-avatar true}

   ;; Q8: Catch phrase - yum, toil toil toil
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"yum\", \"toil toil toil\"" :text-style :quote :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q8-phrase1"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q8-phrase1" :answer "Jack R" :text "That's right, it was Jack Rowland!" :show-avatar true}

   ;; Q9: Catch phrase - matey, super fair
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"matey\", \"super fair\"" :text-style :quote :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q9-phrase2"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q9-phrase2" :answer "Samir" :text "It was Samir!" :show-avatar true}

   ;; Q10: Catch phrase - wanna play, fakes
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "\"wanna play\", \"fakes\"" :text-style :quote :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q10-phrase3"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q10-phrase3" :answer "James" :text "Who else but James!" :show-avatar true}

   ;; Q11: Hard one - Tiananmen Square
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Now for a hard one:"}
   {:type :text :content "\"Tiananmen Square\"" :text-style :quote :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q11-phrase4"
    :options ["Oliver" "Zack" "Retrospectre" "Jaspreet" "James" "Jack C" "Jack R" "Samir" "Lee" "Liam"]
    :show-photos true}
   {:type :reveal :id "q11-phrase4" :answer "Liam" :text "The answer was of course… Liam!" :show-avatar true}

   ;; Scores checkpoint
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Let's check in with scores now:" :show-avatar true}
   {:type :scores :questions ["q1-messages" "q2-sticker" "q3-gap"
                              "q4-emoji1" "q5-emoji2" "q6-emoji3" "q7-emoji4"
                              "q8-phrase1" "q9-phrase2" "q10-phrase3" "q11-phrase4"]}

   ;; ===== ROUND 3 =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Round 3" :text-style :header}
   {:type :text :content "A lot has happened over the years"}
   {:type :text :content "And you can see it marked in the history of our chat" :show-avatar true}

   ;; Q12: Most messages day
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "What happened on the day with the most messages?" :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q12-event1"
    :options ["A YuGiOh tournament" "A Wedding" "Gamestop" "The birth of a child"]}
   {:type :reveal :id "q12-event1" :answer "Gamestop" :text "The answer was of course: Gamestop in January 2021" :show-avatar true}
   {:type :text :content "Why would you expect anything else?"}
   {:type :text :content "There were 1402 messages on that day"}
   {:type :link :content "https://t.me/c/1360175818/289350" :show-avatar true}

   ;; Q13: Most messages 2025
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "2021 was a long time ago though, much more exciting things have happened this year"}
   {:type :text :content "What happened on the day with the most messages in 2025?" :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q13-event2"
    :options ["A YuGiOh Tournament" "A Wedding" "Gamestop (again)" "The birth of a child"]}
   {:type :reveal :id "q13-event2" :answer "A YuGiOh Tournament" :text "In February this year 662 messages were sent about a YuGiOh Tournament" :show-avatar true}
   {:type :link :content "https://t.me/c/1360175818/492975"}
   {:type :text :content "Never change" :show-avatar true}

   ;; Q14: Most photos
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Ok, next"}
   {:type :text :content "What happened the day the most photos were shared?" :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q14-event3"
    :options ["Holiday Photos" "A Wedding" "MTG Cards" "The birth of a child"]}
   {:type :reveal :id "q14-event3" :answer "Holiday Photos" :text "It was uncomfortably close but Holiday Photos wins" :show-avatar true}
   {:type :text :content "This was the trip to Tenerife" :batch-with-next true}
   {:type :holiday-collage}
   {:type :text :content "What an exciting trip!"}
   {:type :link :content "https://t.me/c/1360175818/367499" :show-avatar true}

   ;; Q15: Most participants
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "What conversations had the most participants?" :show-avatar true :batch-with-next true}
   {:type :buttons
    :id "q15-event4"
    :options ["Toil posting" "A Wedding" "Gamestop (for real)" "The birth of a child"]}
   {:type :reveal :id "q15-event4" :answer "The birth of a child" :text "This was genuinely a tie between the birth of Leo and Amelia" :show-avatar true}
   {:type :link :content "https://t.me/c/1360175818/505946"}
   {:type :link :content "https://t.me/c/1360175818/507465"}
   {:type :text :content "Thank god we've found something normal" :show-avatar true}

   ;; ===== FINAL SCORES =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "And our final scores:" :text-style :header :show-avatar true}
   {:type :scores :final true}

   ;; ===== FINAL WRAPPED PROFILES =====

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Now check out your updated Wrapped profile with your quiz ranking!" :show-avatar true}
   {:type :wrapped-profiles :show-avatar true :show-stats true}

   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Want to see everyone else's profiles?" :show-avatar true}
   {:type :wrapped-gallery :show-avatar true}

   ;; Outro
   {:type :sender-header :sender "Wrapped" :batch-with-next true}
   {:type :text :content "Thanks to everyone for participating!" :show-avatar true}])



;; >> State

;; Static participants - real client IDs mapped to names
;; Tell me which names to swap once you see the UI
(def static-participants
  (into {}
    (map (fn [[client-id name]]
           [client-id {:name name :client-id client-id}])
         {"1767213434253-781069" "Jack Crowson"   ;; 11 pts
          "1767213444853-804932" "Jack Rowland"   ;; 11 pts
          "1767213422567-842519" "James"          ;; 11 pts
          "1767213449507-526339" "Zack"           ;; 11 pts
          "1767213447890-794511" "Jess"            ;; 10 pts
          "1767213422814-439619" "Jaspreet"       ;; 10 pts
          "1767213452264-130133" "Liam"           ;; 9 pts
          "1767213423557-952555" "Alice"          ;; 9 pts
          "1767213562049-414570" "Mariana"        ;; 8 pts
          "1767213466512-641167" "Georgia"        ;; 6 pts
          })))

(def default-state
  {:message-index 0
   :all-answers static-quiz-answers  ;; Use static answers instead of empty
   :scores {}
   :participants static-participants  ;; Use static participants
   :my-name nil
   :name-submitted? false})

(defn save-state! []
  (js/localStorage.setItem "chat-quiz-state"
    (js/JSON.stringify (select-keys @state [:my-name :name-submitted? :message-index]))))

(defn load-state []
  (when-let [saved (js/localStorage.getItem "chat-quiz-state")]
    (js/JSON.parse saved)))

(def state (atom (merge default-state (load-state))))

(defn reset-state! []
  (js/localStorage.removeItem "chat-quiz-state")
  (reset! state default-state))



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

;; Map of question-id -> correct answer for quick lookup
(def question-answers
  (->> quiz-messages
       (filter #(= (:type %) :reveal))
       (map (fn [msg] [(:id msg) (:answer msg)]))
       (into {})))

;; Calculate score for a participant, optionally limited to specific questions
(defn calculate-score-for [all-answers client-id question-ids]
  (let [questions (or question-ids (keys question-answers))]
    (count (filter (fn [qid]
                     (= (get-in all-answers [qid client-id])
                        (get question-answers qid)))
                   questions))))

;; Calculate all scores, optionally limited to specific questions
(defn calculate-all-scores [participants all-answers question-ids]
  (reduce
    (fn [scores [client-id _]]
      (assoc scores client-id (calculate-score-for all-answers client-id question-ids)))
    {}
    participants))

;; Convenience wrapper that reads from state
(defn calculate-score [client-id]
  (calculate-score-for (:all-answers @state) client-id nil))

;; Updates scores in state (single swap) - uses all questions
(defn update-all-scores! []
  (swap! state (fn [s]
                 (assoc s :scores (calculate-all-scores (:participants s) (:all-answers s) nil)))))

;; Helper to add ranks with ties handling
(defn add-ranks [sorted-entries]
  (loop [remaining sorted-entries
         current-rank 1
         prev-score nil
         same-rank-count 0
         result []]
    (if (empty? remaining)
      result
      (let [entry (first remaining)
            score (:score entry)
            new-rank (if (= score prev-score)
                       current-rank
                       (+ current-rank same-rank-count))
            new-same-count (if (= score prev-score)
                             (inc same-rank-count)
                             1)]
        (recur (rest remaining)
               new-rank
               score
               new-same-count
               (conj result (assoc entry :rank new-rank)))))))

;; Get sorted scores, optionally limited to specific question IDs
(defn get-sorted-scores
  ([] (get-sorted-scores nil))
  ([question-ids]
   (let [participants (:participants @state)
         all-answers (:all-answers @state)
         scores (calculate-all-scores participants all-answers question-ids)
         sorted (->> scores
                     (map (fn [[client-id score]]
                            {:client-id client-id
                             :name (get-in participants [client-id :name] "Unknown")
                             :score score}))
                     (sort-by :score >))]
     (add-ranks sorted))))



;; >> Answer Processing

;; Get answer for current selected user
(defn my-answer [question-id]
  (when-let [my-name (:my-name @state)]
    ;; Find the client-id for the selected name
    (when-let [my-client-id (some (fn [[cid data]]
                                     (when (= (:name data) my-name) cid))
                                   (:participants @state))]
      (get-in @state [:all-answers question-id my-client-id]))))



;; >> Navigation Functions

(defn set-message-index! [idx]
  (swap! state assoc :message-index idx)
  (save-state!))

(defn post-next-message! []
  (let [idx (:message-index @state)
        total (count quiz-messages)
        new-idx (inc idx)]
    (when (<= new-idx total)
      (set-message-index! new-idx))))

(defn go-back-message! []
  (let [idx (:message-index @state)
        new-idx (dec idx)]
    (when (>= new-idx 0)
      (set-message-index! new-idx))))

(defn skip-to-top! []
  (set-message-index! 0))

(defn skip-to-bottom! []
  (set-message-index! total-steps))



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
(defn text-message [{:keys [content text-style show-avatar?]}]
  (let [sticker-url (when (= text-style "emoji") (get sticker-images content))
        style-class (cond
                      (= text-style "header") "bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold text-lg"
                      (= text-style "emoji") "bg-transparent text-6xl text-center py-4"
                      (= text-style "quote") "bg-[#242424] text-gray-200 italic"
                      :else "bg-[#242424] text-white")]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {:class "min-w-0"}
          (if sticker-url
            ($ "img" {:src sticker-url :class "w-32 h-32 object-contain"})
            ($ "div" {:class (str "rounded-2xl px-4 py-2 inline-block " style-class)}
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


;; Standalone large emoji (no message box)
;; If sender is provided, shows user's avatar, otherwise shows Wrapped avatar
(defn emoji-message [{:keys [content sender show-avatar?]}]
  (let [photo-url (when sender (get profile-photos sender))]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          (if sender
            (if photo-url
              ($ "img" {:src photo-url
                        :class (str "w-8 h-8 rounded-full object-cover" (when-not show-avatar? " invisible"))})
              ($ "div" {:class (str "w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center text-white text-sm font-bold"
                                    (when-not show-avatar? " invisible"))}
                 (first sender)))
            ($ "div" {:class (avatar-class show-avatar?)} "W")))
       ($ "div" {:class "text-7xl py-2"}
          content))))

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
            ($ "img" {:src (str base-path "/trump_dabbing.jpeg") :class "rounded-xl max-w-64 max-h-64 object-cover"})
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
                ($ "img" {:src (str base-path "/telegram_logo.png") :class "w-24 h-24 object-contain"})
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

;; Holiday photo collage
(defn holiday-collage [{:keys [show-avatar?]}]
  ($ "div" {:class "flex items-end gap-2 mb-0.5"}
     ($ "div" {:class "w-8 shrink-0"}
        ($ "div" {:class (avatar-class show-avatar?)} "W"))
     ($ "div" {:class "flex-1 min-w-0"}
        ($ "div" {:class "rounded-2xl bg-[#242424] p-2"}
           ($ "div" {:class "grid grid-cols-3 gap-1"}
              (.map (to-array (map-indexed vector holiday-photos))
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
                                      (if locked?
                                        "bg-[#242424] text-gray-400"
                                        "bg-[#242424] text-white hover:bg-[#2f2f2f]")))
                        :disabled locked?
                        :on-click (fn []
                                    (when-not locked?
                                      (swap! state assoc :my-name name :name-submitted? true)
                                      (save-state!)))}
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

;; Button grid for answers (display-only, no interaction)
(defn button-grid [{:keys [id options labels is-last? current-answer show-photos]}]
  (let [;; Use passed current-answer or fetch fresh
        current-answer (or current-answer (my-answer id))
        answer-count (count (get (:all-answers @state) id {}))
        option-pairs (map-indexed (fn [idx opt] {:option opt :label (or (get labels idx) opt)}) options)
        shuffled-pairs (shuffle-with-seed option-pairs id)]
    ($ "div" {:class "py-2 pl-10"}
       ($ "div" {:class "grid grid-cols-2 gap-2"}
          (.map (to-array shuffled-pairs)
                (fn [{:keys [option label]}]
                  (let [selected? (= current-answer option)
                        sticker-url (get sticker-images option)
                        photo-url (when show-photos (get profile-photos label))]
                    ($ "div"
                       {:key option
                        :class (str "rounded-xl text-sm font-medium "
                                    (if sticker-url "p-2 " "px-4 py-3 ")
                                    (if selected?
                                      "bg-blue-600 text-white"
                                      "bg-[#242424] text-gray-400"))}
                       (cond
                         sticker-url
                         ($ "img" {:src sticker-url :class "w-12 h-12 object-contain"})

                         show-photos
                         ($ "div" {:class "flex items-center gap-2"}
                            (if photo-url
                              ($ "img" {:src photo-url :class "w-6 h-6 rounded-full object-cover"})
                              ($ "div" {:class "w-6 h-6 rounded-full bg-[#3a3a3a] flex items-center justify-center text-xs"}
                                 (first label)))
                            ($ "span" {} label))

                         :else label))))))
       (when (some? current-answer)
         ($ "div" {:class "text-center text-sm mt-2 text-blue-400"}
            "Selected answer"))
       (let [participant-count (count (:participants @state))]
         ($ "div" {:class "text-center text-gray-500 text-sm mt-2"}
            answer-count " / " participant-count " answered")))))

;; Number input (display-only)
(defn number-input [{:keys [id placeholder is-last? current-answer]}]
  (let [input-answer (or current-answer (my-answer id))
        submitted? (some? input-answer)
        answer-count (count (get (:all-answers @state) id {}))]
    ($ "div" {:class "py-2 pl-10"}
       ($ "input" {:type "number"
                   :min 0
                   :class "w-full px-4 py-3 rounded-xl bg-[#242424] text-white placeholder-gray-400 text-center text-xl"
                   :placeholder placeholder
                   :disabled true
                   :value (or input-answer "")
                   :read-only true})
       (when submitted?
         ($ "div" {:class "text-center text-sm mt-2 text-blue-400"}
            "Selected answer"))
       (let [participant-count (count (:participants @state))]
         ($ "div" {:class "text-center text-gray-500 text-sm mt-2"}
            answer-count " / " participant-count " answered")))))

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
(defn scores-message [{:keys [final show-avatar? questions]}]
  ;; Use :questions list to limit which questions count, or nil for all (final)
  (let [sorted-scores (get-sorted-scores (when-not final questions))]
    ($ "div" {:class "flex items-end gap-2 mb-0.5"}
       ($ "div" {:class "w-8 shrink-0"}
          ($ "div" {:class (avatar-class show-avatar?)} "W"))
       ($ "div" {}
          ($ "div" {:class "rounded-2xl px-4 py-3 bg-yellow-900/30 border border-yellow-700"}
             ($ "div" {:class "text-yellow-300 font-bold mb-3"}
                (if final "🏆 Final Scores" "📊 Current Scores"))
             ($ "div" {:class "space-y-2"}
                (.map (to-array sorted-scores)
                      (fn [{:keys [name score rank]}]
                        (let [medal (case rank 1 "🥇" 2 "🥈" 3 "🥉" nil)
                              photo-url (get profile-photos name)
                              is-winner? (and final (= rank 1))
                              row-class (if is-winner?
                                          "flex justify-between items-center bg-yellow-500/10 -mx-2 px-2 py-1 rounded-lg"
                                          "flex justify-between items-center")]
                          ($ "div" {:key name :class row-class}
                             ($ "div" {:class "flex items-center gap-2 text-white"}
                                ($ "span" {:class "w-6 text-center text-sm text-gray-400"} (str rank "."))
                                (when medal ($ "span" {} medal))
                                ($ "div" {:class "relative"}
                                   (when is-winner?
                                     ($ "div" {:class "absolute -top-3 -right-1 text-base rotate-12"} "👑"))
                                   (if photo-url
                                     ($ "img" {:src photo-url
                                               :class (str "rounded-full object-cover " (if is-winner? "w-8 h-8" "w-6 h-6"))})
                                     ($ "div" {:class (str "rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs "
                                                           (if is-winner? "w-8 h-8" "w-6 h-6"))}
                                        (first name))))
                                ($ "span" {:class (when is-winner? "font-semibold")} name))
                             ($ "span" {:class (str "font-bold " (if is-winner? "text-yellow-300" "text-yellow-400"))} score)))))
                (when (empty? sorted-scores)
                  ($ "div" {:class "text-gray-400 italic"} "No scores yet"))))))))

;; Wrapped Profile Card - Spotify Wrapped style
(defn wrapped-profile-card [{:keys [name profile show-quiz-rank? quiz-rank is-winner? show-stats? for-screenshot?]}]
  (let [photo-url (get profile-photos name)
        {:keys [vibe vibe-desc total-messages messages-2025 avg-length emoji-count
                fav-emoji late-night-pct sarcasm traits]} profile]
    ($ "div" {:class "relative bg-gradient-to-br from-purple-900 via-indigo-900 to-blue-900 rounded-2xl overflow-hidden shadow-xl shadow-purple-900/50"}
       ;; Background pattern
       ($ "div" {:class "absolute inset-0 opacity-20"
                 :style {:background-image (str "url('" base-path "/background.svg')")
                         :background-repeat "repeat"
                         :filter "invert(1)"}})
       ;; Content
       ($ "div" {:class "relative p-6"}
          ;; Header with photo and name
          ($ "div" {:class (str "flex items-start gap-4" (when show-stats? " mb-6"))}
             ($ "div" {:class "relative"}
                ;; Crown for winner - larger with animation
                (when is-winner?
                  ($ "div" {:class "absolute -top-5 left-1/2 -translate-x-1/2 text-4xl z-10 animate-bounce"
                            :style {:animationDuration "2s"}} "👑"))
                ;; Quiz rank badge - gradient style
                (when (and show-quiz-rank? quiz-rank)
                  ($ "div" {:class "absolute -bottom-2 -right-2 bg-gradient-to-br from-yellow-400 to-amber-500 text-black w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm z-10 shadow-lg"}
                     (str "#" quiz-rank)))
                ;; Profile photo - better border
                (if photo-url
                  ($ "img" {:src photo-url
                            :class "w-24 h-24 rounded-full object-cover border-4 border-white/30 shadow-lg"})
                  ($ "div" {:class "w-24 h-24 rounded-full bg-white/20 flex items-center justify-center text-4xl font-bold text-white border-4 border-white/30"}
                     (first name))))
             ;; Name and vibe
             ($ "div" {:class "flex-1 min-w-0"}
                ($ "div" {:class "text-white text-2xl font-bold mb-1 truncate"} name)
                ($ "div" {:class "text-purple-300 text-lg font-semibold mb-1"} vibe)
                (when show-stats?
                  ($ "div" {:class "text-gray-300 text-sm"} vibe-desc))))

          ;; Pre-quiz teaser when stats hidden
          (when-not show-stats?
            ($ "div" {:class "mt-4 text-center text-gray-400 text-sm italic"}
               "More stats revealed after the quiz..."))

          ;; Stats grid - only shown after quiz
          (when show-stats?
            ($ "div" {}
               ($ "div" {:class "grid grid-cols-3 gap-3 mb-4"}
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} (.toLocaleString total-messages))
                     ($ "div" {:class "text-xs text-gray-300"} "Messages"))
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} avg-length)
                     ($ "div" {:class "text-xs text-gray-300"} "Words/msg"))
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} (str emoji-count))
                     ($ "div" {:class "text-xs text-gray-300"} "Emojis"))
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} (if fav-emoji fav-emoji "-"))
                     ($ "div" {:class "text-xs text-gray-300"} "Fav Emoji"))
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} (str late-night-pct "%"))
                     ($ "div" {:class "text-xs text-gray-300"} "Night Owl"))
                  ($ "div" {:class "bg-white/10 rounded-xl p-3 text-center"}
                     ($ "div" {:class "text-2xl font-bold text-white"} (str (js/Math.round (* sarcasm 100)) "%"))
                     ($ "div" {:class "text-xs text-gray-300"} "Sarcasm")))

               ;; Traits
               (when (seq traits)
                 ($ "div" {:class "flex flex-wrap gap-2 mb-4"}
                    (.map (to-array traits)
                          (fn [trait]
                            ($ "span" {:key trait
                                       :class "bg-white/20 text-white px-3 py-1 rounded-full text-sm"}
                               trait)))))

               ;; 2025 stats
               ($ "div" {:class "text-center"}
                  ($ "div" {:class "text-gray-400 text-xs"} "2025")
                  ($ "div" {:class "text-white font-semibold"}
                     (.toLocaleString messages-2025) " messages this year"))))

          ;; Watermark for screenshots
          (when for-screenshot?
            ($ "div" {:class "mt-4 pt-3 border-t border-white/10 text-center"}
               ($ "div" {:class "text-gray-500 text-xs"} "Chat: Wrapped 2025")))))))

;; Fullscreen profile modal
(defn profile-fullscreen [{:keys [name profile show-quiz-rank? quiz-rank is-winner? show-stats? on-close]}]
  ($ "div" {:class "fixed inset-0 z-50 bg-black flex items-center justify-center p-4"
            :on-click on-close}
     ($ "div" {:class "w-full max-w-sm"
               :on-click (fn [e] (.stopPropagation e))}
        ($ wrapped-profile-card {:name name
                                 :profile profile
                                 :show-quiz-rank? show-quiz-rank?
                                 :quiz-rank quiz-rank
                                 :is-winner? is-winner?
                                 :show-stats? show-stats?
                                 :for-screenshot? true}))))

;; Profile viewer - shows user's own profile based on selected name
(defn wrapped-profile-selector [{:keys [show-avatar? is-last? show-stats]}]
  (let [[fullscreen? set-fullscreen!] (useState false)
        my-name (:my-name @state)
        profile (get wrapped-profiles my-name)
        sorted-scores (get-sorted-scores)
        my-entry (first (filter #(= (:name %) my-name) sorted-scores))
        quiz-rank (when show-stats (:rank my-entry))
        is-winner? (and show-stats (= quiz-rank 1))]
    ($ "div" {:class "py-2 pl-10"}
       (if (and my-name profile)
         ;; Show their profile card with tap to fullscreen
         ($ "div" {}
            ($ "div" {:class "text-gray-300 text-sm mb-3"} "Tap to view fullscreen:")
            ($ "div" {:class "cursor-pointer"
                      :on-click #(set-fullscreen! true)}
               ($ wrapped-profile-card {:name my-name
                                        :profile profile
                                        :show-quiz-rank? show-stats
                                        :quiz-rank quiz-rank
                                        :is-winner? is-winner?
                                        :show-stats? show-stats})))
         ;; Fallback if no name selected
         ($ "div" {:class "text-gray-400 text-sm italic"}
            "Select your name above to see your Wrapped profile"))
       ;; Fullscreen modal
       (when fullscreen?
         ($ profile-fullscreen {:name my-name
                                :profile profile
                                :show-quiz-rank? show-stats
                                :quiz-rank quiz-rank
                                :is-winner? is-winner?
                                :show-stats? show-stats
                                :on-close #(set-fullscreen! false)})))))

;; Profile gallery - grid to view anyone's profile at the end
(defn wrapped-profile-gallery [{:keys [show-avatar? is-last?]}]
  (let [[selected-profile set-selected!] (useState nil)
        [fullscreen? set-fullscreen!] (useState false)
        sorted-scores (get-sorted-scores)
        get-entry (fn [n] (first (filter #(= (:name %) n) sorted-scores)))]
    ($ "div" {:class "py-2 pl-10"}
       ($ "div" {:class "grid grid-cols-2 gap-2"}
          (.map (to-array participant-names)
                (fn [name]
                  (let [photo-url (get profile-photos name)
                        profile (get wrapped-profiles name)
                        entry (get-entry name)
                        quiz-rank (:rank entry)
                        is-winner? (= quiz-rank 1)]
                    (when profile
                      ($ "button"
                         {:key name
                          :class "flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-all bg-[#242424] text-white hover:bg-[#2f2f2f]"
                          :on-click (fn []
                                      (set-selected! name)
                                      (set-fullscreen! true))}
                         ($ "div" {:class "relative"}
                            (when is-winner?
                              ($ "div" {:class "absolute -top-2 -right-1 text-xs"} "👑"))
                            (if photo-url
                              ($ "img" {:src photo-url :class "w-8 h-8 rounded-full object-cover"})
                              ($ "div" {:class "w-8 h-8 rounded-full bg-[#2f2f2f] flex items-center justify-center text-xs"}
                                 (first name))))
                         ($ "span" {:class "truncate"} name)))))))
       ;; Fullscreen modal
       (when (and fullscreen? selected-profile)
         (let [profile (get wrapped-profiles selected-profile)
               entry (get-entry selected-profile)
               quiz-rank (:rank entry)
               is-winner? (= quiz-rank 1)]
           ($ profile-fullscreen {:name selected-profile
                                  :profile profile
                                  :show-quiz-rank? true
                                  :quiz-rank quiz-rank
                                  :is-winner? is-winner?
                                  :show-stats? true
                                  :on-close #(set-fullscreen! false)}))))))

;; Wrapped intro message
(defn wrapped-intro [{:keys [show-avatar?]}]
  ($ "div" {:class "flex items-end gap-2 mb-0.5"}
     ($ "div" {:class "w-8 shrink-0"}
        ($ "div" {:class (avatar-class show-avatar?)} "W"))
     ($ "div" {:class "min-w-0"}
        ($ "div" {:class "rounded-2xl px-4 py-3 bg-gradient-to-r from-purple-900/50 to-indigo-900/50 border border-purple-700/50"}
           ($ "div" {:class "text-purple-200 font-semibold text-lg mb-1"} "Your Wrapped is Ready!")
           ($ "div" {:class "text-gray-300 text-sm"} "It wouldn't be Wrapped if we didn't have a cool profile page")))))

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
      :emoji ($ emoji-message props)
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
      :holiday-collage ($ holiday-collage props)
      :reaction-ticker ($ reaction-ticker props)
      :text-with-reactions ($ text-with-reactions props)
      :wrapped-intro ($ wrapped-intro props)
      :wrapped-profiles ($ wrapped-profile-selector props)
      :wrapped-gallery ($ wrapped-profile-gallery props)
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
        total total-steps]
    ($ "div" {:class "px-3 pt-2 pb-4 bg-[#1c1c1d] border-t border-gray-800 pb-safe shrink-0 relative z-10"}
       ($ "div" {:class "flex items-center justify-between"}
          ($ "span" {:class "text-gray-400 text-xs shrink-0"} idx " / " total)
          ($ "div" {:class "flex items-center gap-1.5"}
             ($ "button" {:class "px-2 py-1 bg-gray-700 text-gray-300 rounded text-xs hover:bg-gray-600"
                          :disabled (zero? idx)
                          :on-click skip-to-top!}
                "↑ Top")
             ($ "button" {:class "px-2 py-1 bg-gray-700 text-gray-300 rounded text-xs hover:bg-gray-600"
                          :disabled (>= idx total)
                          :on-click skip-to-bottom!}
                "↓ Bottom")
             ($ "button" {:class "px-2 py-1 bg-gray-700 text-gray-300 rounded text-xs hover:bg-gray-600 disabled:bg-gray-800 disabled:text-gray-500"
                          :disabled (zero? idx)
                          :on-click go-back-message!}
                "← Back")
             ($ "button" {:class "px-2 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 disabled:bg-gray-600"
                          :disabled (>= idx total)
                          :on-click post-next-message!}
                "Next →"))))))

;; Main UI
(defn chat-quiz-ui []
  ($ "div" {:class "h-dvh bg-[#0e0e0e] flex flex-col overflow-hidden relative"}
     ($ "div" {:class "fixed inset-0 opacity-10 pointer-events-none z-0"
               :style {:background-image (str "url('" base-path "/background.svg')")
                       :background-repeat "repeat"
                       :filter "invert(1)"}})
     ($ header)
     ($ messages-area)
     ($ controller-panel)))



;; >> Initialization

(defn init! []
  ;; Calculate initial scores from static data
  (swap! state (fn [s]
                 (assoc s :scores (calculate-all-scores (:participants s) (:all-answers s)))))

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
