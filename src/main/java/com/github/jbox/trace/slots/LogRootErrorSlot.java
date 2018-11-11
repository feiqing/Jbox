package com.github.jbox.trace.slots;

import com.github.jbox.slot.JobSlot;
import com.github.jbox.trace.TraceSlotContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.jbox.utils.JboxUtils.getAbstractMethod;
import static com.github.jbox.utils.JboxUtils.getSimplifiedMethodName;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:51:00.
 */
public class LogRootErrorSlot implements JobSlot<TraceSlotContext> {

    private static final long serialVersionUID = -9004722041244327806L;

    private static final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Override
    public void invoke(TraceSlotContext context) throws Throwable {
        try {
            context.next();
        } catch (Throwable t) {
            rootLogger.error("method: [{}] invoke failed", getSimplifiedMethodName(getAbstractMethod(context.getJoinPoint())), t);
            throw t;
        }
    }
}
