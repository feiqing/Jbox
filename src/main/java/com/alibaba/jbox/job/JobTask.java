package com.alibaba.jbox.job;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-28 20:57:00.
 */
public interface JobTask<Input extends JobContext<Output>, Output> {

    Output invoke(Input context) throws Throwable;

    default String desc(Input context) {
        return this.getClass().getName();
    }
}
