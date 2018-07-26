package com.github.jbox.trace;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-26 07:21:00.
 */
@Slf4j
@Data
public class NodeContext {

    public static final String DEFAULT_TRACE_ID = "-";

    private final List<InvokerNode> invokerNodes;

    private String traceId = DEFAULT_TRACE_ID;

    private ProceedingJoinPoint joinPoint;

    private Class<?> clazz;

    private Method method;

    private Object target;

    private Object[] args;

    private Object result;

    private int executedIndex = -1;

    private int executingIndex = -1;

    private Map<String, Object> attributes;

    NodeContext(List<InvokerNode> invokerNodes) {
        this.invokerNodes = invokerNodes;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        if (this.result == null) {
            this.result = result;
        } else {
            throw new IllegalStateException("result is already been set.");
        }
    }

    public Object getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(key, value);
    }

    public void next() throws Throwable {
        try {
            executingIndex++;
            if (executingIndex <= executedIndex) {
                throw new IllegalStateException(describe() + " has already been invoked: " + invokerNodes.get(executingIndex));
            }

            executedIndex++;
            if (executingIndex < invokerNodes.size()) {
                InvokerNode executor = invokerNodes.get(executingIndex);
                try {
                    logTrace("entering {}: {} ...", describe(), executor);
                    executor.invoke(this);
//                } catch (TraceException e) {
//                    throw e;
//                } catch (Throwable e) {
//                    throw new TraceException("failed to invoke " + describe() + ": " + executor, e);
                } finally {
                    logTrace("... exited {}: {}", describe(), executor);
                }
                if (executedIndex < invokerNodes.size() && executedIndex == executingIndex) {
                    logTrace("[{}] execution was interrupted by {}: {}", clazz.getName(), describe(), executor);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("[{}] reaches its end.", clazz.getName());
                }
            }
        } finally {
            executingIndex--;
        }
    }

    private void logTrace(String template, Object... args) {
        if (log.isTraceEnabled()) {
            log.trace(template, args);
        }
    }

    private String describe() {
        return "FiloValve[#" + (executingIndex + 1) + "/" + invokerNodes.size() + ",class=" + invokerNodes.get(executingIndex) + "]";
    }
}
