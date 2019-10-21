package com.github.jbox.utils;

import java.util.Calendar;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-05 17:03
 */
public class T {

    public static final int OneS = 1000;

    public static final int OneM = 60 * OneS;

    public static final int OneH = 60 * OneM;

    public static final int OneD = 24 * OneH;

    public static final int OneW = 7 * OneD;

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

    private static long t(long ts, int... fields) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        for (int field : fields) {
            calendar.set(field, 0);
        }
        return calendar.getTimeInMillis();
    }


    public static void main(String[] args) {
        test1();
    }

    private static void test0() {
        long l = System.currentTimeMillis();
        System.out.println(DateUtils.timeMillisFormat(l));
        System.out.println(DateUtils.timeMillisFormat(ts(l)));
        System.out.println(DateUtils.timeMillisFormat(tm(l)));
        System.out.println(DateUtils.timeMillisFormat(th(l)));
        System.out.println(DateUtils.timeMillisFormat(td(l)));
    }

    private static void test1() {
        long l = System.currentTimeMillis() - 14 * T.OneH;
        System.out.println(DateUtils.timeMillisFormat(l));
        System.out.println(DateUtils.timeMillisFormat(ts(l)));
        System.out.println(DateUtils.timeMillisFormat(tm(l)));
        System.out.println(DateUtils.timeMillisFormat(th(l)));
        System.out.println(DateUtils.timeMillisFormat(td(l)));
    }
}
