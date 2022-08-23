package com.github.jbox.mybatis;

import java.lang.annotation.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 3:30 下午.
 */
@Documented
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * 指定: 属性对应的sql列
     * 默认: #属性名驼峰转下划线#
     *
     * @return
     */
    String name() default "";

    /**
     * Same as name()
     *
     * @return
     */
    String value() default "";

    /**
     * 指定: 是否排除当前属性
     * 默认: 否
     *
     * @return
     */
    boolean exclude() default false;
}
