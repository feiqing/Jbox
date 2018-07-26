package com.github.jbox.trace.nodes;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.trace.InvokerNode;
import com.github.jbox.trace.NodeContext;
import com.github.jbox.trace.Trace;
import com.github.jbox.trace.TraceException;
import com.github.jbox.utils.JboxUtils;
import com.google.common.base.Strings;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
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

    /**
     * use for replace @Trace when not use Trace annotation.
     * configKey:${className:methodName} -> TraceConfig
     */
    private ConcurrentMap<String, TraceConfig> traceConfigs = new ConcurrentHashMap<>();

    private static final Logger tracer = LoggerFactory.getLogger("com.github.jbox.trace");

    private static final ConcurrentMap<String, Logger> BIZ_LOGGERS = new ConcurrentHashMap<>();

    /**
     * used when method's implementation class has non default logger instance.
     */
    private Logger defaultBizLogger;

    private String configKeyPattern = "%s:%s";

    /**
     * determine the 'log method invoke cost time' append method param or not.
     */
    private volatile boolean param = true;

    /**
     * determine the 'log method invoke cost time' append method invoke result or not.
     */
    private volatile boolean result = true;

    /**
     * determine append 'com.github.jbox.trace' log or not.
     */
    private volatile boolean trace = false;

    public void setBizLoggerName(String bizLoggerName) {
        this.defaultBizLogger = LoggerFactory.getLogger(bizLoggerName);
    }


    public Logger getDefaultBizLogger() {
        return defaultBizLogger;
    }

    public void setDefaultBizLogger(Logger defaultBizLogger) {
        this.defaultBizLogger = defaultBizLogger;
    }


    public boolean isParam() {
        return param;
    }

    public void setParam(boolean param) {
        this.param = param;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }


    @Override
    public void invoke(NodeContext context) throws Throwable {
        long start = System.currentTimeMillis();
        context.next();
        String configKey = getConfigKey(context);
        Method method = context.getMethod();

        Object target = context.getTarget();
        Object[] args = context.getArgs();
        Object result = context.getResult();
        long rt = System.currentTimeMillis() - start;

        Object[] traceConfig = getTraceConfig(configKey, method.getAnnotation(Trace.class));
        if (isNeedLogger((Long) traceConfig[0], rt)) {
            String logContent = buildLogContent(method, rt, args, result);

            logBiz(logContent, configKey, method, target, (String) traceConfig[1]);
            logTrace(logContent);
        }
    }

    private String getConfigKey(NodeContext context) {
        String className = context.getClazz().getName();
        String methodName = context.getMethod().getName();
        return String.format(configKeyPattern, className, methodName);
    }

    /*** ***************************************************************************** ***/
    /***  user 'com.github.jbox.trace.TraceConfig' like as '@Trace' config @since 1.6 ***/
    /*** ***************************************************************************** ***/
    private Object[] getTraceConfig(String configKey, Trace trace) {

        long threshold;
        String loggerName;
        if (trace == null) {
            TraceConfig config = traceConfigs.computeIfAbsent(configKey, key -> new TraceConfig());
            threshold = config.getThreshold();
            loggerName = config.getLogger();
        } else {
            threshold = trace.threshold();
            loggerName = trace.value();
        }

        return new Object[]{threshold, loggerName};
    }


    /*** ******************************* ***/
    /***  append cost logger @since 1.1  ***/
    /*** ******************************* ***/

    private boolean isNeedLogger(long threshold, long rt) {
        return rt > threshold;
    }

    private String buildLogContent(Method method, long costTime, Object[] args, Object resultObj) {
        StringBuilder logBuilder = new StringBuilder(120);
        logBuilder
                .append("method: [")
                .append(JboxUtils.getSimplifiedMethodName(method))
                .append("] invoke rt [")
                .append(costTime)
                .append("]ms");

        if (isParam()) {
            logBuilder.append(", params:")
                    .append(Arrays.toString(args));
        }

        // @since 1.5
        if (isResult()) {
            logBuilder.append(", result: ")
                    .append(JSONObject.toJSONString(resultObj));
        }

        return logBuilder.toString();
    }

    private void logBiz(String logContent, String configKey, Method method, Object target, String loggerName) {
        Class<?> clazz = method.getDeclaringClass();
        Logger bizLogger = BIZ_LOGGERS.computeIfAbsent(configKey, key -> {
            try {
                if (Strings.isNullOrEmpty(loggerName)) {
                    return getDefaultBizLogger() != null ? getDefaultBizLogger() : getClassInnerLogger(clazz, target);
                } else {
                    return getNamedBizLogger(loggerName, clazz, target);
                }
            } catch (IllegalAccessException e) {
                throw new TraceException(e);
            }
        });

        if (bizLogger != null) {
            bizLogger.warn(logContent);
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

    private void logTrace(String logContent) {
        if (isTrace()) {
            tracer.info(logContent);
        }
    }

    public void setTraceConfigs(Map<String, TraceConfig> traceConfigs) {
        if (traceConfigs != null && !traceConfigs.isEmpty()) {
            this.traceConfigs.putAll(traceConfigs);
        }
    }

    public void addTraceConfig(String configKey, TraceConfig traceConfig) {
        this.traceConfigs.put(configKey, traceConfig);
    }

    public ConcurrentMap<String, TraceConfig> getTraceConfigs() {
        return traceConfigs;
    }

    public void setTraceConfigs(
            ConcurrentMap<String, TraceConfig> traceConfigs) {
        this.traceConfigs = traceConfigs;
    }

    private static class TraceConfig implements Serializable {

        private static final long serialVersionUID = -6182100093212660636L;

        /**
         * determine the 'method invoke cost time' logger(type {@code org.slf4j.Logger}) used.
         */
        private String logger = "";

        /**
         * method invoke total cost threshold, dependent logger config.
         * if ${method invoke cost time} > ${threshold} then append an 'cost time' log.
         */
        private long threshold = -1;

        public String getLogger() {
            return logger;
        }

        public void setLogger(String logger) {
            this.logger = logger;
        }

        public long getThreshold() {
            return threshold;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }
    }
}
