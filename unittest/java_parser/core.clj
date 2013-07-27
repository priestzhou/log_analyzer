(ns unittest.java-parser.core
    (:use
        testing.core
        java-parser.core
        [utilities.parse :only [positional-stream]]
    )
    (:import
        utilities.parse.InvalidSyntaxException
    )
)

(suite "white space"
    (:fact whitespace-space
        (jwhitespace (positional-stream " "))
        :is
        [nil \space]
    )
    (:fact whitespace-tab
        (jwhitespace (positional-stream "\t"))
        :is
        [nil \tab]
    )
    (:fact whitespace-formfeed
        (jwhitespace (positional-stream "\f"))
        :is
        [nil \formfeed]
    )
    (:fact whitespace-newline
        (jwhitespace (positional-stream "\n"))
        :is
        [nil \newline]
    )
)

(suite "white space random"
    (:testbench
        (fn [test]
            (doseq [ch (range 128)]
                (when-not (#{\space \tab \formfeed \newline} ch)
                    (let [sb (doto (StringBuilder.) (.append ch))
                            s (str sb)
                        ]
                        (test s)
                    )
                )
            )
        )
    )
    (:fact whitespace-not-whitespace
        (fn [s]
            (jwhitespace (positional-stream s))
        )
        :throws
        InvalidSyntaxException
    )
)
