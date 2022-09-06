package com.github.jbox.mybatis;

import com.github.jbox.mybatis.provider._SqlProvider_;
import com.google.common.annotations.Beta;
import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 10:11 上午.
 */
public interface BaseMapper<ID extends Serializable, E extends BaseEntity<ID>> {

    /* select */

    default List<E> selectAll() {
        return select(Where.where());
    }

    @SelectProvider(type = _SqlProvider_.class, method = "select")
    List<E> select(Where where);

    @SelectProvider(type = _SqlProvider_.class, method = "select")
    E selectOne(Where where);

    default E selectById(@Param("id") ID id) {
        return selectOne(Where.where().is("id", id));
    }

    @SelectProvider(type = _SqlProvider_.class, method = "selectByIds")
    List<E> selectByIds(@Param("ids") Collection<ID> ids);

    /* selectMap */

    default List<Map<String, Object>> selectAllMap() {
        return selectMap(Where.where());
    }

    @SelectProvider(type = _SqlProvider_.class, method = "select")
    List<Map<String, Object>> selectMap(Where where);

    @SelectProvider(type = _SqlProvider_.class, method = "select")
    Map<String, Object> selectOneMap(Where where);

    default Map<String, Object> selectMapById(@Param("id") ID id) {
        return selectOneMap(Where.where().is("id", id));
    }

    @SelectProvider(type = _SqlProvider_.class, method = "selectByIds")
    List<Map<String, Object>> selectMapByIds(@Param("ids") Collection<ID> ids);

    /* count、exists */

    @SelectProvider(type = _SqlProvider_.class, method = "count")
    Long count(Where where);

    default boolean exists(Where where) {
        Long count = count(where);
        return count != null && count > 0;
    }

    /* insert、upsert */

    @InsertProvider(type = _SqlProvider_.class, method = "insert")
    @Options(useGeneratedKeys = true)
    Integer insert(E entity);

    @Beta // 目前仅适配了MySQL的语法
    @InsertProvider(type = _SqlProvider_.class, method = "upsert")
    @Options(useGeneratedKeys = true)
    Integer upsert(E entity);

    /* update */

    // 注意: update方法不会修改`id`字段
    @UpdateProvider(type = _SqlProvider_.class, method = "update")
    Integer update(@Param("entity") E entity, @Param("where") Where where);

    default Integer updateById(@Param("entity") E entity) {
        return update(entity, Where.where().is("id", entity.getId()));
    }

    @UpdateProvider(type = _SqlProvider_.class, method = "updateByIds")
    Integer updateByIds(@Param("entity") E entity, @Param("ids") Collection<ID> ids);

    /* delete */

    @DeleteProvider(type = _SqlProvider_.class, method = "delete")
    Integer delete(Where where);

    default Integer deleteById(@Param("id") ID id) {
        return delete(Where.where().is("id", id));
    }

    @DeleteProvider(type = _SqlProvider_.class, method = "deleteByIds")
    Integer deleteByIds(@Param("ids") Collection<ID> ids);
}
