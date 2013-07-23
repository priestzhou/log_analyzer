(ns log-collector.log-line-parser
    (:require
        [clojure.string :as str]
        [clojure.java.io :as io]
    )
    (:import 
        java.io.BufferedReader
        java.util.Date
        [java.text SimpleDateFormat ParsePosition]
    )
)

(def ^:dynamic *raw-log-pattern* 
    #"(^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\s+(.*?):\s*(.*)"
)

(defn- parse-timestamp [x]
    (-> (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss,SSS")
        (.parse x (ParsePosition. 0))
        (.getTime)
    )
)

(defn- parse-log-line [x]
    (let [[_ tst lvl loc msg] (re-find *raw-log-pattern* (first x))]
        {:timestamp (parse-timestamp tst), 
            :level lvl, 
            :location loc, 
            :message 
                (->> (cons msg (rest x))
                    (str/join "\n")
                    (str/trim-newline)
                )
        }
    )
)

(defn- next-log-event [rdr init]
    (let [raw-text (transient (if init [init] []))]
        (loop [ln (.readLine rdr)]
            (if-not ln
                [(persistent! raw-text) nil]
                (if (re-find *raw-log-pattern* ln)
                    [(persistent! raw-text) ln]
                    (do
                        (conj! raw-text ln)
                        (recur (.readLine rdr))
                    )
                )
            )
        )
    )
)

(defn- parse-log-raw' [rdr init]
    (let [[raw-text nxt-header] (next-log-event rdr init)]
        (if nxt-header
            (lazy-seq 
                (cons 
                    raw-text
                    (parse-log-raw' rdr nxt-header)
                )
            )
            (if (= 0 (count raw-text))
                []
                [raw-text]
            )
        )
    )
)

(defn parse-log-raw [rdr]
    (map parse-log-line 
        (rest (parse-log-raw' rdr nil))
    )
)

(defn parse-log-with-path [p]
    (with-open [rdr (io/reader (.toFile p))]
        (doall (parse-log-raw rdr))
    )
)

(def ^:private cache (atom 0))

(defn cache-log-line [l]
    (when (> (:timestamp l) @cache )
        (swap! cache (constantly (:timestamp l)))
        l
    )
)
