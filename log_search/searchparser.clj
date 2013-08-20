(ns log-search.searchparser
    (:use
        [logging.core :only [defloggers]]
    )    
    (:require 
        [clojure.string :as cs]
        [serializable.fn :as sfn]
        [utilities.parse :as ups]
    )
    (:import
        utilities.parse.InvalidSyntaxException
    )    
)

(defloggers debug info warn error)

(def ^:private whitespace #{\space \tab})

(def ^:private usedsym #{\- \_ \= \. })


(defn- whitespaces [stream]
    (let [[strm parsed] (
                (ups/many1 (ups/expect-char-if whitespace))
                stream
            )
        ]
        [strm [(ffirst parsed) (second (last parsed))]]
    )
)

(defn- numberchar [stream]

)


(defn- parse-event-str [stream]
    (let [[strm parsed] (
                (ups/many1 
                    (ups/choice
                        (ups/expect-char-if ups/letter)
                        (ups/expect-char-if ups/digit)
                        (ups/expect-char-if usedsym)
                    )
                )
                stream
            )
            pStr (ups/extract-string-between stream strm)
        ]
        [strm pStr]
    )

)

(defn- parse-event-nostar [stream]
    (let [[strm parsed] (
                parse-event-str
                stream
            )
            efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case (str " " parsed " ") )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        [strm {:eventRules [efunc]}]
    )
)

(defn- parse-event-leftstar [stream]
    (let [[strm parsed] (
                (ups/chain
                    (ups/expect-char \*)
                    parse-event-str
                )
                stream
            )
            efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case (str (last parsed) " ") )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        [strm {:eventRules [efunc]}]
    )
)

(defn- parse-event-rightstar [stream]
    (let [[strm parsed] (
                (ups/chain
                    parse-event-str
                    (ups/expect-char \*)
                )
                stream
            )
            efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case (str " " (first parsed) ) )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        [strm {:eventRules [efunc]}]
    )
)

(defn- parse-event-bothstar [stream]
    (let [[strm parsed] (
                (ups/chain
                    (ups/expect-char \*)
                    parse-event-str
                    (ups/expect-char \*)
                )
                stream
            )
            efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case  (nth parsed 1)  )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        [strm {:eventRules [efunc]}]
    )
)

(defn- parse-event [stream]
    (let [[strm rst]
    ((ups/choice
            parse-event-bothstar
            parse-event-rightstar
            parse-event-leftstar
            parse-event-nostar            
        )
        stream
        )]
        [strm  rst]
    )   
        
)

(defn- parse-split [stream]
    (   (ups/chain
            (ups/expect-char \|)
            (ups/optional whitespaces)
        )
        stream
    )
)

(defn- parse-user-string [stream]
    (let [ [strm parsed] ((ups/between 
                    (ups/expect-char \")
                    (ups/expect-char \")
                    (ups/expect-no-eof)
                )
                stream
            )
            rst (ups/extract-string-between stream strm)
        ]
        [strm rst]
    )
)
                    ;
                    ;
                    ;
(defn- parse-string-parser [stream]
    (let [[strm parsed] (
                (ups/chain
                    parse-split
                    (ups/expect-string "parse")
                    whitespaces
                    parse-user-string
                    whitespaces
                    (ups/expect-string "as")
                    whitespaces
                    parse-event-str
                )
                stream
            )
            tKey (last parsed)
            parseStr (nth parsed 3)
        ]
    [strm  
        {            
            :key
            tKey
            :parser
            parseStr
        }
    ]
    )
)
 ;(ups/choice parse-string-parser parse-reg-parser)
(defn- parse-parsers [stream]
    (let [[strm parsed] ((ups/many 
                parse-string-parser    
            )
            stream
        )
        ]
        [strm parsed]
    )
)

(defn parse-all [inStr]
    (let [stream (ups/positional-stream inStr)
            [strm rst](
                (ups/chain 
                    whitespaces
                    parse-event
                    whitespaces
                    parse-parsers
                )
                stream
            )
        ]
    rst
    )
)


(defn- splitStr [sStr]
    (cs/split sStr #"\|")
)

(defn- event-func [eStr]
    (sfn/fn [inStr]
        (->>
            (re-find 
                (re-pattern (cs/lower-case (cs/trim eStr) )) 
                (cs/lower-case inStr)
            )
            nil?
            not
        )
    )
)

(def ^:private parser-fun-map
    {
        "parse" 
        (sfn/fn f [secStr]
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
        "reparse"
        identity
    }
)



(defn- parser-one [pStr ptag]
    (let [pSeq (cs/split pStr #"\"" ) ;"
            secStr (second pSeq)
            secSeq (cs/split secStr #"\*")
            lastStr (last pSeq)
            tKey (re-find #"(?<= as )[\S]+" lastStr)
        ]
        (println ptag)
        (println ((get parser-fun-map ptag) secStr))
        {   
            :key
            tKey
            :parser 
            #(re-find 
                (re-pattern 
                    ((get parser-fun-map ptag) secStr)
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
        (if (and 
                (not (nil? pStr)) 
                (or 
                    (re-find #"^[\s]*parse" pStr) 
                    (re-find #"^[\s]*reparse" pStr)
                )
            )
            (build-parse 
                tailSeq 
                (merge pMap 
                    {:parseRules 
                        (conj 
                            plist 
                            (parser-one 
                                pStr 
                                (re-find #"(?<=^[\s]*)[\S]+" pStr)
                            )
                        )
                    }
                )
            )
            pMap
        )
    )
)

(def ^:private wheresym #"=|<>|>|>=|<|<=|where")
(def ^:private wheresym2 #" = | <> | > | >= | < | <= ")

(defn- where-step [pStr]
    (let [pSeq (->> 
                (cs/split pStr wheresym)
                (map cs/trim)
                (filter #(< 0 (count %)))
                ) ;"
            secStr (second pSeq)
            firstStr (first pSeq)
            tag (re-find wheresym2 pStr)
        ]
        (println pSeq)
            (println secStr)
            (println firstStr)
            (println (read-string secStr))
        (sfn/fn f [log]

            (=
                (get log firstStr)
                secStr
            )
        )
        
    )
)

(defn- build-where [sSeq pMap]
    (let [pStr (first sSeq)
            tailSeq (rest sSeq)
            plist (:whereRules pMap)
        ]
        (if (and 
                (not (nil? pStr)) 
                (re-find #"^[\s]*where" pStr)
            )
            (build-where 
                tailSeq 
                (merge pMap 
                    {:whereRules (conj plist (where-step pStr ))}
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
            whereSeq (filter  #(re-find #"^[\s]*where" %) tailSeq)
        ]
        (if (empty? tailSeq)
            {:eventRules [(event-func eStr)]}
            (->>
                (build-parse  
                    tailSeq  
                    (assoc {:eventRules [(event-func eStr)]} :parseRules []) 
                )
                (#(build-where
                    whereSeq
                    (assoc % :whereRules [])
                ))
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

(def ^:private static-rules
    {
        "sum"
        (fn [l] 
            (reduce #(+ %1 (read-string %2)) 0 l)
        )
        "count"
        (fn [l] 
            (reduce (fn [a b] (+ a 1)) 0 l)
        )
    }
)

(defn- get-pSeq [sItem]
    (->>
        sItem
        (#(cs/split % #" "))
        (filter #(not (empty? %)) )
    )
)

(defn- static-one [pSeq]
    (let [c1 (count pSeq)
            funcStr (cs/lower-case (first pSeq))
            keyStr (last pSeq)
            statFun (get static-rules funcStr)
        ]
        (if (and (= c1 2) (not (nil? statFun)) )
            {
                :statInKey keyStr,
                :statFun statFun,
                :statOutKey (str funcStr "_" keyStr)
            }
            (println "the input format can't parse as static")
        )
    )
)

(defn- parse-static [gStr]
    (let [gSeq (cs/split gStr #" by ")
            sStr (first gSeq)
            sSeq (cs/split sStr #",")
            pSeqs (map get-pSeq sSeq)
        ]
        (map 
            static-one
            pSeqs
        )
    )
)

(defn- get-group-time [timeStep timeValue]
    (let [modTime (mod timeValue timeStep)
            groupTime (- timeValue modTime)
        ]
        (.format 
            (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
            groupTime
        )
    )
)

(def ^:private timeMap
    {
        "60" {:tw 60000,:tf #(get-group-time 5000 %)},
        "300" {:tw 300000,:tf #(get-group-time 10000 %)}
    }
)

(defn sparser 
    ([sStr]
        (debug "sparser sStr" sStr)
        (let [log-parser (log-table-parser sStr)
                sSeq (splitStr sStr)
                lastStr (last sSeq)
                gKeys (get-groupkey lastStr)
                statRules (parse-static lastStr)
            ]
            (debug "the log-parser" log-parser)
            (if (empty? gKeys)
                log-parser  
                (assoc log-parser :groupKeys gKeys :statRules statRules
                ) 
            )
        )
    )
    ([sStr timeWindow]
        (debug "str timeWindow in sparser" sStr timeWindow)
        (let [psr (sparser sStr)
                timeRule (get timeMap timeWindow)
                tw (:tw timeRule)
                startTime `(- 
                        (System/currentTimeMillis)
                        ~(eval tw)
                    )
            ]
            (debug "sparser s t let in")
            (assoc psr :timeRule (assoc timeRule :startTime startTime))
        )
    )
   ([sStr timeWindow startTime]
        (let [psr (sparser sStr)
                timeRule (assoc (get timeMap timeWindow) :startTime startTime)
            ]
            (assoc psr :timeRule timeRule)
        )
    )
)
