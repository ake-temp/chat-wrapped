(ns server)

(defn file [path content-type]
  (js/Response. (.file js/Bun path) {:headers {"Content-Type" content-type}}))

(defn handler [req]
  (let [url (js/URL. (.-url req))
        pathname (.-pathname url)]
    (condp re-matches pathname
      #"/" (file "public/index.html" "text/html")
      #"/app\.js" (file "app.js" "application/javascript")
      #"/node_modules/.*" (file (str ".." pathname) "application/javascript")
      (js/Response. "Not Found" #js {:status 404}))))

(def server (.serve js/Bun {:port 3000 :fetch handler}))

(.log js/console (str "Server running at http://localhost:" (.-port server)))
