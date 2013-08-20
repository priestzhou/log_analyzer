(ns unittest.log-search.searchparser
    (:use testing.core 
    )
    (:require
        [log-search.searchparser :as sp]
    )
)

(suite "check parser"
    (:fact parser-test1
        (->>
            (sp/parse-all " test1 ")
            (filter map?)
            count
        )
        :is
        1
    )
    (:fact parser-test3
        (->>
            (sp/parse-all " ttt* ")
            (filter map?)
            count
        )
        :is
        1
    )
    (:fact parser-test4
        (->>
            (sp/parse-all " *test ")
            (filter map?)
            count
        )
        :is
        1
    )
    (:fact parser-test
        (->>
            (sp/parse-all " *test1* ")
            (filter map?)
            count
        )
        :is
        1
    )
)
