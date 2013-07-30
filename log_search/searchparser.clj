(ns log-search.searchparser
    (:require 
        [clojure.string :as cs]
    )
)


(defn- splitStr [sStr]
    (cs/split sStr #"\|")
)

(defn- event-func [eStr]
    #(->>
        (re-find (re-pattern (cs/lower-case eStr)) (cs/lower-case %))
        nil?
        not
    )
)

(defn- parser-one [pStr]
    (let [pSeq (cs/split pStr #"\"" ) ;"
            secStr (second pSeq)
            lastStr (last pSeq)
            tKey (re-find #"(?<= as )[\S]+" lastStr)
        ]
        (println secStr)
        (println lastStr)
        (println tKey)
        {   
            :key
            tKey
            :parser 
            #(re-find 
                (re-pattern 
                    (str 
                        "(?<="
                        (cs/replace secStr #"\*" ")[.]*(?=")
                        ")"
                    )
                ) 
                %
            )
        }
    )
)

(defn- build-parse [sSeq pMap]
    (let [pStr (first sSeq)
            tailSeq (rest sSeq)
            plist (:parseRules pMap)
        ]
        (println (not (nil? pStr)))
        (println (re-find #"parse" pStr))
        (if (and (not (nil? pStr)) (re-find #"parse" pStr))
            (build-parse 
                tailSeq 
                (merge pMap 
                    {:parseRules (conj plist (parser-one pStr))}
                )
            )
            pMap
        )
    )
)

(defn sparser [sStr]
    (let [sSeq (splitStr sStr)
            eStr (first sSeq)
            tailSeq (rest sSeq)
        ]
        (if (empty? tailSeq)
            {:eventRules [(event-func eStr)]}
            (build-parse  
                tailSeq  
                (assoc {:eventRules [(event-func eStr)]} :parseRules []) 
            )
        )
    )
)

