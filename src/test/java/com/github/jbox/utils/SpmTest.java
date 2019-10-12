package com.github.jbox.utils;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/12 10:45 AM.
 */
public class SpmTest {

    private static Spm.Type test = () -> "test";

    public static void main(String[] args) {
        Spm.log(test, 48042, true, 11L, 8848);
    }
}
