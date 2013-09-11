package com.flyingsand.spark

import java.util.Map
import java.util.HashMap
import java.util.ArrayList
import spark._
import spark.SparkContext._
import spark.rdd._
import scala.util.parsing.json._
import net.liftweb.json._
import scala.collection.immutable.{HashMap=>smap }
import collection.JavaConverters._

/**
 * Hello world!
 *
 */
object App
{


    def main(args: Array[String]):Unit={
/*
        val jarStrings = new Array[String](2)
        jarStrings.update(0,"./spark_scala_engine-0.1.jar") 
        jarStrings.update(1,"../demo1/target/lib/lift-json_2.8.0-2.1.jar") 
        val sc = new SparkContext("spark://10.144.44.181:7077", "test",
            "/home/hadoop/spark/", jarStrings);
        //val bv = sc.broadcast(stoj)
        val t1 = sc.textFile("/home/hadoop/build/namenodelog_all")
        val t2 = t1.map(x=> JsonParser.parse(x))
        val t3 = t2.map(x=> (x\\("timestamp"),x) )
        val t4 = t3.groupByKey()

        println(  t4.count() );
        */
    val master ="spark://10.144.44.18:7077"
    val jobname ="spark_engine"
    val sparkhome="/home/hadoop/spark/"
    val input = "/home/hadoop/build/namenodelog_all"
   val jarStrings = new Array[String](2)
    jarStrings.update(0,"./spark_scala_engine-0.1.jar") 
    jarStrings.update(1,"../demo1/target/lib/lift-json_2.8.0-2.1.jar") 
    val sc = new SparkContext(master,jobname,sparkhome,jarStrings)
    val rdd1 = sc.textFile(input).map(x=> new java.util.HashMap[String,Any](JsonParser.parse(x).values.asInstanceOf[smap[String,Any]].toMap.asJava) );

    rdd1.cache
    println(rdd1.count)
        
        //Thread.sleep(100000)

        var rules = new HashMap[String,Any]() 
        rules.put("eventRules","(cmd)")
        var pr1 = new HashMap[String,String]
        pr1.put("key","test1")
        pr1.put("parser","cmd=([\\S]*)")
        var pr2 = new HashMap[String,String]
        pr2.put("key","test12")
        pr2.put("parser","ip=/([\\S]*)")        
        var prs = new ArrayList[HashMap[String,String]]
        prs.add(pr1)
        prs.add(pr2)
        rules.put("parseRules",prs)

        var gk = new ArrayList[String]
        gk.add("test1")
        gk.add("test12")
        //rules.put("groupKeys", gk)

        val se = new Spark_engine(rules,rdd1);
        val rdd = se.getFilterResult() ;
        println(rdd.count);
        println(rdd.first);

    }
    def stoj(s: String):Map[String,String]={
        JSON.parseFull(s).get.asInstanceOf[Map[String,String]]
    }



    


}


