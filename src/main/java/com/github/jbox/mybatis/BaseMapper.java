package com.github.jbox.mybatis;

import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ToDo @date :  2022-06-27: 初步可用, 后面还需要继续测试
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 10:11 上午.
 */
public interface BaseMapper<ID extends Serializable, E extends BaseEntity<ID>> {

    default List<E> selectAll() {
        return select(Where.where());
    }

    @SelectProvider(type = SqlProvider.class, method = "find")
    List<E> select(Where where);

    // todo
    List<Map<String, Object>> selectMaps(Where where);
    // todo List<JSONObject> selectMaps(Where where);

    @SelectProvider(type = SqlProvider.class, method = "findOne")
    E selectOne(Where where);

    // todo
    Map<String, Object> selectMap(Where where);

    default E selectById(@Param("id") ID id) {
        return selectOne(Where.where().is("id", id));
    }

    @SelectProvider(type = SqlProvider.class, method = "findByIds")
    List<E> selectByIds(@Param("ids") Collection<ID> ids);

    @SelectProvider(type = SqlProvider.class, method = "count")
    Long count(Where where);

    default boolean exists(Where where) {
        final Long count = count(where);
        return count != null && count > 0;
    }

    @InsertProvider(type = SqlProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer insert(E entity);

    @InsertProvider(type = SqlProvider.class, method = "upsert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer upsert(E entity);

    @UpdateProvider(type = SqlProvider.class, method = "updateById")
    Integer updateById(E entity);

    // todo
    Integer update(E entity, Where where);

    @DeleteProvider(type = SqlProvider.class, method = "delete")
    Integer delete(Where where);

    default Integer deleteById(@Param("id") ID id) {
        return delete(Where.where().is("id", id));
    }

    // todo
    Integer deleteByIds(@Param("ids") Collection<ID> ids);
}
