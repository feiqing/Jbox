package com.github.jbox.trace.nodes;

import com.github.jbox.trace.NodeContext;
import com.github.jbox.trace.InvokerNode;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:33:00.
 */
public class SentinelInvokerNode implements InvokerNode {

    @Override
    public void invoke(NodeContext context) throws Throwable {
        context.next();
    }
}
