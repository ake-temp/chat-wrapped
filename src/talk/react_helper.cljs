(ns talk.react-helper
  (:require ["react" :as react]
            ["react-dom/client" :as react-dom]))

;; Prop name conversions from hiccup style to React style
(def prop-mappings
  {"class" "className"
   "for" "htmlFor"
   "on-click" "onClick"
   "on-change" "onChange"
   "on-submit" "onSubmit"
   "on-key-down" "onKeyDown"
   "on-key-up" "onKeyUp"
   "on-mouse-enter" "onMouseEnter"
   "on-mouse-leave" "onMouseLeave"
   "on-focus" "onFocus"
   "on-blur" "onBlur"
   "on-input" "onInput"
   "tab-index" "tabIndex"
   "auto-focus" "autoFocus"
   "auto-complete" "autoComplete"
   "default-value" "defaultValue"
   "read-only" "readOnly"
   "col-span" "colSpan"
   "row-span" "rowSpan"})

;; Style property conversions (kebab-case to camelCase)
(def style-mappings
  {"background-image" "backgroundImage"
   "background-repeat" "backgroundRepeat"
   "background-size" "backgroundSize"
   "background-position" "backgroundPosition"
   "background-color" "backgroundColor"
   "transition-timing-function" "transitionTimingFunction"
   "transition-duration" "transitionDuration"
   "transition-property" "transitionProperty"
   "font-size" "fontSize"
   "font-weight" "fontWeight"
   "font-family" "fontFamily"
   "line-height" "lineHeight"
   "text-align" "textAlign"
   "text-decoration" "textDecoration"
   "border-radius" "borderRadius"
   "border-color" "borderColor"
   "border-width" "borderWidth"
   "margin-top" "marginTop"
   "margin-bottom" "marginBottom"
   "margin-left" "marginLeft"
   "margin-right" "marginRight"
   "padding-top" "paddingTop"
   "padding-bottom" "paddingBottom"
   "padding-left" "paddingLeft"
   "padding-right" "paddingRight"
   "z-index" "zIndex"
   "max-width" "maxWidth"
   "min-width" "minWidth"
   "max-height" "maxHeight"
   "min-height" "minHeight"})

(defn convert-style [style]
  "Convert kebab-case style properties to camelCase"
  (when style
    (let [result #js {}]
      (doseq [k (js/Object.keys style)]
        (let [react-key (or (get style-mappings k) k)
              v (aget style k)]
          (aset result react-key v)))
      result)))

(defn convert-props [props]
  "Convert hiccup-style props to React props"
  (when props
    (let [result #js {}]
      (doseq [k (js/Object.keys props)]
        (let [react-key (or (get prop-mappings k) k)
              v (aget props k)
              ;; Convert style object if present
              final-v (if (= k "style") (convert-style v) v)]
          (aset result react-key final-v)))
      result)))

;; Create React element - main API
(defn $ [type & args]
  "Create a React element. Usage: ($ \"div\" {:class \"foo\"} child1 child2)
   or ($ \"div\" child1 child2) if no props"
  (let [first-arg (first args)
        has-props? (and (some? first-arg)
                        (not (string? first-arg))
                        (not (react/isValidElement first-arg))
                        (object? first-arg)
                        (not (array? first-arg)))
        props (if has-props? (convert-props first-arg) nil)
        children (if has-props? (rest args) args)]
    (apply react/createElement type props children)))

;; Re-export React hooks and utilities
(def useState react/useState)
(def useEffect react/useEffect)
(def useRef react/useRef)
(def useMemo react/useMemo)
(def useCallback react/useCallback)
(def useContext react/useContext)
(def createContext react/createContext)
(def Fragment react/Fragment)

;; React DOM
(def createRoot react-dom/createRoot)

;; Helper to force re-render from external state changes (like atoms)
(defn useForceUpdate []
  (let [[_ set-state] (useState 0)]
    (fn [] (set-state inc))))

;; Counter for unique watch keys
(def ^:private watch-counter (atom 0))

;; Helper to subscribe to an atom
(defn useAtom [atom]
  "Subscribe to a squint atom and re-render when it changes"
  (let [force-update (useForceUpdate)
        value (useRef @atom)
        key-ref (useRef nil)]
    ;; Create a unique key on first render
    (when-not (.-current key-ref)
      (set! (.-current key-ref) (str "react-atom-" (swap! watch-counter inc))))
    (useEffect
      (fn []
        (let [key (.-current key-ref)]
          (add-watch atom key
            (fn [_ _ _ new-val]
              (set! (.-current value) new-val)
              (force-update)))
          ;; Cleanup
          (fn [] (remove-watch atom key))))
      #js [])
    (.-current value)))
