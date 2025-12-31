(ns talk.ui
  (:require ["./ably.js" :as ably]
            ["./react_helper.js" :refer [$]]))

(defn connection-pill []
  (let [status (ably/connection-status)]
    ;; React handles nil gracefully - no need for [:div] placeholder
    (when-not (ably/connected?)
      ($ "div" {:class "fixed bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 bg-yellow-500 text-white rounded-full text-sm font-medium shadow-lg z-50"}
         "Connection status: " status))))
