package com.github.jbox.utils;

import static com.github.jbox.utils.AESUtils.decode;
import static com.github.jbox.utils.AESUtils.encode;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-24 14:19:00.
 */
public class AESUtilsTest {
    public static void main(String[] args) {
        String encode = encode("1234567890123456", "123");
        System.out.println(encode);
        String decode = decode("1234567890123456", encode);
        System.out.println(decode);
    }
}
