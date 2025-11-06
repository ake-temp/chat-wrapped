(ns server)

(def server
  (.serve js/Bun
    #js {:port 3000
         :fetch (fn [req]
                  (let [url (js/URL. (.-url req))
                        pathname (.-pathname url)]
                    (cond
                      (= pathname "/")
                      (js/Response. (.file js/Bun "public/index.html"))

                      (= pathname "/app.js")
                      (js/Response. (.file js/Bun "app.js")
                                    #js {:headers #js {"Content-Type" "application/javascript"}})

                      (.startsWith pathname "/node_modules/")
                      (js/Response. (.file js/Bun (str ".." pathname))
                                    #js {:headers #js {"Content-Type" "application/javascript"}})

                      :else
                      (js/Response. "Not Found" #js {:status 404}))))}))

(.log js/console (str "Server running at http://localhost:" (.-port server)))
