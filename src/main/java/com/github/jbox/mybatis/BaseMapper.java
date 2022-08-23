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

    default List<E> selectAll() {
        return select(Where.where());
    }

    @SelectProvider(type = _SqlProvider_.class, method = "select")
    List<E> select(Where where);

    // todo: 测试
    @SelectProvider(type = _SqlProvider_.class, method = "select")
    List<Map<String, Object>> selectMap(Where where);

    @SelectProvider(type = _SqlProvider_.class, method = "selectOne")
    E selectOne(Where where);

    // todo: 测试
    @SelectProvider(type = _SqlProvider_.class, method = "selectOne")
    Map<String, Object> selectOneMap(Where where);

    default E selectById(@Param("id") ID id) {
        return selectOne(Where.where().is("id", id));
    }

    @SelectProvider(type = _SqlProvider_.class, method = "selectByIds")
    List<E> selectByIds(@Param("ids") Collection<ID> ids);

    @SelectProvider(type = _SqlProvider_.class, method = "count")
    Long count(Where where);

    // todo: 测试
    default boolean exists(Where where) {
        Long count = count(where);
        return count != null && count > 0;
    }

    @InsertProvider(type = _SqlProvider_.class, method = "insert")
    @Options(useGeneratedKeys = true)
    Integer insert(E entity);

    // todo: 测试
    @UpdateProvider(type = _SqlProvider_.class, method = "update")
    Integer update(E entity, Where where);

    // todo: 测试
    default Integer updateById(E entity) {
        return update(entity, Where.where().is("id", entity.getId()));
    }

    // todo: 测试
    @UpdateProvider(type = _SqlProvider_.class, method = "updateByIds")
    Integer updateByIds(E entity, Where where);

//    @UpdateProvider(type = _SqlProvider.class, method = "updateById")
//    Integer updateById(E entity);

    // 目前仅适配了MySQL的语法
    @Beta
    @InsertProvider(type = _SqlProvider_.class, method = "upsert")
    @Options(useGeneratedKeys = true)
    Integer upsert(E entity);

    @DeleteProvider(type = _SqlProvider_.class, method = "delete")
    Integer delete(Where where);

    default Integer deleteById(@Param("id") ID id) {
        return delete(Where.where().is("id", id));
    }

    // todo: 测试
    @DeleteProvider(type = _SqlProvider_.class, method = "deleteByIds")
    Integer deleteByIds(@Param("ids") Collection<ID> ids);
}
