package com.alibaba.jbox.job;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-28 20:57:00.
 */
public interface JobTask<T extends JobContext> extends Serializable {

    void invoke(T context) throws Throwable;

    default String desc(T context) {
        return this.getClass().getName();
    }
}
