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
    (:fact literal-int-negative
        (
            (jliteral-int)
            (positional-stream "-1")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int -1]]
    )
    (:fact literal-int-positive
        (
            (jliteral-int)
            (positional-stream "+1")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 1]]
    )
    (:fact literal-int-long
        (
            (jliteral-int)
            (positional-stream "1l")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 1]]
    )
    (:fact literal-int-Long
        (
            (jliteral-int)
            (positional-stream "1L")
        )
        :is
        [[[:eof 2 1 3]] [:literal-int 1]]
    )
)

(suite "float-point literal"
    (:fact literal-float-decimal-0
        (
            (jliteral-float)
            (positional-stream "1.0")
        )
        :is
        [[[:eof 3 1 4]] [:literal-float "1.0"]]
    )
    (:fact literal-float-decimal-1
        (
            (jliteral-float)
            (positional-stream "1.0E1")
        )
        :is
        [[[:eof 5 1 6]] [:literal-float "1.0E1"]]
    )
    (:fact literal-float-decimal-2
        (
            (jliteral-float)
            (positional-stream "1.0E+1")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "1.0E+1"]]
    )
    (:fact literal-float-decimal-3
        (
            (jliteral-float)
            (positional-stream "10E-1")
        )
        :is
        [[[:eof 5 1 6]] [:literal-float "10E-1"]]
    )
    (:fact literal-float-decimal-4
        (
            (jliteral-float)
            (positional-stream ".1E1")
        )
        :is
        [[[:eof 4 1 5]] [:literal-float ".1E1"]]
    )
    (:fact literal-float-decimal-5
        (
            (jliteral-float)
            (positional-stream "1f")
        )
        :is
        [[[:eof 2 1 3]] [:literal-float "1f"]]
    )
    (:fact literal-float-decimal-6
        (
            (jliteral-float)
            (positional-stream "1F")
        )
        :is
        [[[:eof 2 1 3]] [:literal-float "1F"]]
    )
    (:fact literal-float-decimal-7
        (
            (jliteral-float)
            (positional-stream "1d")
        )
        :is
        [[[:eof 2 1 3]] [:literal-float "1d"]]
    )
    (:fact literal-float-decimal-8
        (
            (jliteral-float)
            (positional-stream "1D")
        )
        :is
        [[[:eof 2 1 3]] [:literal-float "1D"]]
    )
    (:fact literal-float-decimal-9
        (
            (jliteral-float)
            (positional-stream "1E1")
        )
        :is
        [[[:eof 3 1 4]] [:literal-float "1E1"]]
    )
    (:fact literal-float-decimal-9
        (
            (jliteral-float)
            (positional-stream "1.E1")
        )
        :is
        [[[:eof 4 1 5]] [:literal-float "1.E1"]]
    )
    (:fact literal-float-hexidecimal-0
        (
            (jliteral-float)
            (positional-stream "0x1P1")
        )
        :is
        [[[:eof 5 1 6]] [:literal-float "0x1P1"]]
    )
    (:fact literal-float-hexidecimal-1
        (
            (jliteral-float)
            (positional-stream "0x1p1")
        )
        :is
        [[[:eof 5 1 6]] [:literal-float "0x1p1"]]
    )
    (:fact literal-float-hexidecimal-2
        (
            (jliteral-float)
            (positional-stream "0x1.P1")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1.P1"]]
    )
    (:fact literal-float-hexidecimal-3
        (
            (jliteral-float)
            (positional-stream "0x1.aP1")
        )
        :is
        [[[:eof 7 1 8]] [:literal-float "0x1.aP1"]]
    )
    (:fact literal-float-hexidecimal-4
        (
            (jliteral-float)
            (positional-stream "0x.aP1")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x.aP1"]]
    )
    (:fact literal-float-hexidecimal-5
        (
            (jliteral-float)
            (positional-stream "0x1P+1")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P+1"]]
    )
    (:fact literal-float-hexidecimal-6
        (
            (jliteral-float)
            (positional-stream "0x1P-1")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P-1"]]
    )
    (:fact literal-float-hexidecimal-7
        (
            (jliteral-float)
            (positional-stream "0x1P1f")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P1f"]]
    )
    (:fact literal-float-hexidecimal-8
        (
            (jliteral-float)
            (positional-stream "0x1P1F")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P1F"]]
    )
    (:fact literal-float-hexidecimal-9
        (
            (jliteral-float)
            (positional-stream "0x1P1d")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P1d"]]
    )
    (:fact literal-float-hexidecimal-10
        (
            (jliteral-float)
            (positional-stream "0x1P1D")
        )
        :is
        [[[:eof 6 1 7]] [:literal-float "0x1P1D"]]
    )
)

(suite "literal char"
    (:fact literal-char-normal
        (
            (jliteral-char)
            (positional-stream "'a'")
        )
        :is
        [[[:eof 3 1 4]] [:literal-char \a]]
    )
    (:fact literal-char-escape-sequence-backspace
        (
            (jliteral-char)
            (positional-stream "'\\b'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \backspace]]
    )
    (:fact literal-char-escape-sequence-tab
        (
            (jliteral-char)
            (positional-stream "'\\t'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \tab]]
    )
    (:fact literal-char-escape-sequence-newline
        (
            (jliteral-char)
            (positional-stream "'\\n'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \newline]]
    )
    (:fact literal-char-escape-sequence-formfeed
        (
            (jliteral-char)
            (positional-stream "'\\f'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \formfeed]]
    )
    (:fact literal-char-escape-sequence-return
        (
            (jliteral-char)
            (positional-stream "'\\r'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \return]]
    )
    (:fact literal-char-escape-sequence-double-quote
        (
            (jliteral-char)
            (positional-stream "'\\\"'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \"]]
    )
    (:fact literal-char-escape-sequence-single-quote
        (
            (jliteral-char)
            (positional-stream "'\\''")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \']]
    )
    (:fact literal-char-escape-sequence-backslash
        (
            (jliteral-char)
            (positional-stream "'\\\\'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \\]]
    )
    (:fact literal-char-escape-sequence-octal-A
        (
            (jliteral-char)
            (positional-stream "'\\101'")
        )
        :is
        [[[:eof 6 1 7]] [:literal-char \A]]
    )
    (:fact literal-char-escape-sequence-octal-0
        (
            (jliteral-char)
            (positional-stream "'\\60'")
        )
        :is
        [[[:eof 5 1 6]] [:literal-char \0]]
    )
    (:fact literal-char-escape-sequence-octal-bel
        (
            (jliteral-char)
            (positional-stream "'\\7'")
        )
        :is
        [[[:eof 4 1 5]] [:literal-char \u0007]]
    )
    (:fact literal-char-escape-sequence-unicode
        (
            (jliteral-char)
            (positional-stream "'\\u0041'")
        )
        :is
        [[[:eof 8 1 9]] [:literal-char \A]]
    )
)

(suite "literal string"
    (:fact literal-string-empty
        (
            (jliteral-string)
            (positional-stream "\"\"")
        )
        :is
        [[[:eof 2 1 3]] [:literal-string ""]]
    )
    (:fact literal-string-normal
        (
            (jliteral-string)
            (positional-stream "\"a\"")
        )
        :is
        [[[:eof 3 1 4]] [:literal-string "a"]]
    )
    (:fact literal-string-escaped
        (
            (jliteral-string)
            (positional-stream "\"\\60\"")
        )
        :is
        [[[:eof 5 1 6]] [:literal-string "0"]]
    )
)

(suite "type"
    (:fact type-name
        (
            (jtype)
            (positional-stream "A")
        )
        :is
        [[[:eof 1 1 2]] [:type "A"]]
    )
    (:fact type-name-dotted
        (
            (jtype)
            (positional-stream "a.B")
        )
        :is
        [[[:eof 3 1 4]] [:type-inner "B" [:type "a"]]]
    )
    (:fact type-array
        (
            (jtype)
            (positional-stream "a[]")
        )
        :is
        [[[:eof 3 1 4]] [:type-array [:type "a"]]]
    )
    (:fact type-array-multi-dimension
        (
            (jtype)
            (positional-stream "a[][]")
        )
        :is
        [[[:eof 5 1 6]] [:type-array [:type-array [:type "a"]]]]
    )
    (:fact type-parameterized
        (
            (jtype)
            (positional-stream "A<B>")
        )
        :is
        [[[:eof 4 1 5]] [:type-parameterized [:type "A"] :parameters [[:type "B"]]]]
    )
    (:fact type-parameterized-inner-class
        (
            (jtype)
            (positional-stream "A<B>.C")
        )
        :is
        [[[:eof 6 1 7]] [:type-inner "C" 
            [:type-parameterized [:type "A"] :parameters [[:type "B"]]]
        ]]
    )
    (:fact type-parameterized-multi
        (
            (jtype)
            (positional-stream "A<B,C>")
        )
        :is
        [[[:eof 6 1 7]] [:type-parameterized [:type "A"] 
            :parameters [[:type "B"] [:type "C"]]
        ]]
    )
    (:fact type-parameterized-wildcard
        (
            (jtype)
            (positional-stream "A<?>")
        )
        :is
        [[[:eof 4 1 5]] [:type-parameterized [:type "A"]
            :parameters [[:type-var "?"]]
        ]]
    )
    (:fact type-parameterized-subtype
        (
            (jtype)
            (positional-stream "A<? extends B, C extends B>")
        )
        :is
        [[[:eof 27 1 28]] [:type-parameterized [:type "A"]
            :parameters [[:type-var "?" :extends [:type "B"]]
                [:type-var "C" :extends [:type "B"]]
            ]
        ]]
    )
    (:fact type-parameterized-supertype
        (
            (jtype)
            (positional-stream "A<? super B, C super B>")
        )
        :is
        [[[:eof 23 1 24]] [:type-parameterized [:type "A"]
            :parameters [[:type-var "?" :super [:type "B"]]
                [:type-var "C" :super [:type "B"]]
            ]
        ]]
    )
    (:fact type-parameterized-nested
        (
            (jtype)
            (positional-stream "A<B<C>>")
        )
        :is
        [[[:eof 7 1 8]]
            [:type-parameterized [:type "A"]
                :parameters [
                    [:type-parameterized [:type "B"]
                        :parameters [
                            [:type "C"]
                        ]
                    ]
                ]
            ]
        ]
    )
)
