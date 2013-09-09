(ns clj-spark.spark.functions
  (:require
    [clj-spark.util]
    [serializable.fn :as sfn])
  (:import
    scala.Tuple2)
  (:use clojure.java.io)
)

;;; Helpers

(defn- serfn?
  [f]
  (= (type f) :serializable.fn/serializable-fn))

(def serialize-fn sfn/serialize)

(def deserialize-fn (memoize sfn/deserialize))

(def array-of-bytes-type (Class/forName "[B"))

;;; Generic

(defn -init
  "Save the function f in state"
  [f]
  [[] f])

(defn -init1
  "Save the function f in state"
  [f]
  (let [f1 (if (instance? array-of-bytes-type f)
            (deserialize-fn f)
            f)] 
  [[] f1]
  )
)

(defn -call
  [this & xs]
  ; A little ugly that I have to do the deser here, but I tried in the -init fn and it failed.  Maybe it would work in a :post-init?
  (let [
        fn-or-serfn (.state this)
        f (if (instance? array-of-bytes-type fn-or-serfn)
            (deserialize-fn fn-or-serfn)
            fn-or-serfn)
        t1 (println "defn -call")
        ]
    (apply f xs)
    ))


(defn -call1
  [this & xs]
  ; A little ugly that I have to do the deser here, but I tried in the -init fn and it failed.  Maybe it would work in a :post-init?
  (let [
        f (.state this)
        ]
    (apply f xs)
    ))

;;; Functions

(defn mk-sym
  [fmt sym-name]
  (symbol (format fmt sym-name)))

(defmacro gen-function
  [clazz wrapper-name]
  (let [new-class-sym (mk-sym "clj_spark.spark.functions.%s" clazz)
        prefix-sym (mk-sym "%s-" clazz)
        ]
    `(do
      (def ~(mk-sym "%s-init" clazz) -init)
      (def ~(mk-sym "%s-call" clazz) -call)
      (gen-class
        :name ~new-class-sym
        :extends ~(mk-sym "spark.api.java.function.%s" clazz)
        :prefix ~prefix-sym
        :init ~'init
        :state ~'state
        :constructors {[Object] []})
      (defn ~wrapper-name [f#]
        (new ~new-class-sym
          (if (serfn? f#) (serialize-fn f#) f#))))))

(println "start===")
(println (macroexpand-1 '(gen-function Function function)))

(println "===end")

(do 
  (def Function1-init clj-spark.spark.functions/-init1) 
  (def Function1-call clj-spark.spark.functions/-call1) 
  (clojure.core/gen-class 
    :name clj_spark.spark.functions.Function1 
    :extends spark.api.java.function.Function 
    :prefix Function1- 
    :init init 
    :state state 
    :constructors {[java.lang.Object] []}
  ) 
  (clojure.core/defn 
    function1 
    [f__133__auto__] 
    (new 
      clj_spark.spark.functions.Function1 
      (if (clj-spark.spark.functions/serfn? f__133__auto__) 
        (do 
          (println "test in function")
          (clj-spark.spark.functions/serialize-fn f__133__auto__)
        )
        f__133__auto__
      )
    )
  )
)

(do 
  (def Function-init clj-spark.spark.functions/-init) 
  (def Function-call clj-spark.spark.functions/-call) 
  (clojure.core/gen-class 
    :name clj_spark.spark.functions.Function 
    :extends spark.api.java.function.Function 
    :prefix Function- 
    :init init 
    :state state 
    :constructors {[java.lang.Object] []}
  ) 
  (clojure.core/defn 
    function 
    [f__133__auto__] 
    (new 
      clj_spark.spark.functions.Function 
      (if (clj-spark.spark.functions/serfn? f__133__auto__) 
        (do 
          (println "test in function")
          (clj-spark.spark.functions/serialize-fn f__133__auto__)
        )
        f__133__auto__
      )
    )
  )
)

;(gen-function Function function)

(gen-function VoidFunction void-function)

(gen-function Function2 function2)

(gen-function FlatMapFunction flat-map-function)

(gen-function PairFunction pair-function)

; Replaces the PairFunction-call defined by the gen-function macro.
(defn PairFunction-call
  [this x]
  (let [[a b] (-call this x)] (Tuple2. a b)))
