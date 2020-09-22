package com.github.jbox.kafka.helper;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.utils.Collections3;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 2:40 PM.
 */
public class ConsumerManager {

    private static final List<KafkaConsumer<String, JSONObject>> consumers = new ArrayList<>();

    public static KafkaConsumer<String, JSONObject> newConsumer(String topic, String group, Map<String, Object> config) {
        if (Collections3.isEmpty(config) || !config.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new KafkaException("consumer.config() need [" + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + "] config");
        }

        Properties props = new Properties();
        /* 默认配置: 可通过consumer.config()覆盖 */

        // 两次poll之间的最大允许间隔: 请不要改得太大, 服务器会掐掉空闲连接, 不要超过30000
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 25000);

        //每次poll的最大数量: 该值不要改得太大, 如果poll太多数据, 而不能在下次poll之前消费完, 则会触发一次负载均衡, 产生卡顿
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);

        // 消息的反序列化方式
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.github.jbox.kafka.helper.JsonDeserializer");

        // 当前消费实例所属的消费组, 请在控制台申请之后填写: 属于同一个组的消费实例, 会负载消费消息
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group);

        // 加载自定义配置
        Collections3.nullToEmpty(config).forEach(props::put);

        KafkaConsumer<String, JSONObject> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(topic));

        consumers.add(consumer);

        return consumer;
    }

    public static void stopAll() {
        consumers.forEach(KafkaConsumer::close);
    }
}
