package com.github.jbox.kafka.helper;

import com.github.jbox.utils.Collections3;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 2:47 PM.
 */
public class ProducerManager {

    private static final ConcurrentMap<String, KafkaProducer<String, Object>> producers = new ConcurrentHashMap<>();

    public static KafkaProducer<String, Object> getProducer(String topic, Map<String, Object> config) {
        if (Collections3.isEmpty(config) || !config.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new KafkaException("need [" + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + "] config");
        }

        return producers.computeIfAbsent(topic, (_topic) -> {
            Properties props = new Properties();
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.github.jbox.kafka.helper.JsonSerializer");
            props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30 * 1000);
            Collections3.nullToEmpty(config).forEach(props::put);

            return new KafkaProducer<>(props);
        });
    }

    public static void stopAll() {
        for (KafkaProducer<String, Object> producer : producers.values()) {
            producer.flush();
            producer.close();
        }
    }
}
