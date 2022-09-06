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
     * 指定: Entity对应的数据库表
     * <br>默认: #类名驼峰转下划线#
     *
     * @return
     */
    String table() default "";

    /**
     * Same as name()
     *
     * @return
     */
    String value() default "";

    /**
     * 指定: Entity的主键Id生成策略
     * <br>默认: 使用数据库的AUTO_INCREMENT策略
     * <br>可选: 使用外部Sequence实例, 详见: SequenceRegistry.
     *
     * @return
     */
    String primaryKey() default PRI_AUTO_INCREMENT;

    /**
     * 指定: Entity的主键Id生成的SequenceId
     * <br>默认: 与table_name相同.
     *
     * @return
     */
    String sequenceId() default "";

    /**
     * 指定: 是否使用`gmt_create`列
     * <br>默认: 使用
     * <br>策略: insert、upsert时, 若未指定entity.gmtCreate属性值, 默认使用数据库`NOW()` Function.
     *
     * @return
     */
    boolean useGmtCreate() default true;

    /**
     * 指定: 是否存在`gmt_modified`列
     * <br>默认: 存在
     * <br>策略: insert、update、upsert时, 若未指定entity.gmtModified属性值, 默认使用数据库`NOW()` Function.
     *
     * @return
     */
    boolean useGmtModified() default true;

    String PRI_AUTO_INCREMENT = "__AUTO_INCREMENT__";

    String PRI_SEQUENCE_INSTANCE = "__SEQUENCE_INSTANCE__";

}
