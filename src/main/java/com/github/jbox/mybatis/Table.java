package com.github.jbox.mybatis;

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

    String name() default "";

    String sequence() default "__AUTO_INCREMENT__"; // if useSequence, default as name()

    boolean gmtCreate() default true;

    boolean gmtModified() default true;
}
