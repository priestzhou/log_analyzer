(ns log-collector.log-line-cache
)

; when must I expire a log line in cache?
(def ^:private cache (atom #{}))

(defn cache-log-line [l]
)
