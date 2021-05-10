package com.github.jbox.mysql

import com.github.jbox.mysql.TableInterceptor.{getMapperClass, getTableName}
import com.google.common.base.Strings
import org.aopalliance.intercept.{MethodInterceptor, MethodInvocation}
import org.apache.commons.lang3.StringUtils
import org.apache.ibatis.binding.MapperProxy
import org.slf4j.MDC
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/5/10 1:39 下午.
 */
class TableInterceptor extends MethodInterceptor {

  override def invoke(invocation: MethodInvocation): AnyRef = {
    MDC.put("#@table@#", getTableName(getMapperClass(invocation)))
    try {
      invocation.proceed
    } finally {
      MDC.remove("#@table@#")
    }
  }

}

object TableInterceptor {

  private lazy final val field_mapper_interface = {
    val field = ReflectionUtils.findField(classOf[MapperProxy[_]], "mapperInterface")
    ReflectionUtils.makeAccessible(field)
    field
  }

  @volatile
  private var field_h: Field = _

  private def getMapperClass(invocation: MethodInvocation) = {
    if (field_h == null) synchronized {
      val h = ReflectionUtils.findField(invocation.getThis.getClass, "h")
      ReflectionUtils.makeAccessible(h)
      field_h = h
    }
    val proxy = ReflectionUtils.getField(field_h, invocation.getThis).asInstanceOf[MapperProxy[_]]
    ReflectionUtils.getField(field_mapper_interface, proxy).asInstanceOf[Class[_]]
  }

  private final val tables = new ConcurrentHashMap[Class[_], String]

  private def getTableName(clazz: Class[_]) = tables.computeIfAbsent(clazz, _ => _getTable(clazz))

  private def _getTable(clazz: Class[_]): String = {
    val table = clazz.getAnnotation(classOf[Table])
    if (table != null) {
      if (Strings.isNullOrEmpty(table.value))
        throw new RuntimeException("no support")
      return table.value
    }

    val name = clazz.getSimpleName

    val mapper = StringUtils.indexOfIgnoreCase(name, "MAPPER")
    if (mapper != -1)
      return SqlProvider.hump2line(String.valueOf(name.charAt(0)).toLowerCase + StringUtils.substring(name, 1, mapper))

    val dao = StringUtils.indexOfIgnoreCase(name, "DAO")
    if (dao != -1)
      return SqlProvider.hump2line(String.valueOf(name.charAt(0)).toLowerCase + StringUtils.substring(name, 1, dao))

    throw new RuntimeException("no support")
  }
}
