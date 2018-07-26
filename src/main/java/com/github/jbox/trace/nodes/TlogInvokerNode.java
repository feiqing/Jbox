package com.github.jbox.trace.nodes;

import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;
import com.github.jbox.trace.tlog.LogEvent;
import com.github.jbox.trace.tlog.TlogManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 07:49:00.
 */
@Data
@Slf4j
public class TlogInvokerNode implements InvokerNode {

    private String configKeyPattern = "%s:%s";

    private List<TlogManager> tlogManagers = new CopyOnWriteArrayList<>();

    @Override
    public void invoke(NodeContext context) throws Throwable {
        LogEvent logEvent = new LogEvent();
        try {
            logEvent.setMethod(context.getMethod());
            logEvent.setClassName(context.getClazz().getName());
            logEvent.setMethodName(context.getMethod().getName());
            logEvent.setConfigKey(getConfigKey(context));
            logEvent.setArgs(context.getArgs());

            logEvent.setStartTime(System.currentTimeMillis());
            context.next();
            logEvent.setRt(System.currentTimeMillis() - logEvent.getStartTime());

            logEvent.setResult(context.getResult());

        } catch (Throwable t) {
            logEvent.setException(t);
            throw t;
        } finally {
            sendLogEvent(logEvent);
        }
    }

    private AtomicBoolean isFirst = new AtomicBoolean(false);

    private void sendLogEvent(LogEvent event) {
        if (CollectionUtils.isEmpty(tlogManagers)) {
            if (isFirst.compareAndSet(false, true)) {
                log.warn("tlogManagers is empty.");
            }

            return;
        }

        event.init();
        for (TlogManager tlogManager : tlogManagers) {
            tlogManager.postLogEvent(event);
        }
    }

    private String getConfigKey(NodeContext context) {
        String className = context.getClazz().getName();
        String methodName = context.getMethod().getName();
        return String.format(configKeyPattern, className, methodName);
    }
}
