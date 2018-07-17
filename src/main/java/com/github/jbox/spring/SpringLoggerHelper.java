package com.github.jbox.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/11/10 23:22:00.
 */
class SpringLoggerHelper {

    private static final Logger logger = LoggerFactory.getLogger("com.github.jbox.spring");

    static void info(String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(format, args);
        }
    }

    static void warn(String format, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(format, args);
        }
    }

    static void error(String format, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(format, args);
        }
    }
}
