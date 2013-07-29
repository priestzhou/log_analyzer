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
        (
            (jwhitespaces)
            (positional-stream " \t\f\n")
        )
        :is
        [[[:eof 4 2 1]] [0 4]]
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
            (
                (jwhitespaces)
                (positional-stream s)
            )
        )
        :throws
        InvalidSyntaxException
    )
)

(suite "comments"
    (:fact traditional-comment
        (
            (jcomment)
            (positional-stream "/*abc*/d")
        )
        :is
        [[[\d 7 1 8] [:eof 8 1 9]] [:comment-traditional 2 5]]
    )
    (:fact eol-comment
        (
            (jcomment)
            (positional-stream "//abc\nd")
        )
        :is
        [[[\d 6 2 1] [:eof 7 2 2]] [:comment-eol 2 5]]
    )
    (:fact eol-comment-eof
        (
            (jcomment)
            (positional-stream "//abc")
        )
        :is
        [[[:eof 5 1 6]] [:comment-eol 2 5]]
    )
)

(suite "identifier"
    (:fact identifier-match
        (
            (jidentifier)
            (positional-stream "a10_")
        )
        :is
        [[[:eof 4 1 5]] [:identifier "a10_"]]
    )
    (:fact identifier-unmatch
        (fn []
            (
                (jidentifier)
                (positional-stream "1a0_")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact identifier-star
        (fn []
            (
                (jidentifier)
                (positional-stream "*")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact identifier-eof
        (fn []
            (
                (jidentifier)
                (positional-stream "")
            )
        )
        :throws InvalidSyntaxException
    )
)

(suite "identifier-abs"
    (:fact identifier-abs-single
        (
            (jidentifier-abs)
            (positional-stream "a")
        )
        :is
        [[[:eof 1 1 2]] [:identifier-abs "a"]]
    )
    (:fact identifier-abs-multi
        (
            (jidentifier-abs)
            (positional-stream "a.b")
        )
        :is
        [[[:eof 3 1 4]] [:identifier-abs "a.b"]]
    )
)
