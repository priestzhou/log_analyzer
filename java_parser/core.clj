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


(defn- parse-digits-and-underscores' [stream parsers last-stream last-parsed]
    (if (empty? parsers)
        [stream last-stream last-parsed]
        (let [[p & ps] parsers
                [strm prsd] (p stream)
            ]
            (recur strm ps stream prsd)
        )
    )
)

(defn- parse-digits-and-underscores [stream err-msg & parsers]
    (try
        (let [[strm last-strm prsd] 
                    (parse-digits-and-underscores' stream parsers nil nil)
                len (- (second (last prsd)) (ffirst prsd))
                bufs (for [[x] (take len last-strm)] x)
            ]
            [strm len bufs]
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream err-msg))
    ))
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
    (let [[strm len bufs] (parse-digits-and-underscores stream "expect decimal" 
                (ups/many1 (ups/expect-char-if digit-or-underscore))
            )
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

(defn- hexdigit-or-underscore [ch]
    (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 
        \a \b \c \d \e \f 
        \A \B \C \D \E \F
        \_} 
    ch)
)

(defn- hex-str->int [s len]
    (let [sb (StringBuilder. (+ 2 len))]
        (.append sb "0x")
        (doseq [x (for [y s :when (not= y \_)] y)]
            (.append sb x)
        )
        (read-string (str sb))
    )
)

(defn- jliteral-int-hex-parser [stream]
    (let [[strm len bufs] (parse-digits-and-underscores stream "expect decimal" 
                (ups/choice (ups/expect-string "0x") (ups/expect-string "0X"))
                (ups/many1 (ups/expect-char-if hexdigit-or-underscore))
            )
        ]
        (cond
            (= (first bufs) \_) (throw
                (ups/gen-ISE stream "hexadecimal cannot be led by '_'")
            )
            (= (last bufs) \_) (throw
                (ups/gen-ISE stream "hexadecimal cannot be followed by '_'")
            )
            :else [strm [:literal-int (hex-str->int bufs len)]]
        )
    )
)

(defn jliteral-int []
    (ups/choice
        (partial jliteral-int-hex-parser)
        (partial jliteral-int-dec-parser)
    )
)
