package com.github.jbox.helpers;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-26 23:07:00.
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {

    T get() throws Exception;
}
