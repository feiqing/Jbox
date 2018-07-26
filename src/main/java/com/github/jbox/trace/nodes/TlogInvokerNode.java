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

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 07:49:00.
 */
@Data
@Slf4j
public class TlogInvokerNode implements InvokerNode {

    private String configKeyPattern = "%s:%s";

    private List<TlogManager> tlogManagers;

    @Override
    public void invoke(NodeContext context) throws Throwable {
        LogEvent logEvent = new LogEvent();
        try {
            long start = System.currentTimeMillis();
            logEvent.setStartTime(start);
            logEvent.setMethod(context.getMethod());
            logEvent.setClassName(context.getClazz().getName());
            logEvent.setMethodName(context.getMethod().getName());
            logEvent.setConfigKey(getConfigKey(context));
            logEvent.setArgs(context.getArgs());

            context.next();
            logEvent.setResult(context.getResult());

            long rt = System.currentTimeMillis() - start;
            logEvent.setRt(rt);
        } catch (Throwable t) {
            logEvent.setException(t);
            throw t;
        } finally {
            sendLogEvent(logEvent);
        }
    }

    private void sendLogEvent(LogEvent event) {
        if (CollectionUtils.isEmpty(tlogManagers)) {
            log.warn("tlogManagers is empty.");
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

    public void addTLogManager(TlogManager tlogManager) {
        if (tlogManagers == null) {
            tlogManagers = new CopyOnWriteArrayList<>();
        }
        tlogManagers.add(tlogManager);
    }
}
