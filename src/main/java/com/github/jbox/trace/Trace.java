package com.github.jbox.trace;

import java.lang.annotation.*;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.3
 * @since 2016/11/25 11:51.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Trace {

    /**
     * determine the 'method invoke cost time' logger(type {@code org.slf4j.Logger}) used.
     */
    String value() default "";

    /**
     * method invoke total cost threshold, dependent logger config.
     * if ${method invoke cost time} > ${threshold} then append an 'cost time' log.
     */
    long threshold() default 0;
}
