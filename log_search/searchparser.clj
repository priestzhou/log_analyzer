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
        (re-find (re-pattern (cs/lower-case (cs/trim eStr) )) (cs/lower-case %))
        nil?
        not
    )
)

(defn- parser-one [pStr]
    (let [pSeq (cs/split pStr #"\"" ) ;"
            secStr (second pSeq)
            secSeq (cs/split secStr #"\*")
            lastStr (last pSeq)
            tKey (re-find #"(?<= as )[\S]+" lastStr)
        ]
        {   
            :key
            tKey
            :parser 
            #(re-find 
                (re-pattern 
                    (str 
                        "(?<="
                        (cs/replace 
                            secStr 
                            #"\*"
                            ")[\\\\S]*(?="    
                        )
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
        (if (and (not (nil? pStr)) (re-find #"^[\s]*parse" pStr))
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

(defn- log-table-parser [sStr]
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

(defn- get-groupkey [gStr]
    (let [gSeq (cs/split gStr #" by ")]
        (if (< 1 (count gSeq))
            (->>
                gSeq
                last
                (#(cs/split % #","))
                (map cs/trim )
            )
            []
        )
    )
)

(defn sparser [sStr]
    (let [log-parser (log-table-parser sStr)
            sSeq (splitStr sStr)
            lastStr (last sSeq)
            gKeys (get-groupkey lastStr)
        ]
        (if (empty? gKeys)
            log-parser  
            (assoc log-parser :groupKeys gKeys) 
        )
    )
)

