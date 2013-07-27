(ns java-parser.core
    (:require
        [utilities.parse :as ups]
    )
)

(defn jwhitespace [stream]
    (->> stream
        ((ups/expect-char-if #{\space \tab \formfeed \newline}))
    )
)
