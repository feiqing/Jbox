package com.github.jbox.kafka;

import com.github.jbox.kafka.helper.ProducerManager;
import com.github.jbox.utils.Spm;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collections;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/5/20 10:33 AM.
 */
@Slf4j
public class KafkaProducers implements DisposableBean {

    private static final Spm.Type type = () -> "KafkaProducer";

    private String bootstrapServers;

    public KafkaProducers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void sendMessage(ProducerRecord<String, Object> msg) {
        long start = System.currentTimeMillis();
        ProducerManager.getProducer(msg.topic(), Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)).send(msg, (metadata, exception) -> {
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

    @Override
    public void destroy() {
        ProducerManager.stopAll();
    }
}
