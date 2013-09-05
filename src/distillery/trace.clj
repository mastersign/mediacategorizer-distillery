(ns distillery.trace)

(def ^:private trace-agent (agent nil))

(defn trace-message
  [& msg]
  (let [text (apply str msg)]
  (send-off trace-agent
        (fn [state] (println (str "# " text))))
  text))

(defmacro trace-block
  [msg & body]
  `(do
     (trace-message (str "BEGIN " ~msg "..."))
     (let [start# (System/nanoTime)
           result# (do ~@body)
           end# (/ (- (System/nanoTime) start#) 1000000.0)]
       (trace-message (str "END   " ~msg " after " end# " msecs"))
       result#)))

