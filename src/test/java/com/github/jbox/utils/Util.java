package com.github.jbox.utils;

/**
 * @author jifang.zjf
 * @since 2017/7/23 下午7:24.
 */
public class Util {

    public static void delay(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
