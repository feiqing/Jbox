package com.github.jbox.mysql

import SqlProvider.hump2line
import com.google.common.base.Joiner
import org.reflections.ReflectionUtils.getAllFields
import org.slf4j.MDC

import java.lang.reflect.{Field, Modifier}
import java.util
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/5/10 10:41 上午.
 */
class SqlProvider {

  def find(where: Where) = s"SELECT * FROM $getTable ${getWhere(where)} ${getOrderBy(where)} ${getLimit(where)}"

  def findOne(where: Where): String =
    if (where.__like.nonEmpty)
      throw new RuntimeException("findOne() not support 'LIKE'")
    else
      find(where)

  def findByIds(ids: util.Collection[Long]): String =
    if (ids != null && !ids.isEmpty)
      s"SELECT * FROM $getTable WHERE id IN(" + Joiner.on(", ").join(ids) + ")"
    else
      throw new RuntimeException("ids is empty")

  def count(where: Where) = s"SELECT COUNT(*) FROM $getTable ${getWhere(where)}"

  def insert(obj: Any): String = {
    val colNames: ListBuffer[String] = ListBuffer[String]()
    colNames += "`gmt_create`"
    colNames += "`gmt_modified`"

    val colVals: ListBuffer[String] = ListBuffer[String]()
    colVals += "NOW()"
    colVals += "NOW()"

    for ((k, v) <- getFields(obj.getClass)) {
      colNames += k
      colVals += v
    }

    s"INSERT INTO $getTable (${colNames.mkString(",")}) VALUES(${colVals.mkString(", ")})"
  }

  def upsert(obj: Any): String = {
    val colNames: ListBuffer[String] = ListBuffer[String]()
    colNames += "`gmt_create`"
    colNames += "`gmt_modified`"

    val colVals: ListBuffer[String] = ListBuffer[String]()
    colVals += "NOW()"
    colVals += "NOW()"

    for ((k, v) <- getFields(obj.getClass)) {
      colNames += k
      colVals += v
    }

    s"""INSERT INTO $getTable(${colNames.mkString(",")})
       |VALUES(${colVals.mkString(", ")})
       |ON DUPLICATE KEY UPDATE ${(colNames zip colVals).tail.map(t => t._1 + " = " + t._2).mkString(", ")}
       |""".stripMargin
  }

  def updateById(obj: Any): String = {
    val sets =
      for ((k, v) <- getFields(obj.getClass))
        yield k + " = " + v

    s"UPDATE $getTable SET gmt_modified = NOW() ${if (sets.nonEmpty) ", " + sets.mkString(", ")} WHERE `id` = #{id}"
  }

  def delete(where: Where) = s"DELETE FROM $getTable ${getWhere(where)}"

  private def getWhere(where: Where): String = {
    val parts: ListBuffer[String] = ListBuffer[String]()
    parts ++= getIs(where)
    parts ++= getLike(where)
    if (parts.isEmpty)
      ""
    else
      "WHERE " + Joiner.on(" AND ").join(parts.asJava)
  }

  private def getIs(where: Where): List[String] = {
    if (where.__is.isEmpty) {
      return List()
    }
    where.__is.filter(_._2 != null).map(entry => String.format("`%s` = #{%s}", hump2line(entry._1), entry._1)).toList
  }

  private def getLike(where: Where): List[String] = {
    if (where.__like.isEmpty) {
      return List()
    }
    where.__like.filter(_._2 != null).map(entry => "`" + hump2line(entry._1) + "` LIKE '%${" + entry._1 + "}%'").toList
  }

  private def getLimit(where: Where): String = {
    val offset: Any = where.get("offset")
    val limit: Any = where.get("limit")
    if (offset == null && limit != null) {
      return "LIMIT #{limit}"
    }
    if (offset != null && limit != null) {
      return "LIMIT #{offset}, #{limit}"
    }
    if (offset == null) {
      return ""
    }
    throw new RuntimeException("un supported")
  }

  private def getOrderBy(where: Where): String = {
    if (where.__orderBy.isEmpty) {
      return ""
    }
    "ORDER BY " + Joiner.on(", ").join(
      where.__orderBy.map(tup => String.format("`%s` %s", hump2line(tup._1), if (tup._2) "ASC" else "DESC")).toList.asJava
    )
  }

  private val fields: ConcurrentMap[Class[_], mutable.Map[String, String]] = new ConcurrentHashMap[Class[_], mutable.Map[String, String]]

  private def getFields(clazz: Class[_]): mutable.Map[String, String] =
    fields
      .computeIfAbsent(clazz, _ => {
        val fields: mutable.Map[String, String] = new mutable.LinkedHashMap[String, String]
        getAllFields(clazz, field => {
          def foo(field: Field): Boolean = {
            if (field == null)
              return false
            if (Modifier.isStatic(field.getModifiers))
              return false
            if (Modifier.isFinal(field.getModifiers))
              return false
            !Array("id", "gmtCreate", "gmtModified").contains(field.getName)
          }

          foo(field)
        })
          .forEach(field => {
            val column: Column = field.getAnnotation(classOf[Column])
            val name: String = field.getName
            val col: String = if (column != null) column.value else hump2line(name)
            fields.put("`" + col + "`", "#{" + name + "}")
          })

        fields
      })


  private def getTable = MDC.get("#@table@#")
}

object SqlProvider {

  private val lines: ConcurrentMap[String, String] = new ConcurrentHashMap[String, String]

  private[mysql] def hump2line(hump: String): String = lines.computeIfAbsent(hump, _.replaceAll("[A-Z]", "_$0").toLowerCase)

}
