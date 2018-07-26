package com.github.jbox.trace.nodes;

import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;
import com.taobao.eagleeye.EagleEye;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import static com.github.jbox.trace.NodeContext.DEFAULT_TRACE_ID;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 1.0 - put traceId
 * @since 2018-07-26 07:36:00.
 */
public class EagleEyeInvokerNode implements InvokerNode {

    /**
     * append {@code %X{traceId}} in logback/log4j MDC.
     */
    private static final String TRACE_ID = "traceId";

    @Override
    public void invoke(NodeContext context) throws Throwable {
        String traceId = EagleEye.getTraceId();
        if (StringUtils.isNotBlank(traceId) && StringUtils.equals(traceId, DEFAULT_TRACE_ID)) {
            context.setTraceId(traceId);
            MDC.put(TRACE_ID, traceId);
        }

        try {
            context.next();
        } finally {
            if (StringUtils.isNotBlank(traceId)) {
                MDC.remove(TRACE_ID);
            }
        }
    }
}
