package com.alibaba.jbox.trace.tlog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import com.alibaba.jbox.trace.tlog.TlogFilter.TLogContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

import static ch.qos.logback.core.spi.FilterReply.DENY;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/23 08:15:00.
 */
public class LogBackHelper {

    private static final Logger tracer = LoggerFactory.getLogger("com.github.jbox.trace");

    public static Logger initTLogger(String loggerName, String filePath, String charset, String pattern, int maxHistory, long totalSizeCapKb,
                                     List<TlogFilter> filters) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(loggerName), "log name can't be empty!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(filePath), "log file can't be empty!");

        Logger logger = LoggerFactory.getLogger(loggerName);
        try {
            Class.forName("ch.qos.logback.classic.Logger");
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ch.qos.logback.classic.Logger tLogger = (ch.qos.logback.classic.Logger) logger;

                // init appender
                RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
                appender.setName(tLogger.getName());
                appender.setContext(tLogger.getLoggerContext());
                appender.setFile(filePath);
                appender.setAppend(true);

                // init policy
                TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
                policy.setParent(appender);
                policy.setFileNamePattern(filePath + TlogConstants.LOGGER_FILE_PATTERN);
                policy.setMaxHistory(maxHistory);
                if (totalSizeCapKb != 0) {
                    policy.setTotalSizeCap(new FileSize(totalSizeCapKb * FileSize.KB_COEFFICIENT));
                }
                policy.setContext(tLogger.getLoggerContext());
                policy.start();

                // init encoder
                PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                encoder.setPattern(pattern);
                encoder.setCharset(Charset.forName(charset));
                encoder.setContext(tLogger.getLoggerContext());
                encoder.start();

                // start appender
                appender.setRollingPolicy(policy);
                appender.setEncoder(encoder);
                /*  @since 1.2
                    registerBuildInFilters(appender);
                 */
                registerCustomFilters(appender, filters);
                appender.start();

                // start logger
                tLogger.detachAndStopAllAppenders();
                tLogger.setAdditive(false);
                tLogger.setLevel(Level.ALL);
                tLogger.addAppender(appender);
            } else {
                tracer.warn(
                        "app [{}] not used Logback impl for Log, please set '{}' logger in your logger context manual.",
                        System.getProperty("project.name", "unknown"),
                        loggerName);
            }
        } catch (ClassNotFoundException e) {
            tracer.warn("app [{}] not used Logback impl for Log, please set '{}' logger in your logger context manual.",
                    System.getProperty("project.name", "unknown"),
                    loggerName, e);
        }

        return logger;
    }

    private static void registerCustomFilters(RollingFileAppender<ILoggingEvent> appender, List<TlogFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        for (TlogFilter filter : filters) {
            appender.addFilter(new Filter<ILoggingEvent>() {
                @Override
                public FilterReply decide(ILoggingEvent event) {
                    TLogContext context = new TLogContext(event, event.getFormattedMessage());
                    TlogFilter.FilterReply reply = filter.decide(context);
                    if (reply == null) {
                        return DENY;
                    } else {
                        return FilterReply.valueOf(reply.name());
                    }
                }
            });
        }
    }

    /*
    // @since 1.2: 双11性能优化, 不再校验log内容来源
    private static void registerBuildInFilters(RollingFileAppender<ILoggingEvent> appender) {
        appender.addFilter(new Filter<ILoggingEvent>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                StackTraceElement[] callerData = event.getCallerData();
                if (callerData != null && callerData.length >= 1) {
                    if (LogEventParser.class.getName().equals(callerData[0].getClassName())) {
                        return FilterReply.NEUTRAL;
                    } else {
                        return DENY;
                    }
                }

                return FilterReply.NEUTRAL;
            }
        });
    }
    */
}
