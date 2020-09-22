package com.github.jbox.kafka.helper;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * todo: 不要删除, 被Kafka序列化使用!!!
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/6/10 9:57 AM.
 */
public class JsonSerializer implements Serializer<Object> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        return JSON.toJSONBytes(data);
    }

    @Override
    public void close() {
    }
}
