package com.github.jbox.trace.nodes;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;
import com.github.jbox.trace.Trace;
import com.github.jbox.trace.TraceException;
import com.github.jbox.utils.JboxUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 1.1 logger invoke elapsed time & parameters
 * @since 2018-07-26 08:07:00.
 */
@Data
public class ElapsedLogInvokerNode implements InvokerNode {

    private static final Logger tracer = LoggerFactory.getLogger("com.github.jbox.trace");

    private Logger defaultBizLogger;

    private ConcurrentMap<String, Logger> bizLoggers = new ConcurrentHashMap<>();

    private boolean appendParam = true;

    private boolean appendResult = true;

    private boolean logTrace = false;

    @Override
    public void invoke(NodeContext context) throws Throwable {
        Method method = context.getMethod();
        Object target = context.getTarget();
        Object[] args = context.getArgs();

        long start = System.currentTimeMillis();
        context.next();
        long rt = System.currentTimeMillis() - start;

        Object result = context.getResult();
        
        Pair<String, Long> trace = getTrace(method);
        if (rt > trace.getRight()) {
            String logContent = buildLogContent(method, rt, args, result);

            logBiz(logContent, context.getClazz(), target, trace.getLeft());
            logTrace(logContent);
        }
    }

    private Pair<String, Long> getTrace(Method method) {
        Trace trace = method.getAnnotation(Trace.class);
        if (trace == null) {
            throw new TraceException("method:[" + method.getName() + "] not annotated by @Trace.");
        }

        return Pair.of(trace.value(), trace.threshold());
    }

    private String buildLogContent(Method method, long costTime, Object[] args, Object resultObj) {
        StringBuilder logBuilder = new StringBuilder(120);
        logBuilder
                .append("method: [")
                .append(JboxUtils.getSimplifiedMethodName(method))
                .append("] invoke rt [")
                .append(costTime)
                .append("]ms");

        if (appendParam) {
            logBuilder.append(", params:")
                    .append(JSONObject.toJSONString(args));
        }

        if (appendResult) {
            logBuilder.append(", result: ")
                    .append(JSONObject.toJSONString(resultObj));
        }

        return logBuilder.toString();
    }

    private void logBiz(String logContent, Class<?> clazz, Object target, String loggerName) {
        Logger bizLogger = bizLoggers.computeIfAbsent(clazz.getName(), key -> {
            try {
                if (StringUtils.isNotBlank(loggerName)) {
                    return getNamedBizLogger(loggerName, clazz, target);
                }

                return defaultBizLogger != null ? defaultBizLogger : getClassInnerLogger(clazz, target);
            } catch (IllegalAccessException e) {
                throw new TraceException(e);
            }
        });

        if (bizLogger != null) {
            bizLogger.warn(logContent);
        }
    }

    private void logTrace(String logContent) {
        if (logTrace) {
            tracer.info(logContent);
        }
    }

    private Logger getClassInnerLogger(Class<?> clazz, Object target) throws IllegalAccessException {
        Logger bizLogger = null;
        for (Field field : clazz.getDeclaredFields()) {
            if (Logger.class.isAssignableFrom(field.getType())) {
                if (bizLogger == null) {
                    field.setAccessible(true);
                    bizLogger = (Logger) field.get(target);
                } else {
                    throw new TraceException(
                            "duplicated field's type is 'org.slf4j.Logger', please specify the used Logger name in @Trace"
                                    + ".name()");
                }
            }
        }

        return bizLogger;
    }

    private Logger getNamedBizLogger(String loggerName, Class<?> clazz, Object target) {
        try {
            Field loggerField = ReflectionUtils.findField(clazz, loggerName);
            return (Logger) ReflectionUtils.getField(loggerField, target);
        } catch (IllegalStateException e) {
            throw new TraceException(
                    "not such 'org.slf4j.Logger' instance named [" + loggerName + "], in class [" + clazz.getName() + "]",
                    e);
        }
    }
}
