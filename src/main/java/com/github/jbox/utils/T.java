package com.github.jbox.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-05 17:03
 */
public class T {

    private static final ConcurrentMap<String, ThreadLocal<DateFormat>> formatters = new ConcurrentHashMap<>();

    public static final long OneS = 1000;

    public static final long OneM = 60 * OneS;

    public static final long OneH = 60 * OneM;

    public static final long OneD = 24 * OneH;

    public static final long OneW = 7 * OneD;

    public static long ts(long timestamp) {
        return t(timestamp, Calendar.MILLISECOND);
    }

    public static long tm(long timestamp) {
        return t(timestamp, Calendar.SECOND, Calendar.MILLISECOND);
    }

    public static long th(long timestamp) {
        return t(timestamp, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND);
    }

    public static long td(long timestamp) {
        return t(timestamp, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND);
    }


    public static final String DATE_PATTERN = "yyyy-MM-dd";

    public static String dateFormat(Object source) {
        return format(source, DATE_PATTERN);
    }

    public static Date dateParse(String source) {
        return parse(source, DATE_PATTERN);
    }

    public static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static String timeFormat(Object source) {
        return format(source, TIME_PATTERN);
    }

    public static Date timeParse(String source) {
        return parse(source, TIME_PATTERN);
    }

    public static final String MILLIS_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

    public static String millisFormat(Object source) {
        return format(source, MILLIS_PATTERN);
    }

    public static Date millisParse(String source) {
        return parse(source, MILLIS_PATTERN);
    }

    public static String format(Object source, String pattern) {
        if (source == null) {
            return null;
        }

        return formatter(pattern).format(source);
    }

    public static Date parse(String source, String pattern) throws ParseException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(source), "parse source can not be empty");

        try {
            return formatter(pattern).parse(source);
        } catch (java.text.ParseException e) {
            throw new ParseException(e);
        }
    }

    // 将CheckedException 转换为UnCheckedException
    public static class ParseException extends RuntimeException {

        public ParseException(java.text.ParseException cause) {
            super(cause);
        }
    }

    private static long t(long ts, int... fields) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        for (int field : fields) {
            calendar.set(field, 0);
        }
        return calendar.getTimeInMillis();
    }

    private static DateFormat formatter(String pattern) {
        return formatters.computeIfAbsent(pattern, _K -> ThreadLocal.withInitial(() -> new SimpleDateFormat(pattern))).get();
    }

    public static void main(String[] args) {
        test1();
    }

    private static void test0() {
        long l = System.currentTimeMillis();
        System.out.println(millisFormat(l));
        System.out.println(millisFormat(ts(l)));
        System.out.println(millisFormat(tm(l)));
        System.out.println(millisFormat(th(l)));
        System.out.println(millisFormat(td(l)));
    }

    private static void test1() {
        long l = System.currentTimeMillis() - 14 * T.OneH;
        System.out.println(millisFormat(l));
        System.out.println(millisFormat(l));
        System.out.println(millisFormat(tm(l)));
        System.out.println(millisFormat(th(l)));
        System.out.println(millisFormat(td(l)));
    }
}
