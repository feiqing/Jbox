package com.github.jbox.kafka.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.jbox.kafka.KafkaConsumer;
import com.github.jbox.utils.Collections3;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;

import static com.github.jbox.kafka.Configs.*;
import static com.github.jbox.kafka.KafkaConsumers.getConfig;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 1:53 PM.
 */
public class FailedLetterKafkaExchanger {

    private final ConcurrentMap<String, List<SynchronousQueue<ConsumerRecord<String, JSONObject>>>> transQueues = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject>> pullers = new ConcurrentHashMap<>();

    {
        ThreadManager.newThread("FailedLetterKafkaExchanger", "default", () -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            if (pullers.isEmpty()) {
                                Thread.sleep(DEFAULT_EXCHANGE_PULL_TIMEOUT);
                            }

                            for (org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject> puller : pullers.values()) {
                                ConsumerRecords<String, JSONObject> records = puller.poll(DEFAULT_EXCHANGE_PULL_TIMEOUT);
                                for (ConsumerRecord<String, JSONObject> record : records) {
                                    Letter letter = JSON.toJavaObject(record.value(), Letter.class);
                                    List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues = transQueues.get(letter.topic);
                                    if (Collections3.isEmpty(queues)) {
                                        continue;
                                    }
                                    queues.get(Math.abs(letter.partition) % queues.size()).put(new ConsumerRecord<>(letter.topic, letter.partition, letter.offset, letter.key, letter.value));
                                }
                            }
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                }
        );
    }

    public void addQueue(String topic, SynchronousQueue<ConsumerRecord<String, JSONObject>> queue) {
        this.addQueues(topic, Collections.singletonList(queue));
    }

    public void addQueues(String topic, List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues) {
        transQueues.put(topic, queues);
    }

    public void addLetter(KafkaConsumer bizConsumer, ConsumerRecord<String, JSONObject> msg) {
        String exchangeTopic = getConfig(bizConsumer, KAFKA_EXCHANGE_LETTER_TOPIC, DEFAULT_EXCHANGE_LETTER_TOPIC);
        String exchangeGroup = getConfig(bizConsumer, KAFKA_EXCHANGE_LETTER_GROUP, bizConsumer.group());
        ProducerManager.getProducer(exchangeTopic, bizConsumer.config()).send(new ProducerRecord<>(exchangeTopic, new Letter(msg)));
        pullers.computeIfAbsent(exchangeGroup, (_K) -> ConsumerManager.newConsumer(exchangeTopic, exchangeGroup, bizConsumer.config()));
    }

    @Data
    @NoArgsConstructor
    public static class Letter implements Serializable {

        private static final long serialVersionUID = 1383804694316363503L;

        public String topic;

        public int partition;

        public long offset;

        public String key;

        public JSONObject value;

        Letter(ConsumerRecord<String, JSONObject> msg) {
            this.topic = msg.topic();
            this.partition = msg.partition();
            this.offset = msg.offset();
            this.key = msg.key();
            this.value = msg.value();
        }
    }
}
