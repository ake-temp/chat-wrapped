(ns server
  (:require ["./http" :as http]
            ["./sse" :as sse]
            ["node:fs" :as fs]))

(def counter (atom 0))
(def clients (atom #{}))

(defn handle-counter [f]
  (swap! counter f)
  (sse/broadcast @clients {:event "counter" :data @counter})
  {:status 200 :headers {"content-type" "text/plain"} :body "OK"})

(defn handler [stream headers]
  (let [pathname (aget headers ":path")]
    (condp re-matches pathname
      ;; >> Static Files
      #"/"
      (http/respond-file! stream (http/file "public/index.html" "text/html"))
      #"/app\.js"
      (http/respond-file! stream (http/file "app.js" "application/javascript"))
      #"/node_modules/.*" 
      (http/respond-file! stream (http/file (str ".." pathname) "application/javascript"))

      ;; >> API
      #"/api/sse"
      (sse/setup! stream
        (fn on-start [stream]
          (println "New SSE client connected")
          (swap! clients conj stream)
          (.write stream (sse/event {:event "counter" :data @counter})))
        (fn on-close [stream]
          (println "SSE client disconnected")
          (swap! clients disj stream)))
      #"/api/counter/inc" (http/respond! stream (handle-counter inc))
      #"/api/counter/dec" (http/respond! stream (handle-counter dec))

      ;; >> Default
      (http/respond! stream (http/not-found)))))

(http/run-server handler
                 {:key (.readFileSync fs "../localhost-key.pem")
                  :cert (.readFileSync fs "../localhost-cert.pem")})
