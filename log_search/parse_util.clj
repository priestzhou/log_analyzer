(ns log-search.parse-util
    (:use
        [logging.core :only [defloggers]]
    )    
    (:require 
        [clojure.string :as cs]
        [serializable.fn :as sfn]
        [utilities.parse :as ups]
    )
    (:import
        utilities.parse.InvalidSyntaxException
    )    
)

(def whitespace #{\space \tab})

(def usersym #{\- \_ \= \. \/ \: })

(def parsersym #{\/ \- \_ \= \. \\ \" \: \, \? \< \( \) \] \[ \} \{ \+ \|})

(defn whitespaces [stream]
    (let [[strm parsed] (
                (ups/many1 (ups/expect-char-if ups/whitespace))
                stream
            )
        ]
        [strm [(ffirst parsed) (second (last parsed))]]
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

(defn jliteral-string-parser [stream]
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