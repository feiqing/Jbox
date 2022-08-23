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

    /**
     * 指定: Entity对应的sql表
     * 默认: #类名驼峰转下划线#
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
     * 指定: Entity的主键Id生成策略
     * 默认: 使用数据库的AUTO_INCREMENT策略
     * 可选: 使用外部Sequence实例, 详见: SequenceRegistry.
     *
     * @return
     */
    String primaryKey() default PRI_AUTO_INCREMENT;

    String sequence() default "";

    /**
     * 指定: 是否存在`gmt_create`列
     * 默认: 存在
     * 策略: insert、upsert时, 若未指定entity.gmtCreate属性值, 默认使用数据库`NOW()` Function.
     *
     * @return
     */
    boolean gmtCreate() default true;

    /**
     * 指定: 是否存在`gmt_modified`列
     * 默认: 存在
     * 策略: insert、update、upsert时, 若未指定entity.gmtModified属性值, 默认使用数据库`NOW()` Function.
     *
     * @return
     */
    boolean gmtModified() default true;

    String idColumn() default "id";

    String gmtCreateColumn() default "gmt_create";

    String gmtModifiedColumn() default "gmt_modified";

    String PRI_AUTO_INCREMENT = "__AUTO_INCREMENT__";

    String PRI_SEQUENCE_INSTANCE = "__SEQUENCE_INSTANCE__";

}
