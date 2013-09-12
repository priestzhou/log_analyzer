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
import scala.collection.immutable.{HashMap=>smap }
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

class Filter extends java.io.Serializable 
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

class StatValue(val statRules:List[HashMap[String,String]]) extends java.io.Serializable {
    def getStat( groupKeys:HashMap[String,String],loglist:List[HashMap[String,Any]]) ={
      for( sr <- statRules) {
            val inkey = sr.get("statInKey").toString
            val vList = loglist.map(x=>x.get("inkey"))
            val outkey = sr.get("statOutKey").toString
            val funStr = sr.get("statFun").toString
            groupKeys.put(outkey,getFunc(funStr)(vList).toString)
        }
        groupKeys  
    }
    def getFunc( fun:String) ={
        fun match {
            case "count" => countfun _ 
            case "sum" => sumfun _
            case "uc" => ucfun _
            case "min" => minfun _
            case "max" => maxfun _
            case "first" => firstfun _
            case "last" => lastfun _
            case "avg" => avgfun _
        }     
    } 
    def countfun(t:List[Any]) = {
        t.length
    }
    def sumfun(t:List[Any]) = {
        t.map(_.asInstanceOf[Int]).reduce( _ + _ )
    }
    def ucfun(t:List[Any]) = {
        t.distinct.length
    }
    def minfun(t:List[Any]) = {
        t.map(_.asInstanceOf[Int]).min   
    }
    def maxfun(t:List[Any]) = {
        t.map(_.asInstanceOf[Int]).max
    }
    def firstfun(t:List[Any]) = {
        t.init
    }
    def lastfun(t:List[Any]) = {
        t.last
    }
    def avgfun(t:List[Any]) = {
        sumfun(t)/countfun(t)
    }    
} 

class Spark_engine (
    val searchRule: Map[String,Any],
    val rdd:RDD[HashMap[String, Any]]
    ) extends java.io.Serializable 
{

    println(searchRule.keySet)
    println(rdd)
    val eventRule = searchRule.get("eventRules").toString
    val nf = new Filter()
    println(eventRule)
    val rdd2 = rdd.filter(x=> 
            //x.get("message").toString.contains(eventRule)
            nf.eventFitler( x.get("message").toString,eventRule.toString )
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
    val statRules = searchRule.get("statRules").asInstanceOf[List[HashMap[String,String]]]

    val gk = new GenGroupkey(groupKeys)
    val groupRdd = rdd3.map(x=> (gk.getKey(x),x)).groupByKey

    val sv = new StatValue(statRules)
    val statRdd = groupRdd.map(x=>sv.getStat(x._1,x._2))

    def getFilterResult()={ rdd3 }
    def getGroupResult() = { 
        statRdd 
    }
}

class Spark_init (
    val master:String ="spark://10.144.44.18:7077",
    val jobname:String ="spark_engine",
    val sparkhome:String ="/home/hadoop/spark/",
    val input:String = "/home/hadoop/build/namenodelog_all"
    ) extends java.io.Serializable 
{
    val jarStrings = new Array[String](2)
    jarStrings.update(0,"./spark_scala_engine-0.1.jar") 
    jarStrings.update(1,"../demo1/target/lib/lift-json_2.8.0-2.1.jar") 
    
    val sc = new SparkContext(master,jobname,sparkhome,jarStrings)
    val rdd1 = sc.textFile(input).map(x=> new java.util.HashMap[String,Any](JsonParser.parse(x).values.asInstanceOf[smap[String,Any]].toMap) );

    def getInitRdd() = { rdd1 }
}
