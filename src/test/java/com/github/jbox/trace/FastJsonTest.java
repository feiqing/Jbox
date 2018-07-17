package com.github.jbox.trace;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeFilter;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.github.jbox.domain.Address;
import com.github.jbox.domain.People;

import org.junit.Test;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/26 21:10:00.
 */
public class FastJsonTest {

    @Test
    public void testFilter() {
        Address addr = new Address("location", 1.1, 2.2);
        People people = new People("peo", addr);

        SerializeFilter pre = new PropertyPreFilter() {

            @Override
            public boolean apply(JSONSerializer serializer, Object object, String name) {
                System.out.println(name);
                return false;
            }
        };

        SerializeFilter filter = new PropertyFilter() {
            @Override
            public boolean apply(Object object, String name, Object value) {
                System.out.println(name);
                return false;
            }
        };

        SimplePropertyPreFilter filter1 = new SimplePropertyPreFilter("address");
        SerializeFilter[] filters = new SerializeFilter[] {filter, pre};
        String string = JSONObject.toJSONString(people, filter1);
        System.out.println(string);
    }
}
