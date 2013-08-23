(ns kafka-hdfs.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clj-time.coerce :as date-coerce]
        [clj-time.format :as date-format]
        [clojure.data.json :as json]
        [clojure.java.io :as io]
        [utilities.core :as helpers]
        [kfktools.core :as kfk]
    )
    (:import
        [java.nio.charset StandardCharsets]
        [java.util.concurrent BlockingQueue TimeUnit]
        [java.util TreeMap NavigableMap Map]
        [java.net URI]
        [org.apache.hadoop.fs Path FileSystem FileStatus]
    )
)

(defloggers debug info warn error)

(defn- lazyseq->queue! [topic seq ^BlockingQueue q cnt]
    (when-not (empty? seq)
        (let [[x & xs] seq
            message (:message x)
            message (if message (String. message) nil)
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


(defn hpath->uri [^Path p]
    (.toUri p)
)

(defn uri->hpath [^URI uri]
    (Path. uri)
)

(defn gen-uri [base topic ts]
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

(defn recent? [existents uri]
    (let [recent (.lastEntry existents)
        [recent-uri _] (.getValue recent)
        ]
        (= recent-uri uri)
    )
)

(defn parse-timestamp-from-path [p]
    (let [f (.getName p)]
        (->>
            (re-find #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[.]\d{3}Z(?=[.])" f)
            (date-format/parse (date-format/formatters :date-time))
            (date-coerce/to-long)
        )
    )
)

(defn- scan-existents! [tree-map fs base]
    (let [xs (.listStatus fs base)]
        (doseq [x (helpers/array->lazy-seq xs)
            :let [p (.getPath x)]
            ]
            (cond
                (.isDirectory x) (scan-existents! tree-map fs p)
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
        (scan-existents! tree-map fs (uri->hpath base))
        tree-map
    )
)

(def ^:dynamic size-for-new-file (* 60 1024 1024))

(defn out-stream [^NavigableMap existents ^long ts create-file append-file]
    (if (.isEmpty existents)
        (create-file ts)
        (let [kv (.floorEntry existents ts)
            [uri size] (.getValue kv)
            ]
            (if (and (recent? existents uri) (> size size-for-new-file))
                (create-file ts)
                (append-file uri (.getKey kv))
            )
        )
    )
)

(defn create-file [^FileSystem fs add-meta gen-uri ^long ts]
    (let [uri (gen-uri ts)
        out (.create fs (uri->hpath uri))
        ]
        (add-meta ts uri)
        out
    )
)

(defn add-meta [^NavigableMap existents size ts uri]
    (.put existents ts [uri size])
)

(defn append-file [^FileSystem fs update-meta uri ts]
    (let [out (.append fs (uri->hpath uri))]
        (update-meta ts)
        out
    )
)

(defn update-meta [^NavigableMap existents inc-size ts]
    (let [[uri size] (.get existents ts)]
        (.put existents ts [uri (+ size inc-size)])
    )
)

(defn save->hdfs! [fs base timeout queue ^NavigableMap existents]
    (let [m (poll! queue timeout)]
        (when m
            (let [{:keys [topic message]} m
                jsoned-message (json/read-str message :key-fn keyword)
                ts (:timestamp jsoned-message)
                ]
                (with-open [out (out-stream existents ts 
                        (partial create-file fs 
                            (partial add-meta existents (count message))
                            (partial gen-uri base topic)
                        )
                        (partial append-file fs
                            (partial update-meta existents (count message))
                        )
                    )
                    ]
                    (.write out (.getBytes (format "%s\n" message) StandardCharsets/UTF_8))
                )
            )
        )
    )
)
