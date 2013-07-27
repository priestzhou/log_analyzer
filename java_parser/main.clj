(ns java-parser.main
    (:require 
        [utilities.parse :as ups]
    )
    (:gen-class)
)

(defn jblank [stream]
    (-> stream
        ((ups/expect-char-if #{\space \newline \tab}))
        (dissoc :parsed)
    )
)

(defn many-jblank [stream]
    (-> stream
        ((ups/many jblank))
        (dissoc :parsed)
    )
)

(defn many1-jblank [stream]
    (-> stream
        ((ups/many1 jblank))
        (dissoc :parsed)
    )
)

(defn jclass [stream]
    (
        (ups/chain
            many-jblank
            (ups/optional (ups/expect-string "public"))
            many1-jblank
            (ups/expect-string "class")
            many1-jblank
            (ups/expect-string "HelloWorld")
            many1-jblank
            (ups/expect-char \{)
            many-jblank
            (ups/expect-string "public")
            many1-jblank
            (ups/expect-string "static")
            many1-jblank
            (ups/expect-string "void")
            many1-jblank
            (ups/expect-string "main")
            many-jblank
            (ups/expect-char \()
            many-jblank
            (ups/expect-string "String")
            (ups/expect-string "[]")
            many1-jblank
            (ups/expect-string "args")
            many-jblank
            (ups/expect-char \))
            many-jblank
            (ups/expect-char \{)
            many-jblank
            (ups/expect-string "System.out.println")
            many-jblank
            (ups/expect-char \()
            many-jblank
            (ups/expect-char \")
            (ups/expect-string "Hello World")
            (ups/expect-char \")
            many-jblank
            (ups/expect-char \))
            many-jblank
            (ups/expect-char \;)
            many-jblank
            (ups/expect-char \})
            many-jblank
            (ups/expect-char \})
            many-jblank
            ups/expect-eof
        )
        stream
    )
)

(defn -main [& args]
    (->> args
        (first)
        (slurp)
        (ups/positional-stream)
        (jclass)
        (:parsed)
        (prn)
    )
)
