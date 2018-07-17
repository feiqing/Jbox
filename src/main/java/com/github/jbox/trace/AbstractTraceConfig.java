package com.github.jbox.trace;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.jbox.trace.tlog.TLogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/28 13:23:00.
 */
public abstract class AbstractTraceConfig implements Serializable {

    private static final long serialVersionUID = 8892376403020670231L;

    /**
     * use for replace @Trace when not use Trace annotation.
     * configKey:${className:methodName} -> TraceConfig
     */
    private ConcurrentMap<String, TraceConfig> traceConfigs = new ConcurrentHashMap<>();

    /**
     * used when method's implementation class has non default logger instance.
     */
    private Logger defaultBizLogger;

    /**
     * determine 'validate arguments' use or not.
     */
    private volatile boolean validator = false;

    /**
     * determine 'log method invoke cost time' use or not.
     */
    private volatile boolean elapsed = false;

    /**
     * determine the 'log method invoke cost time' append method param or not.
     */
    private volatile boolean param = true;

    /**
     * determine the 'log method invoke cost time' append method invoke result or not.
     */
    private volatile boolean result = true;

    /**
     * determine use sentinel for 'rate limit'.
     */
    private volatile boolean sentinel = false;

    /**
     * determine append 'com.github.jbox.trace' log or not.
     */
    private volatile boolean trace = false;

    /**
     * determine append root error log.
     */
    private volatile boolean errorRoot = false;

    /**
     * determine use tlog manager or not.
     */
    private volatile boolean enableTLogManger = true;

    /**
     * use for push TLog event.
     */
    private List<TLogManager> tLogManagers;

    /**
     * determine use abstract method for detach config/record or not.
     */
    private volatile boolean useAbstractMethod = false;

    public void setBizLoggerName(String bizLoggerName) {
        this.defaultBizLogger = LoggerFactory.getLogger(bizLoggerName);
    }

    public void settLogManager(TLogManager tLogManager) {
        if (tLogManagers == null) {
            tLogManagers = new CopyOnWriteArrayList<>();
        }
        tLogManagers.add(tLogManager);
    }

    public List<TLogManager> getTLogManagers() {
        if (tLogManagers == null) {
            return Collections.emptyList();
        }
        return tLogManagers;
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

    public Logger getDefaultBizLogger() {
        return defaultBizLogger;
    }

    public void setDefaultBizLogger(Logger defaultBizLogger) {
        this.defaultBizLogger = defaultBizLogger;
    }

    public boolean isValidator() {
        return validator;
    }

    public void setValidator(boolean validator) {
        this.validator = validator;
    }

    public boolean isElapsed() {
        return elapsed;
    }

    public void setElapsed(boolean elapsed) {
        this.elapsed = elapsed;
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

    public boolean isSentinel() {
        return sentinel;
    }

    public void setSentinel(boolean sentinel) {
        this.sentinel = sentinel;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    public boolean isErrorRoot() {
        return errorRoot;
    }

    public void setErrorRoot(boolean errorRoot) {
        this.errorRoot = errorRoot;
    }

    public List<TLogManager> gettLogManagers() {
        return tLogManagers;
    }

    public void settLogManagers(List<TLogManager> tLogManagers) {
        this.tLogManagers = tLogManagers;
    }

    public boolean isUseAbstractMethod() {
        return useAbstractMethod;
    }

    public void setUseAbstractMethod(boolean useAbstractMethod) {
        this.useAbstractMethod = useAbstractMethod;
    }

    public boolean isEnableTLogManger() {
        return enableTLogManger;
    }

    public void setEnableTLogManger(boolean enableTLogManger) {
        this.enableTLogManger = enableTLogManger;
    }
}
