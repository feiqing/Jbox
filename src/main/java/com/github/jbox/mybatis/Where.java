package com.github.jbox.mybatis;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 注意-目前尚不支持:
 * 1. IN查询: 由于需要写SQL脚本, 较为复杂, 因此暂时还不支持, 有需求可以提~
 * 2. BETWEEN查询
 * 3. >, >=, <, <=查询
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 3:46 下午.
 */
public class Where extends HashMap<String, Object> {

    private static final long serialVersionUID = 3829956445496228702L;

    @Getter
    private final Map<String, Object> is = new LinkedHashMap<>();

    @Getter
    private final Map<String, Object> like = new LinkedHashMap<>();

    @Getter
    private final Map<String, Boolean> orderBy = new LinkedHashMap<>();

    @Getter
    @Setter
    private boolean useNullValueAsIsNull = false;

    public static Where where() {
        return new Where();
    }

    public Where is(String field, Object value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(field));

        is.put(field, value);
        put(field, value);
        return this;
    }

    public Where isAll(Map<String, Object> fields) {
        Preconditions.checkNotNull(fields);

        is.putAll(fields);
        putAll(fields);
        return this;
    }

    public Where like(String field, Object value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(field));
        Preconditions.checkArgument(Objects.nonNull(value));

        like.put(field, value);
        put(field, value);
        return this;
    }

    public Where likeAll(Map<String, Object> fields) {
        Preconditions.checkNotNull(fields);

        like.putAll(fields);
        putAll(fields);

        return this;
    }

    public Where asc(String field) {
        return orderBy(field, true);
    }

    public Where desc(String field) {
        return orderBy(field, false);
    }

    public Where orderBy(String field, boolean asc) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(field));

        orderBy.put(field, asc);
        return this;
    }

    public Where orderByAll(Map<String, Boolean> fields) {
        Preconditions.checkNotNull(fields);

        fields.forEach(this::orderBy);

        return this;
    }

    public Where offset(int offset) {
        put("offset", offset);
        return this;
    }

    public Where limit(int limit) {
        put("limit", limit);
        return this;
    }
}
