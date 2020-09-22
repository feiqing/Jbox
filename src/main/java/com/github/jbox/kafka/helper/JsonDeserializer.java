package com.github.jbox.kafka.helper;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * todo: 不要删除, 被Kafka反序列化使用!!!
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/6/10 3:09 PM.
 */
public class JsonDeserializer implements Deserializer<JSONObject> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public JSONObject deserialize(String topic, byte[] data) {
        return JSONObject.parseObject(data, JSONObject.class);
    }

    @Override
    public void close() {

    }
}
