package com.flyingsand.spark

import java.util.Map
import java.util.HashMap
import java.util.ArrayList
import java.util.List
import spark._
import spark.SparkContext._
import spark.rdd._
import spark.storage.StorageLevel
import scala.util.matching.Regex
import net.liftweb.json._
import scala.collection.JavaConversions._


/**
 * Hello world!
 *
 */

object T_regex extends java.io.Serializable  {
    def process(pstr:String, str: String): String = {
        val p = pstr.r
        p findFirstIn str match {
          case Some(p(d)) => return d
          case _ => return ""
        }
      }
}

object Filter extends java.io.Serializable 
{
    def eventFitler(s:String,str:String):Boolean={
        val flag = T_regex.process(str,s)
        if(""==flag){
            false
        }else{
            true
        }
    }
    
}

class Tparse(val parseRules:List[Map[String,String]]) extends java.io.Serializable {
    def parseFunc(log:HashMap[String,Any]) = {
        val message = log.get("message").toString ;
        //var mylog = log
        for( pr <- parseRules ) {
            //var key:String =_
            //var pv:String =_
            val (key, pv) = tparse(message,pr)
            log.put(key,pv)
        }
        log
    }

    def tparse(str: String, pr:Map[String,String]) = {
        val key = pr.get("key")
        val tp = pr.get("parser")
        val tr = T_regex.process(tp,str)
        (key, tr)
    }
}

class GenGroupkey(val groupKeys:List[String]) extends java.io.Serializable {
    def getKey(log:HashMap[String,Any]) = {
        var kmap = new HashMap[String,String]()
        for( key <- groupKeys) {
            val v = log.get(key).toString
            kmap.put(key,v)
        }
        kmap
    }
}
/*
class StatValue() extends java.io.Serializable {
    def getStat( (key, value)) ={
        (key,1)
    } 
} 
*/
class Spark_engine (
    val searchRule: Map[String,Any],
    val rdd:RDD[HashMap[String, Any]]
    ) extends java.io.Serializable 
{
    val eventRule = searchRule.get("eventRules").toString
    //val nf = new Filter(eventRule.toString)
    val rdd2 = rdd.filter(x=> 
            //x.get("message").toString.contains(eventRule)
            Filter.eventFitler( x.get("message").toString,eventRule.toString )
        ) 
    val parseRules = searchRule.get("parseRules").asInstanceOf[List[java.util.Map[String,String]]]
    var rdd3 :RDD[HashMap[String,Any]] =_
    if(parseRules !=null){
        val tp = new Tparse(parseRules)
        rdd3 = rdd2.map(x => tp.parseFunc(x))
    }
    else{
        rdd3 = rdd2
    }
    rdd3.persist(StorageLevel.MEMORY_AND_DISK)
    val groupKeys = searchRule.get("groupKeys").asInstanceOf[List[String]]
    val statRules = searchRule.get("statRules")
    //var statRdd:RDD[]=_
    //if(groupKeys !=null){
        val gk = new GenGroupkey(groupKeys)
        val groupRdd = rdd3.map(x=> (gk.getKey(x),x)).groupByKey
       //val statRdd = groupRdd.reduceByKey((x,y) => if(x) +1)
       //val sv = new StatValue()
       val statRdd = groupRdd.map(x=>(x._1,2) )
   // }
   // else{
   //     statRdd = null
   // }

    def getFilterResult()={ rdd3 }
    def getGroupResult() = { statRdd }
}
