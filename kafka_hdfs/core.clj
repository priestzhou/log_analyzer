(ns kafka-hdfs.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clj-time.coerce :as date-coerce]
        [clj-time.format :as date-format]
        [clj-time.core :as date]
        [clojure.data.json :as json]
        [clojure.java.io :as io]
        [utilities.core :as util]
        [kfktools.core :as kfk]
    )
    (:import
        [java.util.concurrent BlockingQueue TimeUnit]
        [java.util TreeMap NavigableMap Map Deque Iterator]
        [java.net URI]
        [org.apache.hadoop.fs Path FileSystem FileStatus]
    )
)

(defloggers debug info warn error)

(defn- lazyseq->queue! [topic seq ^BlockingQueue q cnt]
    (when-not (empty? seq)
        (let [[x & xs] seq
            message (:message x)
            ]
            (.put q {:topic topic :message message})
            (if (= cnt 1000)
                (do
                    (info "consume 1000 messages")
                    (recur topic xs q 1)
                )
                (recur topic xs q (inc cnt))
            )
            
        )
    )
)

(defn assign-consumer-to-queue! [consumer topic queue]
    (future-call 
        (partial lazyseq->queue! topic (kfk/listenTo consumer topic) queue 0)
    )
)

(defn poll! [^BlockingQueue q timeout]
    (let [x (.poll q timeout TimeUnit/MILLISECONDS)]
        x
    )
)


(defn- uri->hpath [^URI uri]
    (Path. uri)
)

(defn- gen-uri [base topic ts]
    (let [d (date-coerce/from-long ts)
        rfc3339-fulldate (date-format/formatter "yyyy-MM-dd")
        rfc3339-datetime (date-format/formatters :date-time)
        ]
        (.resolve base 
            (format "./%s/%s.%s"
                (date-format/unparse rfc3339-fulldate d)
                (date-format/unparse rfc3339-datetime d) 
                topic
            )
        )
    )
)

(defn- parse-timestamp-from-path [p]
    (let [f (.getName p)]
        (->>
            (re-find #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[.]\d{3}Z(?=[.])" f)
            (date-format/parse (date-format/formatters :date-time))
            (date-coerce/to-long)
        )
    )
)

(defn- scan-existents' [tree-map fs base]
    (let [xs (.listStatus fs base)]
        (doseq [x (util/array->lazy-seq xs)
            :let [p (.getPath x)]
            ]
            (cond
                (.isDirectory x) (scan-existents' tree-map fs p)
                (.isFile x) (.put tree-map 
                    (parse-timestamp-from-path p) [(.toUri p) (.getLen x)]
                )
                :else (throw 
                    (RuntimeException. (str "unknown file type: " (.toUri p)))
                )
            )
        )
    )
)

(defn scan-existents [fs base]
    (let [tree-map (TreeMap.)]
        (scan-existents' tree-map fs (uri->hpath base))
        tree-map
    )
)

(def ^:dynamic size-for-new-file (* 60 1024 1024))

(defn- recent? [existents uri]
    (let [recent (.lastEntry existents)
        [recent-uri _] (.getValue recent)
        ]
        (= recent-uri uri)
    )
)

(defn- out-stream! [^NavigableMap existents ^long ts create-file! append-file!]
    (if (.isEmpty existents)
        (create-file! ts)
        (let [kv (.floorEntry existents ts)
            [uri size] (.getValue kv)
            ]
            (if (and (recent? existents uri) (> size size-for-new-file))
                (create-file! ts)
                (append-file! uri (.getKey kv))
            )
        )
    )
)

(defn- new-file! [^FileSystem fs ^Deque cache uri]
    (let [out (.create fs (uri->hpath uri))]
        (.addFirst cache {:uri uri :stream out :open-timestamp (date/now)})
        out
    )
)

(defn- create-file! [new-file! add-meta! gen-uri ^long ts]
    (let [uri (gen-uri ts)]
        (add-meta! ts uri)
        (new-file! uri)
    )
)

(defn- add-meta! [^NavigableMap existents size ts uri]
    (.put existents ts [uri size])
)

(defn- search-in-cache! [^Iterator cache-iter uri]
    (if-not (.hasNext cache-iter)
        nil
        (let [cache-item (.next cache-iter)]
            (if (= (.uri cache-item) uri)
                (:stream cache-item)
                (recur cache-iter uri)
            )
        )
    )
)

(defn- existent-file! [^FileSystem fs ^Deque cache uri]
    (if-let [hit (search-in-cache! (.iterator cache) uri)]
        hit
        (let [out (.append fs (uri->hpath uri))]
            (.addFirst cache {:uri uri :stream out :open-timestamp (date/now)})
            out
        )
    )
)

(defn- append-file! [existent-file! update-meta! uri ts]
    (let [out (existent-file! uri)]
        (update-meta! ts)
        out
    )
)

(defn- update-meta! [^NavigableMap existents inc-size ts]
    (let [[uri size] (.get existents ts)]
        (.put existents ts [uri (+ size inc-size)])
    )
)

(defn- parse-timestamp [^bytes m]
    (-> m
        (util/bytes->str)
        (json/read-str :key-fn keyword)
        (:timestamp)
    )
)

(def utf-8-newline (util/str->bytes "\n"))

(def ^:dynamic timeout 5000)
(def ^:dynamic max-open-files 100)

(defn- close-timeout-files! [^Deque cache]
    (let [iter (.iterator cache)
        now (date/now)
        start (date/minus now (date/millis timeout))
        ]
        (while (.hasNext iter)
            (let [x (.next iter)
                ts (:open-timestamp x)
                ]
                (when-not (date/within? start now ts)
                    (.close (:stream x))
                    (.remove iter)
                )
            )
        )
    )
)

(defn save->hdfs! [queue base ^FileSystem fs ^NavigableMap existents ^Deque cache]
    (let [m (poll! queue timeout)]
        (when m
            (let [{:keys [topic message]} m
                ts (parse-timestamp message)
                ]
                (let [out (out-stream! existents ts 
                        (partial create-file! 
                            (partial new-file! fs cache) 
                            (partial add-meta! existents (alength message))
                            (partial gen-uri base topic)
                        )
                        (partial append-file! 
                            (partial existent-file! fs cache)
                            (partial update-meta! existents (alength message))
                        )
                    )
                    ]
                    (.write out message)
                    (.write out utf-8-newline)
                )
            )
        )
        (close-timeout-files! cache)
    )
)
