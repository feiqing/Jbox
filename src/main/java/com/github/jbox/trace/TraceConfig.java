package com.github.jbox.trace;

import java.io.Serializable;

/**
 * effect like as {@code com.github.jbox.trace.Trace} when not use {@code Trace} annotation.
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/5 16:27:00.
 */
public class TraceConfig implements Serializable {

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
