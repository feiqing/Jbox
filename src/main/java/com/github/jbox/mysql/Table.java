package com.github.jbox.mysql;

import java.lang.annotation.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 3:28 下午.
 */
@Documented
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    
    String value();
}
