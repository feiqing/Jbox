package com.github.jbox.trace.nodes;

import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;
import com.github.jbox.utils.JboxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:51:00.
 */
public class LogRootErrorInvokerNode implements InvokerNode {

    private static final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Override
    public void invoke(NodeContext context) throws Throwable {
        try {
            context.next();
        } catch (Throwable t) {
            rootLogger.error("method: [{}] invoke failed, traceId:{}",
                    JboxUtils.getSimplifiedMethodName(JboxUtils.getAbstractMethod(context.getJoinPoint())),
                    context.getTraceId(), t);

            throw t;
        }
    }
}
