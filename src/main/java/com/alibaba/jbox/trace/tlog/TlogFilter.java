package com.alibaba.jbox.trace.tlog;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/09/25 17:03:00.
 */
@FunctionalInterface
public interface TlogFilter {

    /**
     * determine the LogEvent do log or not.
     *
     * @param context : logging context {@link TLogContext}.
     * @return FilterReply  : {@link FilterReply}.
     */
    FilterReply decide(TLogContext context);

    /**
     * logging context information definition
     */
    class TLogContext {

        /**
         * logback  event.
         */
        private ILoggingEvent loggingEvent;

        /**
         * formatted log message.
         */
        private String fmtLogMsg;

        private Map<String, Object> extContext;

        public TLogContext(ILoggingEvent loggingEvent, String fmtLogMsg) {
            this.loggingEvent = loggingEvent;
            this.fmtLogMsg = fmtLogMsg;
        }

        public ILoggingEvent getLoggingEvent() {
            return loggingEvent;
        }

        public void setLoggingEvent(ILoggingEvent loggingEvent) {
            this.loggingEvent = loggingEvent;
        }

        public String getFmtLogMsg() {
            return fmtLogMsg;
        }

        public void setFmtLogMsg(String fmtLogMsg) {
            this.fmtLogMsg = fmtLogMsg;
        }

        public void putExtContext(String key, Object value) {
            if (extContext == null) {
                extContext = new HashMap<>();
            }
            extContext.put(key, value);
        }

        public Map<String, Object> getExtContext() {
            return extContext == null ? Collections.emptyMap() : extContext;
        }
    }

    enum FilterReply {
        /**
         * DENY: the logMsg will be dropped.
         */
        DENY,
        /**
         * NEUTRAL: then the next filter, if any, will be invoked.
         */
        NEUTRAL,
        /**
         * ACCEPT: the logMsg will be logged without consulting with other filters in the chain.
         */
        ACCEPT
    }
}
