package com.github.jbox.utils;

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

    public static Long ts(Long timestamp) {
        return timestamp - timestamp % OneS;
    }

    public static Long tm(Long timestamp) {
        return timestamp - timestamp % OneM;
    }

    public static Long th(Long timestamp) {
        return timestamp - timestamp % OneH;
    }

    public static long td(Long timestamp) {
        // GMT+8
        return timestamp - timestamp % OneD - 8 * OneH;
    }


    public static void main(String[] args) {
        long l = System.currentTimeMillis();
        System.out.println(DateUtils.timeMillisFormat(l));
        System.out.println(DateUtils.timeMillisFormat(ts(l)));
        System.out.println(DateUtils.timeMillisFormat(tm(l)));
        System.out.println(DateUtils.timeMillisFormat(th(l)));
        System.out.println(DateUtils.timeMillisFormat(td(l)));
    }

}
