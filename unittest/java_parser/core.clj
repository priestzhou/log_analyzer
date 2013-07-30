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

(suite "decimal literal"
    (:fact literal-decimal-single-0
        (
            (jliteral-int)
            (positional-stream "0")
        )
        :is
        [[[:eof 1 1 2]] [:literal-int 0]]
    )
    (:fact literal-decimal-single-1
        (
            (jliteral-int)
            (positional-stream "1")
        )
        :is
        [[[:eof 1 1 2]] [:literal-int 1]]
    )
    (:fact literal-decimal-single-9
        (
            (jliteral-int)
            (positional-stream "9")
        )
        :is
        [[[:eof 1 1 2]] [:literal-int 9]]
    )
    (:fact literal-decimal-leading-nonzero
        (
            (jliteral-int)
            (positional-stream "10")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 10]]
    )
    (:fact literal-decimal-separated-by-underscores
        (
            (jliteral-int)
            (positional-stream "1__0")
        )
        :is
        [[[:eof 4 1 5]] [:literal-int 10]]
    )
    (:fact literal-decimal-no-leading-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "_1")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-decimal-no-tailing-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "1_")
            )
        )
        :throws InvalidSyntaxException
    )
)

(suite "hexadecimal literal"
    (:fact literal-hexadecimal-0
        (
            (jliteral-int)
            (positional-stream "0x0")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 0]]
    )
    (:fact literal-hexadecimal-big-X
        (
            (jliteral-int)
            (positional-stream "0X0")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 0]]
    )
    (:fact literal-hexadecimal-9
        (
            (jliteral-int)
            (positional-stream "0x9")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 9]]
    )
    (:fact literal-hexadecimal-a
        (
            (jliteral-int)
            (positional-stream "0xa")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 10]]
    )
    (:fact literal-hexadecimal-A
        (
            (jliteral-int)
            (positional-stream "0xA")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 10]]
    )
    (:fact literal-hexadecimal-f
        (
            (jliteral-int)
            (positional-stream "0xf")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 15]]
    )
    (:fact literal-hexadecimal-F
        (
            (jliteral-int)
            (positional-stream "0xF")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 15]]
    )
    (:fact literal-hexadecimal-separated-by-underscores
        (
            (jliteral-int)
            (positional-stream "0x1__0")
        )
        :is
        [[[:eof 6 1 7]] [:literal-int 16]]
    )
    (:fact literal-hexadecimal-no-leading-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "0x_1")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-hexadecimal-no-tailing-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "0x1_")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-hexadecimal-no-digits
        (fn []
            (
                (jliteral-int)
                (positional-stream "0x")
            )
        )
        :throws InvalidSyntaxException
    )
)

(suite "binary literal"
    (:fact literal-binary-0
        (
            (jliteral-int)
            (positional-stream "0b0")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 0]]
    )
    (:fact literal-binary-1
        (
            (jliteral-int)
            (positional-stream "0b1")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 1]]
    )
    (:fact literal-binary-big-B
        (
            (jliteral-int)
            (positional-stream "0B0")
        )
        :is
        [[[:eof 3 1 4]] [:literal-int 0]]
    )
    (:fact literal-binary-separated-by-underscores
        (
            (jliteral-int)
            (positional-stream "0b1__0")
        )
        :is
        [[[:eof 6 1 7]] [:literal-int 2]]
    )
    (:fact literal-binary-no-digits
        (fn []
            (
                (jliteral-int)
                (positional-stream "0b")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-binary-no-leading-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "0b_0")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-binary-no-tailing-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "0b0_")
            )
        )
        :throws InvalidSyntaxException
    )
)

(suite "octal literal"
    (:fact literal-octal-0
        (
            (jliteral-int)
            (positional-stream "00")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 0]]
    )
    (:fact literal-octal-1
        (
            (jliteral-int)
            (positional-stream "01")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 1]]
    )
    (:fact literal-octal-7
        (
            (jliteral-int)
            (positional-stream "07")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 7]]
    )
    (:fact literal-octal-separated-by-underscores
        (
            (jliteral-int)
            (positional-stream "01__0")
        )
        :is
        [[[:eof 5 1 6]] [:literal-int 8]]
    )
    (:fact literal-octal-leading-underscores
        (
            (jliteral-int)
            (positional-stream "0_10")
        )
        :is
        [[[:eof 4 1 5]] [:literal-int 8]]
    )
    (:fact literal-octal-no-tailing-underscores
        (fn []
            (
                (jliteral-int)
                (positional-stream "01_")
            )
        )
        :throws InvalidSyntaxException
    )
)

(suite "int literal"
    (:fact literal-int-empty
        (fn []
            (
                (jliteral-int)
                (positional-stream "")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-int-invalid-decimal
        (fn []
            (
                (jliteral-int)
                (positional-stream "1a")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-int-invalid-binary
        (fn []
            (
                (jliteral-int)
                (positional-stream "0b012")
            )
        )
        :throws InvalidSyntaxException
    )
    (:fact literal-int-invalid-octal
        (fn []
            (
                (jliteral-int)
                (positional-stream "018")
            )
        )
        :throws InvalidSyntaxException
    )
)
