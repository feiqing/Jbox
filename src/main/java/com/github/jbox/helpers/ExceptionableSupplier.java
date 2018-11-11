package com.github.jbox.helpers;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/9 12:08 PM.
 */
@FunctionalInterface
public interface ExceptionableSupplier<T> {

    T get() throws Exception;
}
