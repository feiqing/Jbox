package com.github.jbox.utils;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-06-01 19:18:00.
 */
public class Objects2 {

    public static <T> T nullToDefault(T nullable, T defaultValue) {
        return nullable == null ? defaultValue : nullable;
    }
}
