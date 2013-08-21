(ns unittest.log-search.searchparser
    (:use testing.core 
    )
    (:require
        [log-search.searchparser :as sp]
    )
)

(suite "check parser"
    (:fact parser-test1
        (sp/parse-all " *003 ")
        :is
        1
    )
    (:fact parser-test2
        (sp/parse-all "1970* | parse \":00,* INFO\" as test-parse-1 ")
        :is
        1
    )    
    (:fact parser-test3
        (sp/parse-all " 1970* | parse \":00,* INFO\" as parse-1    | parse \"hello*\" as parse-2 ")
        :is
        1
    )
    (:fact parser-test4
        (sp/parse-all " 1970*     | parse \"hello*\" as parse-2    | parse \":00,* INFO\" as parse-1 ")
        :is
        1
    )
    (:fact parser-test5
        (sp/parse-all " 970-01-01 | parse \":00,* INFO\" as parse-1    | parse \"hello *\" as parse-2|count b by parse-1" )
        :is
        1
    )

)

