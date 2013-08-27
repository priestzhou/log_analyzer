(ns unittest.kafka-hdfs.core
    (:use 
        testing.core
        kafka-hdfs.core
    )
    (:import
        [java.net URI]
    )
)

(suite "->uri: determine URI by timestamp and topic"
    (:fact gen-uri-case
        (let [base (URI/create "file://flyingsand.com/smile/")
            uri (gen-uri base "test" 1370098139466)
            ]
            (str uri)
        )
        :is
        "file://flyingsand.com/smile/2013-06-01/20130601T144859.466Z.test"
    )
)
