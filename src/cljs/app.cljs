(ns app)

(defn render []
  (let [app-el (js/document.getElementById "app")]
    (set! (.-innerHTML app-el) "<h1>Hello from Squint!</h1>")))

(render)
