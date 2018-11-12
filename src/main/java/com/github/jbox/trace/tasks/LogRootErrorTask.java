package com.github.jbox.trace.tasks;

import com.github.jbox.job.JobTask;
import com.github.jbox.trace.TraceJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.jbox.utils.JboxUtils.getAbstractMethod;
import static com.github.jbox.utils.JboxUtils.getSimplifiedMethodName;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 08:51:00.
 */
public class LogRootErrorTask implements JobTask<TraceJobContext> {

    private static final long serialVersionUID = -9004722041244327806L;

    private static final Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Override
    public void invoke(TraceJobContext context) throws Throwable {
        try {
            context.next();
        } catch (Throwable t) {
            rootLogger.error("method: [{}] invoke failed", getSimplifiedMethodName(getAbstractMethod(context.getJoinPoint())), t);
            throw t;
        }
    }
}
