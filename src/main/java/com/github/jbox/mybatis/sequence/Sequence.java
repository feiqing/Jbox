package com.github.jbox.mybatis.sequence;

import java.io.Serializable;
import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/7/11 14:52.
 */
@FunctionalInterface
public interface Sequence<ID extends Serializable> extends Function<String, ID> {

    ID nextVal(String sequenceName);

    @Override
    default ID apply(String sequenceName) {
        return nextVal(sequenceName);
    }
}
