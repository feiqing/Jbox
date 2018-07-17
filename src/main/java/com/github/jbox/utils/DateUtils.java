package com.github.jbox.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author jifang.zjf@alibaba-inc.co
 * @version 1.1: 使用通用的{@code format(source, pattern)}重构.
 * @since 2016/5/24 18:35.
 */
public class DateUtils {

    private static final LoadingCache<String, ThreadLocal<DateFormat>> formatters = CacheBuilder
            .newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, ThreadLocal<DateFormat>>() {
                @Override
                public ThreadLocal<DateFormat> load(String pattern) {
                    return ThreadLocal.withInitial(() -> new SimpleDateFormat(pattern));
                }
            });

    public static String format(Object source, String pattern) {
        if (source == null) {
            return null;
        }

        return formatters.getUnchecked(pattern).get().format(source);
    }

    public static Date parse(String source, String pattern) throws ParseException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(source), "parse source can not be empty");

        try {
            return formatters.getUnchecked(pattern).get().parse(source);
        } catch (java.text.ParseException e) {
            throw new ParseException(e);
        }
    }

    /**
     * yyyy-MM-dd
     */
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    public static String dateFormat(Object source) {
        return format(source, DATE_PATTERN);
    }

    public static Date dateParse(String source) {
        return parse(source, DATE_PATTERN);
    }

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String timeFormat(Object source) {
        return format(source, TIME_PATTERN);
    }

    public static Date timeParse(String source) {
        return parse(source, TIME_PATTERN);
    }

    /**
     * yyyy-MM-dd HH:mm:ss,SSS
     */
    private static final String TIME_MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    public static String timeMillisFormat(Object source) {
        return format(source, TIME_MILLIS_PATTERN);
    }

    public static Date timeMillisParse(String source) {
        return parse(source, TIME_MILLIS_PATTERN);
    }

    /**
     * 将CheckedException {@link java.text.ParseException} 转换为UnCheckedException {@link com.github.jbox.utils.DateUtils.ParseException}
     */
    public static class ParseException extends RuntimeException {

        public ParseException(java.text.ParseException cause) {
            super(cause);
        }
    }
}
