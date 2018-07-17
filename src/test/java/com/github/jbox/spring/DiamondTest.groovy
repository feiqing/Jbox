package com.github.jbox.spring

import java.lang.reflect.Type

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/26 19:28:00.
 */
class DiamondTest extends GroovyTestCase {

    void testDiamondMethod() {
        String json = "[\"first\", \"second\"]"

        List<String> list = new ArrayList<>()
        Class<? extends List> clazz = list.getClass()
        Type genericSuperclass = clazz.getGenericSuperclass()

        Object obj = DynamicPropertySourcesPlaceholder.convertTypeValue(json, clazz, clazz)
        println obj
        assertTrue obj instanceof List && obj.size() == 2
    }
}
