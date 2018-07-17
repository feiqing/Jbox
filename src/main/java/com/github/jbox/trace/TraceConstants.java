package com.github.jbox.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/23 07:55:00.
 */
interface TraceConstants {

    Logger tracer = LoggerFactory.getLogger("com.github.jbox.trace");

    /**
     * append {@code %X{traceId}} in logback/log4j MDC.
     */
    String TRACE_ID = "traceId";

    String CONFIG_KEY_PATTERN = "%s:%s";
}
