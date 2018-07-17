package com.github.jbox.websocket;

import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-15 11:42:00.
 */
public interface Converters {
    Function<String, Object[]> LONG_ARGS_CONVERT = topic -> new Object[]{Long.valueOf(topic)};
}
