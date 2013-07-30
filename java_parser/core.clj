(ns java-parser.core
    (:use
        clojure.set
        utilities.core
    )
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
                (ups/many1 (ups/expect-char-if ups/whitespace))
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
        ]
        [strm [:identifier (ups/extract-string stream (- end start))]]
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


(defn- str->int [prefix s]
    (let [sb (StringBuilder.)]
        (.append sb prefix)
        (doseq [ch s :when (not= ch \_)]
            (.append sb ch)
        )
        (read-string (str sb))
    )
)

(defn- dec-str->int [s]
    (str->int "" s)
)

(defn- hex-str->int [s]
    (str->int "0x" s)
)

(defn- bin-str->int [s]
    (str->int "2r" s)
)

(defn- oct-str->int [s]
    (str->int "0" s)
)

(defn- parse-int-dec [s]
    (throw-if-not (reduce #(and %1 %2) true (map (union #{\_} ups/digit) s))
        InvalidSyntaxException. ""
    )
    (cond
        (empty? s) (throw (InvalidSyntaxException. "expect integer literal"))
        (= (first s) \_) (throw 
            (InvalidSyntaxException. "decimal cannot be led by '_'")
        )
        (= (last s) \_) (throw 
            (InvalidSyntaxException. "decimal cannot be followed by '_'")
        )
        :else (dec-str->int s)
    )
)

(defn- parse-int-hex [s]
    (throw-if-not (reduce #(and %1 %2) true (map (union #{\_} ups/hexdigit) s))
        InvalidSyntaxException. ""
    )
    (cond
        (empty? s) (throw (InvalidSyntaxException. "expect hexadecimal"))
        (= (first s) \_) (throw 
            (InvalidSyntaxException. "hexadecimal cannot be led by '_'")
        )
        (= (last s) \_) (throw
            (InvalidSyntaxException. "hexadecimal cannot be followed by '_'")
        )
        :else (hex-str->int s)
    )
)

(defn- parse-int-bin [s]
    (throw-if-not (reduce #(and %1 %2) true (map #{\_ \0 \1} s))
        InvalidSyntaxException. ""
    )
    (cond
        (empty? s) (throw
            (InvalidSyntaxException. "expect binary")
        )
        (= (first s) \_) (throw
            (InvalidSyntaxException. "binary cannot be led by '_'")
        )
        (= (last s) \_) (throw
            (InvalidSyntaxException. "binary cannot be followed by '_'")
        )
        :else (bin-str->int s)
    )
)

(defn- parse-int-oct [s]
    (throw-if-not (reduce #(and %1 %2) true (map #{\_ \0 \1 \2 \3 \4 \5 \6 \7} s))
        InvalidSyntaxException. ""
    )
    (cond
        (empty? s) (throw
            (InvalidSyntaxException. "expect octal")
        )
        (= (last s) \_) (throw
            (InvalidSyntaxException. "octal cannot be followed by '_'")
        )
        :else (oct-str->int s)
    )
)

(defn- jliteral-int-parser' [stream]
    (try
        (let [[strm [start end]] (
                    (ups/expect-string-while (union #{\_ \x \X \b \B} ups/hexdigit))
                    stream
                )
                s (ups/extract-string stream (- end start))
            ]
            (if (empty? s)
                (throw (InvalidSyntaxException. ""))
                (let [[x & xs] s]
                    (cond
                        (not= x \0) [strm (parse-int-dec s)]
                        (empty? xs) [strm 0]
                        :else (let [[y & ys] xs]
                            (cond
                                (#{\x \X} y) [strm (parse-int-hex ys)]
                                (#{\b \B} y) [strm (parse-int-bin ys)]
                                :else [strm (parse-int-oct xs)]
                            )
                        )
                    )
                )
            )
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream "expect integer"))
    ))
)

(defn- jliteral-int-parser-sign [stream]
    (let [[[ch] & strm] stream]
        (cond
            (= ch \+) [strm false]
            (= ch \-) [strm true]
            :else [stream false]
        )
    )
)

(defn- jliteral-int-parser [stream]
    (let [[strm1 neg] (jliteral-int-parser-sign stream)
            [strm2 int-value] (jliteral-int-parser' strm1)
            [strm3] (
                (ups/optional (ups/expect-char-if #{\l \L}))
                strm2
            )
        ]
        (if neg
            [strm3 [:literal-int (- int-value)]]
            [strm3 [:literal-int int-value]]
        )
    )
)

(defn jliteral-int []
    (partial jliteral-int-parser)
)


(defn- jliteral-float-exp-part []
    (ups/chain
        (ups/expect-char-if #{\e \E})
        (ups/optional (ups/expect-char-if #{\+ \-}))
        (ups/many1 (ups/expect-char-if ups/digit))
    )
)

(defn- jliteral-float-suffix []
    (ups/expect-char-if #{\f \F \d \D})
)

(defn- jliteral-float-decimal-parser [stream]
    (try
        (let [[strm] (
                    (ups/choice
                        (ups/chain
                            (ups/many1 (ups/expect-char-if ups/digit))
                            (ups/expect-char \.)
                            (ups/many (ups/expect-char-if ups/digit))
                            (ups/optional (jliteral-float-exp-part))
                            (ups/optional (jliteral-float-suffix))
                        )
                        (ups/chain
                            (ups/expect-char \.)
                            (ups/many (ups/expect-char-if ups/digit))
                            (ups/optional (jliteral-float-exp-part))
                            (ups/optional (jliteral-float-suffix))
                        )
                        (ups/chain
                            (ups/many1 (ups/expect-char-if ups/digit))
                            (jliteral-float-exp-part)
                            (ups/optional (jliteral-float-suffix))
                        )
                        (ups/chain
                            (ups/many1 (ups/expect-char-if ups/digit))
                            (ups/optional (jliteral-float-exp-part))
                            (jliteral-float-suffix)
                        )
                    )
                    stream
                )
                s (ups/extract-string-between stream strm)
            ]
            [strm [:literal-float s]]
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream "expect float-point number"))
    ))
)

(defn- jliteral-float-hexadecimal-parser [stream]
    (try
        (let [[strm] (
                    (ups/chain
                        (ups/choice (ups/expect-string "0x") (ups/expect-string "0X"))
                        (ups/choice 
                            (ups/chain 
                                (ups/many1 (ups/expect-char-if ups/hexdigit))
                                (ups/expect-char \.)
                                (ups/many (ups/expect-char-if ups/hexdigit))
                            )
                            (ups/chain
                                (ups/many (ups/expect-char-if ups/hexdigit))
                                (ups/expect-char \.)
                                (ups/many1 (ups/expect-char-if ups/hexdigit))
                            )
                            (ups/many1 (ups/expect-char-if ups/hexdigit))
                        )
                        (ups/expect-char-if #{\p \P})
                        (ups/optional (ups/expect-char-if #{\+ \-}))
                        (ups/many1 (ups/expect-char-if ups/digit))
                        (ups/optional (jliteral-float-suffix))
                    )
                    stream
                )
                s (ups/extract-string-between stream strm)
            ]
            [strm [:literal-float s]]
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream "expect float-point number"))
    ))
)

(defn jliteral-float []
    (ups/choice
        (partial jliteral-float-decimal-parser)
        (partial jliteral-float-hexadecimal-parser)
    )
)
