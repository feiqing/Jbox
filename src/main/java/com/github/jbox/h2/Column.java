package com.github.jbox.h2;

import com.github.jbox.serializer.ISerializer;

import java.lang.annotation.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 2:00 PM.
 */
@Documented
@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String type() default "";

    boolean nullable() default true;

    Class<? extends ISerializer> serializer() default ISerializer.class;
}
