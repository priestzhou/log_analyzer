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
        [clojure.string :as str]
        [utilities.core :as util]
        [kfktools.core :as kfk]
    )
    (:import
        [java.nio.charset StandardCharsets]
        [java.io InputStreamReader BufferedReader]
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
    (let [xs (kfk/listenTo consumer topic)]
        (future
            (lazyseq->queue! topic xs queue 1)
        )
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

(defn gen-uri [base topic ts]
    (let [d (date-coerce/from-long ts)
        rfc3339-fulldate (date-format/formatters :date)
        datetime (date-format/formatters :basic-date-time) ; hdfs does not support \:
        relative (format "./%s/%s.%s"
            (date-format/unparse rfc3339-fulldate d)
            (date-format/unparse datetime d) 
            topic
        )
        final (.resolve base relative)
        ]
        final
    )
)

(defn- parse-timestamp-from-path [p]
    (let [f (.getName p)]
        (->>
            (re-find #"\d{4}\d{2}\d{2}T\d{2}\d{2}\d{2}[.]\d{3}Z(?=[.])" f)
            (date-format/parse (date-format/formatters :basic-date-time))
            (date-coerce/to-long)
        )
    )
)

(defn- scan-existents' [tree-map fs base]
    (when (.exists fs base)
        (let [xs (.listStatus fs base)]
            (doseq [x (util/array->lazy-seq xs)
                :let [p (.getPath x)]
                ]
                (cond
                    (.isDirectory x) (scan-existents' tree-map fs p)
                    (.isFile x) (.put tree-map 
                        (parse-timestamp-from-path p) [(.toUri p) (.getLen x) nil]
                    )
                    :else (throw 
                        (RuntimeException. (str "unknown file type: " (.toUri p)))
                    )
                )
            )
        )
    )
)

(defn- lazy-readlines [^BufferedReader rdr]
    (if-let [ln (.readLine rdr)]
        (lazy-seq
            (cons ln (lazy-readlines rdr))
        )
    )
)

(defn- update-recent-in-last! [existents fs]
    (if-let [entry (.lastEntry existents)]
        (let [ts (.getKey entry)
            [uri size] (.getValue entry)
            ]
            (with-open [is (.open fs (uri->hpath uri))]
                (let [decoded (InputStreamReader. is StandardCharsets/UTF_8)
                    buffered (BufferedReader. decoded)
                    recent-ts (->> buffered
                        (lazy-readlines)
                        (map #(json/read-str % :key-fn keyword))
                        (map #(:timestamp %))
                        (reduce max)
                    )
                    ]
                    (.put existents ts [uri size recent-ts])
                )
            )
        )
    )
)

(defn scan-existents [fs base]
    (let [tree-map (TreeMap.)]
        (scan-existents' tree-map fs (uri->hpath base))
        (update-recent-in-last! tree-map fs)
        tree-map
    )
)

(def ^:dynamic size-for-new-file (* 60 1024 1024))

(defn- recent? [existents uri ts]
    (let [recent (.lastEntry existents)
        [recent-uri _ recent-ts] (.getValue recent)
        ]
        (assert (not (nil? recent-ts)))
        (and
            (= recent-uri uri)
            (> ts recent-ts)
        )
    )
)

(defn- within-same-day? [uri ts]
    (let [d0 (-> uri
            (uri->hpath)
            (.getName)
            (str/split #"[T]" 2)
            (first)
        )
        d1 (->> ts
            (date-coerce/from-long)
            (date-format/unparse (date-format/formatters :basic-date))
        )
        ]
        (= d0 d1)
    )
)

(defn- out-stream! [^NavigableMap existents ^long ts create-file! append-file!]
    (if-let [kv (.floorEntry existents ts)]
        (let [[uri size] (.getValue kv)]
            (if (and (recent? existents uri ts) (> size size-for-new-file))
                (create-file! ts)
                (if (within-same-day? uri ts)
                    (append-file! uri ts)
                    (create-file! ts)
                )
            )
        )
        (create-file! ts)
    )
)

(defn- new-file! [^FileSystem fs ^Deque cache uri]
    (let [out (.create fs (uri->hpath uri))]
        (info "create file" :uri uri)
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
    (.put existents ts [uri size ts])
)

(defn- search-in-cache! [^Iterator cache-iter uri]
    (if-not (.hasNext cache-iter)
        nil
        (let [cache-item (.next cache-iter)]
            (if (= (:uri cache-item) uri)
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
            (info "append file" :uri uri)
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
    (let [kv (.floorEntry existents ts)
        _ (assert kv)
        [uri size recent-ts] (.getValue kv)
        new-size (+ size inc-size)
        new-recent-ts (if recent-ts
            (max recent-ts ts)
            ts
        )
        ]
        (.put existents (.getKey kv) [uri new-size new-recent-ts])
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

(def ^:dynamic timeout 60000)
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
                    (info "close file" :uri (:uri x))
                    (.remove iter)
                )
            )
        )
    )
    (while (> (.size cache) max-open-files)
        (let [x (.removeLast cache)]
            (.close (:stream x))
            (info "close file" :uri (:uri x))
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
