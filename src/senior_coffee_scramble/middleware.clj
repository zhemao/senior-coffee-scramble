(ns senior-coffee-scramble.middleware)

(defn exception-stacktrace [e]
  (apply str (cons (str e "\n")
                   (map #(str "\tat " % "\n") (.getStackTrace e)))))

(defn try-handler [f request]
  (try
    (f request)
    (catch Exception e
      {:status 500
       :body (if (System/getenv "DEBUG")
               (exception-stacktrace e)
               "Something went wrong!")})))
