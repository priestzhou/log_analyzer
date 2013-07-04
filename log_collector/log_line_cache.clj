(ns log-collector.log-line-cache
)

; when must I expire a log line in cache?
(def ^:private cache (atom 0))

(defn cache-log-line [l]
    (when (> (:timestamp l) @cache )
        (swap! cache (constantly (:timestamp l)))
        l
    )
)
