package com.github.jbox.hbase;

import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-05 14:09:00.
 */
@FunctionalInterface
public interface ObjectMapper<T> {

    T mapObject(String rowKey, Map<String/*family*/, Map<String/*qualifier*/, Object>> columnMap) throws Exception;
}
