package com.github.jbox.kafka;

import com.github.jbox.kafka.helper.ProducerManager;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.Spm;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collections;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/5/20 10:33 AM.
 */
@Slf4j
public class KafkaProducers implements DisposableBean {

    private static final Spm.Type type = () -> "KafkaProducer";

    private Map<String, Object> config;

    public KafkaProducers(String bootstrapServers) {
        this(Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    public KafkaProducers(Map<String, Object> config) {
        Preconditions.checkState(Collections3.isNotEmpty(config));
        Preconditions.checkState(config.containsKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        this.config = config;
    }

    public void sendMessage(ProducerRecord<String, Object> msg) {
        long start = System.currentTimeMillis();
        sendMessage(msg, (metadata, exception) -> {
            boolean success;
            if (exception != null) {
                success = false;
                log.error("send message error, topic:{} key:{}", msg.topic(), msg.key(), exception);
            } else {
                success = true;
                if (log.isDebugEnabled()) {
                    log.debug("send message success, topic:{} key:{}, meta:[{}:{}]", msg.topic(), msg.key(), metadata.partition(), +metadata.offset());
                }
            }
            Spm.log(type, msg.topic(), success, System.currentTimeMillis() - start);
        });
    }

    public void sendMessage(ProducerRecord<String, Object> msg, Callback callback) {
        ProducerManager
                .getProducer(msg.topic(), config)
                .send(msg, callback);
    }

    @Override
    public void destroy() {
        ProducerManager.stopAll();
    }
}
