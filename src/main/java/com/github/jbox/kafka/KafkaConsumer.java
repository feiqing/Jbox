package com.github.jbox.kafka;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/29 2:13 PM.
 */
public interface KafkaConsumer {

    ConsumeStatus consume(ConsumerRecord<String, JSONObject> record) throws Throwable;

    String topic();

    int concurrence();

    Map<String, Object> config();

    default String group() {
        return "CID-" + topic();
    }
}
