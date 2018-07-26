package com.github.jbox.trace;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 07:22:00.
 */
public interface InvokerNode {

    void invoke(NodeContext context) throws Throwable;
}
