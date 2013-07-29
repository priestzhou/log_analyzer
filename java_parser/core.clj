(ns java-parser.core
    (:require
        [clojure.string :as str]
        [utilities.parse :as ups]
    )
    (:import
        utilities.parse.InvalidSyntaxException
    )
)

(defn- jwhitespaces-parser [stream]
    (let [[strm parsed] (
                (ups/many1 (ups/expect-char-if #{\space \tab \formfeed \newline}))
                stream
            )
        ]
        [strm [(ffirst parsed) (second (last parsed))]]
    )
)

(defn jwhitespaces []
    (partial jwhitespaces-parser)
)

(defn- jcomment-eol-parser [stream]
    (let [[strm [[start _] _ [end _]]] (
                (ups/chain
                    (ups/expect-string "//")
                    (ups/many (ups/expect-char-if #(not (#{\newline :eof} %))))
                    (ups/choice
                        (ups/expect-char \newline)
                        (ups/foresee (ups/expect-eof))
                    )
                )
                stream
            )
        ]
        [strm [:comment-eol (+ start 2) end]]
    )
)

(defn- jcomment-eol []
    (partial jcomment-eol-parser)
)

(defn- jcomment-traditional-parser [stream]
    (let [
            [strm [[start _] [_ end]]] (
                (ups/between 
                    (ups/expect-string "/*")
                    (ups/expect-string "*/")
                    (ups/expect-no-eof)
                )
                stream
            )
        ]
        [strm [:comment-traditional (+ start 2) (- end 2)]]
    )
)

(defn- jcomment-traditional []
    (partial jcomment-traditional-parser)
)

(defn jcomment []
    (ups/choice
        (jcomment-eol)
        (jcomment-traditional)
    )
)

(defn jblank []
    (ups/choice
        (jcomment)
        (jwhitespaces)
    )
)

(defn jblank-many []
    (ups/many
        (jblank)
    )
)

(defn jblank-many1 []
    (ups/many1
        (jblank)
    )
)

(defn- jidentifier-parser [stream]
    (let [[strm [[start _] id-part]] (
                (ups/chain
                    (ups/expect-char-if #(and 
                        (instance? Character %) 
                        (Character/isJavaIdentifierStart %)
                    ))
                    (ups/many (ups/expect-char-if #(and
                        (instance? Character %)
                        (Character/isJavaIdentifierPart %)
                    )))
                )
                stream
            )
            end (if (empty? id-part)
                (inc start)
                (second (last id-part))
            )
            sb (doto (StringBuilder. (- end start)))
        ]
        (doseq [[x] (take (- end start) stream)]
            (.append sb x)
        )
        [strm [:identifier (str sb)]]
    )
)

(defn jidentifier []
    (partial jidentifier-parser)
)

(defn- jidentifier-abs-parser [stream]
    (let [[strm1 prsd1] ((jidentifier) stream)
            [strm2 prsd2] (
                (ups/many (ups/chain (ups/expect-char \.) (jidentifier)))
                strm1
            )
            segs (for [[_ [_ x]] prsd2] x)
        ]
        [strm2 [:identifier-abs (str/join "." (cons (second prsd1) segs))]]
    )
)

(defn jidentifier-abs []
    (partial jidentifier-abs-parser)
)


(defn- digit-or-underscore [ch]
    (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \_} ch)
)

(defn- dec-str->int [s len]
    (let [sb (StringBuilder. len)]
        (doseq [x (for [y s :when (not= y \_)] y)]
            (.append sb x)
        )
        (read-string (str sb))
    )
)

(defn- jliteral-int-dec-parser [stream]
    (let [[strm prsd] (try
                (
                    (ups/many1 (ups/expect-char-if digit-or-underscore))
                    stream
                )
            (catch InvalidSyntaxException _
                (throw (ups/gen-ISE stream "expect decimal"))
            ))
            len (- (second (last prsd)) (ffirst prsd))
            bufs (for [[x] (take len stream)] x)
        ]
        (cond
            (and (> (count bufs) 1) (= (first bufs) \0)) (throw
                (ups/gen-ISE stream "expect decimal")
            )
            (= (first bufs) \_) (throw
                (ups/gen-ISE stream "decimal cannot be led by '_'")
            )
            (= (last bufs) \_) (throw
                (ups/gen-ISE stream "decimal cannot be followed by '_'")
            )
            :else [strm [:literal-int (dec-str->int bufs len)]]
        )
    )
)

(defn- jliteral-int-dec []
    (partial jliteral-int-dec-parser)
)

(defn jliteral-int []
    (ups/choice
        (jliteral-int-dec)
    )
)
