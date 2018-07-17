package com.github.jbox.utils;

import java.nio.charset.StandardCharsets;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-23 19:33:00.
 */
public class HexStr {

    public static String toHexStr(byte bytes[], int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xff).toUpperCase());
        }

        return sb.toString();
    }

    public static byte[] fromHexStr(String hexStr) {
        if (hexStr.length() < 1) {
            return new byte[0];
        }

        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; ++i) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }

        return result;
    }

    public static byte[] getBytes(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] bytes, int size) {
        return new String(bytes, 0, size, StandardCharsets.UTF_8);
    }
}
