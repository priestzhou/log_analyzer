(ns log-collector.log-line-parser
    (:require
        [clojure.string :as str]
        [clojure.java.io :as io]
        [clj-time.core :as datetime]
        [clj-time.format :as datetime-fmt]
        [clj-time.coerce :as datetime-coerce]
        [utilities.net :as net]
    )
    (:import 
        [java.io BufferedReader InputStream]
        [java.nio.file Files OpenOption StandardOpenOption]
    )
)

(def ^:private raw-log-pattern
    #"(?sx) # dot matches all and ignore whitespaces and comments
    ^(\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2},\d{3}) # timestamp
    \s+
    (TRACE|DEBUG|INFO|WARN|ERROR|FATAL) # logging level
    \s+
    (.*?) # location in code
    :\s*
    (.*) # log message"
)

(defn- default-time-format' []
    (datetime-fmt/formatter "yyyy-MM-dd HH:mm:ss,SSS" (datetime/default-time-zone))
)

(def ^:private default-time-format (memoize default-time-format'))

(defn- parse-timestamp [x]
    (let [fmt (default-time-format)
        dt (datetime-fmt/parse fmt x)
        ]
        (datetime-coerce/to-long dt)
    )
)

(defn parse-log-line [x]
    (if-let [parsed (re-find raw-log-pattern x)]
        (let [[_ tst lvl loc msg] parsed]
            [(parse-timestamp tst) lvl loc (str/trim msg)]
        )
    )
)

(defn- format-log-line [parse-log-line x]
    (if-let [[tst lvl loc msg] (parse-log-line x)]
        {
            :timestamp tst, 
            :level lvl, 
            :location loc, 
            :host (-> (net/localhost) (first) (.getHostAddress))
            :message msg
        }
    )
)

(defn- new-log-line? [parse-log-line ln]
    (parse-log-line ln)
)

(defn- reader->lazyseq! [rdr]
    (if-let [ln (.readLine rdr)]
        (lazy-seq
            (cons ln (reader->lazyseq! rdr))
        )
        (do
            (.close rdr)
            []
        )
    )
)

(defn- partition-by-log-events' [new-log-line? dealed rst]
{
    :pre [
        (not (empty? dealed))
    ]
}
    (if (empty? rst)
        [(str/join "\n" dealed)]
        (let [[x & xs] rst]
            (if (new-log-line? x)
                (lazy-seq
                    (cons
                        (str/join "\n" dealed)
                        (partition-by-log-events' new-log-line? [x] xs)
                    )
                )
                (partition-by-log-events' new-log-line? (conj dealed x) xs)
            )
        )
    )
)

(defn- partition-by-log-events [new-log-line? lns]
    (when-not (empty? lns)
        (let [[x & xs] lns]
            (partition-by-log-events' new-log-line? [x] xs)
        )
    )
)

(defn parse-log-events! [parse-log-line rdr]
    (->> rdr
        (reader->lazyseq!)
        (partition-by-log-events
            (partial new-log-line? parse-log-line)
        )
        (map (partial format-log-line parse-log-line))
        (filter #(not (nil? %)))
    )
)

(defn read-logs [opts f offset]
    (let [parser (get opts :parser parse-log-line)
        is (Files/newInputStream f 
            (into-array OpenOption [StandardOpenOption/READ])
        )
        _ (.skip is offset)
        rdr (io/reader is)
        ]
        (parse-log-events! parser rdr)
    )
)

