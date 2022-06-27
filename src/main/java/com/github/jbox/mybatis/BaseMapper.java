package com.github.jbox.mybatis;

import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

/**
 * ToDo @date :  2022-06-27: 初步可用, 后面还需要继续测试
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 10:11 上午.
 */
public interface BaseMapper<E extends BaseEntity<?>> {

    default List<E> findAll() {
        return find(Where.where());
    }

    @SelectProvider(type = SqlProvider.class, method = "find")
    List<E> find(Where where);

    @SelectProvider(type = SqlProvider.class, method = "findOne")
    E findOne(Where where);

    default E findById(@Param("id") Object id) {
        return findOne(Where.where().is("id", id));
    }

    @SelectProvider(type = SqlProvider.class, method = "findByIds")
    List<E> findByIds(@Param("ids") Collection<Object> ids);

    @SelectProvider(type = SqlProvider.class, method = "count")
    Long count(Where where);

    @InsertProvider(type = SqlProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer insert(E entity);

    @InsertProvider(type = SqlProvider.class, method = "upsert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer upsert(E entity);

    @UpdateProvider(type = SqlProvider.class, method = "updateById")
    Integer updateById(E entity);

    @DeleteProvider(type = SqlProvider.class, method = "delete")
    Integer delete(Where where);

    default Integer deleteById(@Param("id") Object id) {
        return delete(Where.where().is("id", id));
    }
}
