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

(defn- parse-octal-escape' [stream]
    (try 
        (
            (ups/choice
                (ups/chain
                    (ups/expect-char-if #{\0 \1 \2 \3})
                    (ups/expect-char-if #{\0 \1 \2 \3 \4 \5 \6 \7})
                    (ups/expect-char-if #{\0 \1 \2 \3 \4 \5 \6 \7})
                )
                (ups/chain
                    (ups/expect-char-if #{\0 \1 \2 \3 \4 \5 \6 \7})
                    (ups/expect-char-if #{\0 \1 \2 \3 \4 \5 \6 \7})
                )
                (ups/expect-char-if #{\0 \1 \2 \3 \4 \5 \6 \7})
            )
            stream
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream "expect octal escaped char"))
    ))
)

(defn- parse-octal-escape [stream]
    (let [[strm] (parse-octal-escape' stream)
            s (ups/extract-string-between stream strm)
        ]
        [strm [:literal-char (char (oct-str->int s))]]
    )
)

(defn- parse-unicode-char' [stream]
    (try (
            (ups/chain
                (ups/expect-char-if ups/hexdigit)
                (ups/expect-char-if ups/hexdigit)
                (ups/expect-char-if ups/hexdigit)
                (ups/expect-char-if ups/hexdigit)
            )
            stream
        )
    (catch InvalidSyntaxException _
        (throw (ups/gen-ISE stream "expect unicode char"))
    ))
)

(defn- parse-unicode-char [stream]
    (let [[strm] (parse-unicode-char' stream)
            s (ups/extract-string-between stream strm)
        ]
        [strm [:literal-char (char (hex-str->int s))]]
    )
)

(defn- parse-escaped-char [stream]
    (let [[[ch] & strm] stream]
        (cond
            (= ch :eof) (throw
                (ups/gen-ISE stream "expect escaped char")
            )
            (= ch \b) [strm [:literal-char \backspace]]
            (= ch \t) [strm [:literal-char \tab]]
            (= ch \n) [strm [:literal-char \newline]]
            (= ch \f) [strm [:literal-char \formfeed]]
            (= ch \r) [strm [:literal-char \return]]
            (= ch \") [strm [:literal-char \"]]
            (= ch \') [strm [:literal-char \']]
            (= ch \\) [strm [:literal-char \\]]
            (= ch \u) (parse-unicode-char strm)
            :else (parse-octal-escape stream)
        )
    )
)

(defn- parse-char [stream]
    (let [[[ch] & rest-stream] stream]
        (cond
            (= ch :eof) (throw
                (ups/gen-ISE stream "expect char")
            )
            (= ch \\) (parse-escaped-char rest-stream)
            :else [rest-stream [:literal-char ch]]
        )
    )
)

(defn- jliteral-char-parser [stream]
    (let [[strm1] (
                (ups/expect-char \')
                stream
            )
            [strm2 ch] (parse-char strm1)
            [strm3] (
                (ups/expect-char \')
                strm2
            )
        ]
        [strm3 ch]
    )
)

(defn jliteral-char []
    (partial jliteral-char-parser)
)


(defn- jliteral-string-parser' [sb stream]
    (let [[[ch] & strm1] stream]
        (cond
            (= ch :eof) (throw (ups/gen-ISE stream "open quoted string"))
            (= ch \") [strm1]
            :else (let [[strm2 [_ ch]] (parse-char stream)]
                (.append sb ch)
                (recur sb strm2)
            )
        )
    )
)

(defn- jliteral-string-parser [stream]
    (let [[strm1] (
                (ups/expect-char \")
                stream
            )
            sb (StringBuilder.)
            [strm2] (jliteral-string-parser' sb strm1)
        ]
        [strm2 [:literal-string (str sb)]]
    )
)

(defn jliteral-string []
    (partial jliteral-string-parser)
)


(defn- jtype-inner-parser [father-type stream]
    (let [[strm1 prsd1] (
                (ups/optional
                    (ups/chain
                        (jblank-many)
                        (ups/expect-char \.)
                        (jblank-many)
                    )
                )
                stream
            )
        ]
        (if-not prsd1
            [stream nil]
            (let [[strm2 [_ id]] ((jidentifier) strm1)]
                [strm2 [:type-inner id father-type]]
            )
        )
    )
)

(declare jtype)

(defn- jtype-var-parser [stream]
    (let [[strm1 prsd1] ((ups/optional (ups/expect-char \?)) stream)
            [strm2 prsd2] ((ups/optional (jidentifier)) stream)
        ]
        (cond 
            prsd1 [strm1 "?"]
            prsd2 [strm2 (second prsd2)]
            :else (throw (ups/gen-ISE stream "expect type var or wildcard here"))
        )
    )
)

(defn- jtype-var-bound-parser [stream]
    (let [[strm1 prsd1] ((ups/optional (ups/expect-string "extends")) stream)
            [strm2 prsd2] ((ups/optional (ups/expect-string "super")) stream)
        ]
        (cond
            prsd1 [strm1 :extends]
            prsd2 [strm2 :super]
            :else (throw (ups/gen-ISE stream "expect \"extends\" or \"super\""))
        )
    )
)

(defn- jtype-parameter-bound-parser [stream]
    (let [[strm [type-var _ bound _ type]] (
                (ups/chain
                    (partial jtype-var-parser)
                    (jblank-many1)
                    (partial jtype-var-bound-parser)
                    (jblank-many1)
                    (jtype)
                )
                stream
            )
        ]
        [strm [:type-var type-var bound type]]
    )
)

(defn- jtype-parameter-bound []
    (partial jtype-parameter-bound-parser)
)

(defn- jtype-parameter-wildcard-parser [stream]
    (let [[strm1 prsd1] ((ups/expect-char \?) stream)]
        [strm1 [:type-var "?"]]
    )
)

(defn- jtype-parameter-wildcard []
    (partial jtype-parameter-wildcard-parser)
)

(defn- jtype-single-argument-parser [stream]
    (let [[strm1 prsd1] (
                (ups/choice
                    (jtype-parameter-bound)
                    (jtype-parameter-wildcard)
                    (jtype)
                )
                stream
            )
        ]
        [strm1 prsd1]
    )
)

(defn- jtype-arguments-list-parser [result stream]
    (let [[strm1 prsd1] (jtype-single-argument-parser stream)
            [strm2 prsd2] (
                (ups/optional
                    (ups/chain
                        (jblank-many)
                        (ups/expect-char \,)
                        (jblank-many)
                    )
                )
                strm1
            )
            [strm3 prsd3] (
                (ups/optional
                    (ups/chain
                        (jblank-many)
                        (ups/expect-char \>)
                        (jblank-many)
                    )
                )
                strm1
            )
        ]
        (cond
            prsd2 (recur (conj result prsd1) strm2)
            prsd3 [strm3 (conj result prsd1)]
            :else (throw (ups/gen-ISE strm1 
                    "expect ',' to separate type parameters or '>' to end type parameters"
                )
            )
        )
    )
)

(defn- jtype-arguments-parser [father-type stream]
    (let [[strm1 prsd1] (
                (ups/optional
                    (ups/chain
                        (jblank-many)
                        (ups/expect-char \<)
                        (jblank-many)
                    )
                )
                stream
            )
        ]
        (if-not prsd1
            [stream nil]
            (let [
                    [strm2 prsd2] (jtype-arguments-list-parser [] strm1)
                ]
                [strm2 [:type-parameterized father-type :parameters prsd2]]
            )
        )
    )
)

(defn- jtype-specifier-parser' [father-type stream]
    (let [
            [strm1 prsd1] (jtype-inner-parser father-type stream)
            [strm2 prsd2] (jtype-arguments-parser father-type stream)
        ]
        (cond
            prsd1 (recur prsd1 strm1)
            prsd2 (recur prsd2 strm2)
            :else [stream father-type]
        )
    )
)

(defn- jtype-specifier-parser [stream]
    (let [[strm1 [_ id1]] ((jidentifier) stream)
            this-type [:type id1]
        ]
        (jtype-specifier-parser' this-type strm1)
    )
)

(defn- gen-array-structure [base depth]
    {
        :pre [
            (not (neg? depth))
        ]
    }
    (if (zero? depth)
        base
        (recur [:type-array base] (dec depth))
    )
)

(defn- jtype-parser [stream]
    (let [[strm1 prsd1] (jtype-specifier-parser stream)
            [strm2 prsd2] (
                (ups/many
                    (ups/chain
                        (jblank-many)
                        (ups/expect-char \[)
                        (jblank-many)
                        (ups/expect-char \])
                    )
                )
                strm1
            )
        ]
        [strm2 (gen-array-structure prsd1 (count prsd2))]
    )
)

(defn jtype []
    (partial jtype-parser)
)
