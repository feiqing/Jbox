package com.github.jbox.mybatis;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
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

    public static Where where() {
        return new Where();
    }

    public Where is(String field, Object value) {
        is.put(field, value);
        put(field, value);
        return this;
    }

    public Where isAll(@NotNull Map<String, Object> fields) {
        is.putAll(fields);
        putAll(fields);
        return this;
    }

    public Where like(String field, Object value) {
        like.put(field, value);
        put(field, value);
        return this;
    }

    public Where likeAll(@NotNull Map<String, Object> fields) {
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
        orderBy.put(field, asc);
        return this;
    }

    public Where orderByAll(@NotNull Map<String, Boolean> fields) {
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
