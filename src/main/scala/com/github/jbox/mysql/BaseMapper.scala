package com.github.jbox.mysql

import org.apache.ibatis.annotations._

import java.util

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/5/10 10:27 上午.
 */
trait BaseMapper[T <: BaseDO] {

  def findAll: util.List[T] = find(Where.where)

  @SelectProvider(`type` = classOf[SqlProvider], method = "find")
  def find(where: Where): util.List[T]

  @SelectProvider(`type` = classOf[SqlProvider], method = "findOne")
  def findOne(where: Where): T

  def findById(id: Long): T = findOne(Where._is("id", id))

  @SelectProvider(`type` = classOf[SqlProvider], method = "findByIds")
  def findByIds(@Param("ids") ids: util.Collection[Long]): util.List[T]

  @SelectProvider(`type` = classOf[SqlProvider], method = "count")
  def count(where: Where): Long

  @InsertProvider(`type` = classOf[SqlProvider], method = "insert")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  def insert(entity: T): Integer

  @UpdateProvider(`type` = classOf[SqlProvider], method = "updateById")
  def updateById(t: T): Integer

  @DeleteProvider(`type` = classOf[SqlProvider], method = "delete")
  def delete(where: Where): Integer

  def deleteById(id: Long): Integer = delete(Where._is("id", id))
}
