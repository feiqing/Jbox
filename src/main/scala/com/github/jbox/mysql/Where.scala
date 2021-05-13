package com.github.jbox.mysql

import java.util.function.BiConsumer
import java.util.{Map => JMap}
import scala.beans.BeanProperty
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/5/10 10:42 上午.
 */
object Where {

  def where = new Where

  def _is(field: String, value: Any): Where = where.is(field, value)

  def _like(field: String, value: Any): Where = where.like(field, value)
}

@SerialVersionUID(3829956445496228702L)
@BeanProperty
class Where extends java.util.HashMap[String, Any] {

  private[mysql] val __is: mutable.Map[String, Any] = mutable.LinkedHashMap[String, Any]()

  private[mysql] final val __like: mutable.Map[String, Any] = mutable.LinkedHashMap[String, Any]()

  private[mysql] final val __orderBy: mutable.Map[String, Boolean] = mutable.LinkedHashMap[String, Boolean]()

  def is(field: String, value: Any): Where = {
    __is(field) = value
    put(field, value)
    this
  }

  def isAll(fields: JMap[String, Any]): Where = {
    __is ++= fields.asScala
    putAll(fields)
    this
  }

  def like(field: String, value: Any): Where = {
    __like.put(field, value)
    put(field, value)
    this
  }

  def likeAll(fields: JMap[String, Any]): Where = {
    __like ++= fields.asScala
    putAll(fields)
    this
  }

  def asc(field: String): Where = {
    orderBy(field, asc = true)
  }

  def desc(field: String): Where = {
    orderBy(field, asc = false)
  }

  def orderBy(field: String, asc: Boolean): Where = {
    __orderBy.put(field, asc)
    this
  }

  def orderByAll(fields: JMap[String, Boolean]): Where = {
    fields.forEach(new BiConsumer[String, Boolean] {
      override def accept(t: String, u: Boolean): Unit = orderBy(t, u)
    })
    this
  }

  def offset(offset: Int): Where = {
    put("offset", offset)
    this
  }

  def limit(limit: Int): Where = {
    put("limit", limit)
    this
  }
}
