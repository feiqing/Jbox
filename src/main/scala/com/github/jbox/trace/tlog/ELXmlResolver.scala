package com.github.jbox.trace.tlog

import com.github.jbox.trace.TraceException
import com.github.jbox.trace.tlog.AbstractTlogConfig.ELConfig
import com.github.jbox.utils.JboxUtils
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.util
import java.util.function
import java.util.{List => JList, Map => JMap}
import scala.collection.JavaConverters.{mapAsScalaMapConverter, seqAsJavaListConverter}
import scala.collection.mutable
import scala.xml.{Elem, NodeSeq}

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/5/8 2:38 下午.
 */
class ELXmlResolver {

}

object ELXmlResolver {

  def resolveConfigFile(configFile: InputStream, methods: JMap[String, JList[ELConfig]], fileName: String): Unit = {
    val config = xml.XML.load(configFile)

    val define: Map[String, String] = (config \ "define")
      .map(_.asInstanceOf[Elem])
      .map(node => (node \ "@name", node \ "@value"))
      .map(t => (t._1.toString(), t._2.toString()))
      .toMap
    log("define", define, fileName)

    val refs = mutable.Map[String, String]()
    (config \ "trace")
      .map(_.asInstanceOf[Elem])
      .foreach(trace => {
        val method = getDefineStr((trace \ "@method").toString(), define)
        if ((trace \ "@ref").length != 0) {
          refs(method) = getDefineStr((trace \ "@ref").toString(), define)
        } else {
          resolveExpressions(method, trace \ "expression", methods)
        }
      })

    for ((k, v) <- refs) {
      methods.put(k, methods.computeIfAbsent(v, new function.Function[String, JList[ELConfig]] {
        override def apply(t: String): JList[ELConfig] = throw new TraceException(s"relative config '$t' is not defined.")
      }))
    }

    log("trace", methods.asScala.toMap, fileName)
  }

  private def resolveExpressions(method: String, expressions: NodeSeq, methods: JMap[String, JList[ELConfig]]): Unit = {
    expressions.map(_.asInstanceOf[Elem]).foreach(
      expr => {
        val key = (expr \ "@key").toString()
        val multi = expr \ "@multi"
        val config = if (multi == null && !multi.toString().toBoolean)
          new ELConfig(key, null)
        else
          new ELConfig(key, (expr \ "field").map(_.asInstanceOf[Elem]).map(e => (e \\ "@value").toString()).toList.asJava)

        methods
          .computeIfAbsent(method, new function.Function[String, JList[ELConfig]] {
            override def apply(t: String): JList[ELConfig] = new util.ArrayList[ELConfig]()
          })
          .add(config)
      }
    )
  }

  private def getDefineStr(origin: String, define: Map[String, String]): String = {
    val prefixIdx = origin.indexOf(TlogConstants.DEF_PREFIX)
    val suffixIdx = origin.indexOf(TlogConstants.DEF_SUFFIX)
    if (prefixIdx != -1 && suffixIdx != -1) {
      val ref = origin.substring(prefixIdx, suffixIdx + TlogConstants.DEF_SUFFIX.length())
      val trimRef = JboxUtils.trimPrefixAndSuffix(ref, TlogConstants.DEF_PREFIX, TlogConstants.DEF_SUFFIX)
      origin.replace(ref, define.getOrElse(trimRef, throw new TraceException("relative def '" + ref + "' is not defined.")))
    } else {
      origin
    }
  }

  private val logger = LoggerFactory.getLogger(classOf[ELXmlResolver])

  private def log(configType: String, configMap: Map[String, Any], configFile: String): Unit = {
    val max = configMap.maxBy(_._1.length)._1.length
    val content = configMap.toList.sortBy(_._1.length).map(t => String.format(s" %-${max + 2}s -> %s", t._1, t._2.asInstanceOf[Object])).mkString("\n")
    logger.info("read [{}] from '{}':\n{}", configType, configFile, content)
  }
}
