package com.github.jbox.serializer.support;

import com.alibaba.fastjson.JSON;
import com.github.jbox.serializer.AbstractSerializer;

import java.nio.charset.StandardCharsets;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 21:41:00.
 */
public class FastJsonSerializer extends AbstractSerializer {

    private Class<?> type;

    public FastJsonSerializer(Class<?> type) {
        this.type = type;
    }

    @Override
    protected byte[] doSerialize(Object obj) {
        String json = JSON.toJSONString(obj);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected Object doDeserialize(byte[] bytes) {
        String json = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        return JSON.parseObject(json, type);
    }
}
