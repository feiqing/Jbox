package com.alibaba.jbox.utils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/26 22:28:00.
 */
public class AopTargetUtilsTest {

    @Test
    public void test() {
        ConcurrentReferenceHashMap<String, Object> map
            = new ConcurrentReferenceHashMap<>();

        map.put("key", null);

        Object value = map.get("key");

        System.out.println(value);

        Assert.assertTrue(value == null);
    }
}
