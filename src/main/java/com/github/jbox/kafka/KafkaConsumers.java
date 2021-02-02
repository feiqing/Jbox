package com.github.jbox.kafka;

import com.alibaba.fastjson.JSONObject;
import com.github.jbox.kafka.helper.*;
import com.github.jbox.spring.LazyInitializingBean;
import com.github.jbox.utils.Collections3;
import com.github.jbox.utils.Objects2;
import com.github.jbox.utils.Spm;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

import static com.github.jbox.kafka.Configs.*;


/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/29 2:14 PM.
 */
@Slf4j
public class KafkaConsumers implements LazyInitializingBean, DisposableBean {

    private static final Spm.Type type = () -> "KafkaConsumer";

    private static final Set<String> topics = new HashSet<>();

    private static final FailedLetterLocalExchanger localExchanger = new FailedLetterLocalExchanger();

    private static final FailedLetterKafkaExchanger kafkaExchanger = new FailedLetterKafkaExchanger();

    @Override
    public void afterApplicationInitiated(ApplicationContext applicationContext) {
        Collection<KafkaConsumer> bizConsumers = applicationContext.getBeansOfType(KafkaConsumer.class).values();
        for (KafkaConsumer bizConsumer : bizConsumers) {
            if (Collections3.isEmpty(bizConsumer.config())) {
                throw new KafkaException("consumer: " + bizConsumer.group() + "'s config() is empty");
            }
            if (!topics.add(bizConsumer.topic())) {
                throw new KafkaException("consumer topic:[" + bizConsumer.topic() + "] is duplicated");
            }
            if (bizConsumer.concurrence() < 1) {
                throw new KafkaException("consumer concurrence less then 1");
            }
            if (!getConfig(bizConsumer, KAFKA_IS_START_CONSUMER, true)) {
                continue;
            }

            if (getConfig(bizConsumer, KAFKA_ORDER_CONSUMER, false)) {
                registerOrderConsumer(bizConsumer);
            } else {
                registerUnOrderConsumer(bizConsumer);
            }
            log.info("register kafka consumer:[{}], topic:[{}]", bizConsumer.getClass().getName(), bizConsumer.topic());
        }

        ThreadManager.startAll();
    }

    private void registerUnOrderConsumer(KafkaConsumer bizConsumer) {
        String topic = bizConsumer.topic();

        SynchronousQueue<ConsumerRecord<String, JSONObject>> queue = new SynchronousQueue<>();
        localExchanger.addQueue(topic, queue);
        kafkaExchanger.addQueue(topic, queue);

        for (int i = 0; i < getConfig(bizConsumer, KAFKA_PULLER_CONCURRENCE, DEFAULT_PULLER_CONCURRENCE); ++i) {
            org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject> kafkaConsumer = ConsumerManager.newConsumer(topic, bizConsumer.group(), bizConsumer.config());
            ThreadManager.newThread("KafkaUnOrderPuller", topic, new UnOrderProducer(kafkaConsumer, queue, getConfig(bizConsumer, KAFKA_PULL_TIMEOUT, DEFAULT_PULL_TIMEOUT)));
        }
        for (int i = 0; i < bizConsumer.concurrence(); ++i) {
            ThreadManager.newThread("KafkaUnOrderConsumer", topic, new KafkaBizConsumerWrapper(queue, bizConsumer));
        }
    }

    private void registerOrderConsumer(KafkaConsumer bizConsumer) {
        String topic = bizConsumer.topic();

        List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues = new ArrayList<>();
        for (int i = 0; i < bizConsumer.concurrence(); ++i) {
            queues.add(new SynchronousQueue<>());
        }
        localExchanger.addQueues(topic, queues);
        kafkaExchanger.addQueues(topic, queues);

        for (int i = 0; i < getConfig(bizConsumer, Configs.KAFKA_PULLER_CONCURRENCE, DEFAULT_PULLER_CONCURRENCE); ++i) {
            org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject> kafkaConsumer = ConsumerManager.newConsumer(topic, bizConsumer.group(), bizConsumer.config());
            ThreadManager.newThread("KafkaOrderPuller", topic, new OrderProducer(kafkaConsumer, queues, getConfig(bizConsumer, Configs.KAFKA_PULL_TIMEOUT, DEFAULT_PULL_TIMEOUT)));
        }
        for (int i = 0; i < bizConsumer.concurrence(); ++i) {
            ThreadManager.newThread("KafkaOrderConsumer", topic, new KafkaBizConsumerWrapper(queues.get(i), bizConsumer));
        }
    }

    @AllArgsConstructor
    private static class UnOrderProducer implements Runnable {

        private org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject> consumer;

        private SynchronousQueue<ConsumerRecord<String, JSONObject>> queue;

        private long timeout;

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, JSONObject> records = consumer.poll(timeout);
                    for (ConsumerRecord<String, JSONObject> record : records) {
                        queue.put(record);
                    }
                }
            } catch (WakeupException | InterruptedException e) {
                log.warn("UnOrderProducer error.", e);
            } finally {
                consumer.close();
            }
        }
    }

    @AllArgsConstructor
    private static class OrderProducer implements Runnable {

        private org.apache.kafka.clients.consumer.KafkaConsumer<String, JSONObject> consumer;

        private List<SynchronousQueue<ConsumerRecord<String, JSONObject>>> queues;

        private long timeout;

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, JSONObject> records = consumer.poll(timeout);
                    for (ConsumerRecord<String, JSONObject> record : records) {
                        queues.get(Math.abs(record.partition()) % queues.size()).put(record);
                    }
                }
            } catch (WakeupException | InterruptedException e) {
                log.warn("OrderProducer error.", e);
            } finally {
                consumer.close();
            }
        }
    }

    private static class KafkaBizConsumerWrapper implements Runnable {

        private SynchronousQueue<ConsumerRecord<String, JSONObject>> queue;

        private KafkaConsumer bizConsumer;

        private boolean logSuccess;

        public KafkaBizConsumerWrapper(SynchronousQueue<ConsumerRecord<String, JSONObject>> queue, KafkaConsumer bizConsumer) {
            this.queue = queue;
            this.bizConsumer = bizConsumer;
            this.logSuccess = getConfig(bizConsumer, KAFKA_SPM_LOG_SUCCESS, true);
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                long start = 0;
                ConsumeStatus status = null;
                ConsumerRecord<String, JSONObject> take = null;
                try {
                    take = queue.take();
                    start = System.currentTimeMillis();
                    status = Objects2.nullToDefault(bizConsumer.consume(take), ConsumeStatus.SUCCESS);
                } catch (InterruptedException ie) {
                    return;
                } catch (Throwable t) {
                    log.error("consume kafka:[{}] msg error", bizConsumer.topic(), t);
                    status = parseStatus(bizConsumer);
                } finally {
                    processStatus(status, take, bizConsumer);
                    if (status != ConsumeStatus.SUCCESS || logSuccess) {
                        Spm.log(type, bizConsumer.topic(), status == ConsumeStatus.SUCCESS, System.currentTimeMillis() - start, bizConsumer.group());
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        ConsumerManager.stopAll();
        ThreadManager.stopAll();
    }

    private static void processStatus(ConsumeStatus status, ConsumerRecord<String, JSONObject> msg, KafkaConsumer bizConsumer) {
        if (msg == null || status == null || status == ConsumeStatus.SUCCESS || status == ConsumeStatus.FAILED) {
            return;
        }

        if (status == ConsumeStatus.RECONSUME_LOCAL) {
            localExchanger.addLetter(msg);
            return;
        }

        kafkaExchanger.addLetter(bizConsumer, msg);
    }

    private static ConsumeStatus parseStatus(KafkaConsumer bizConsumer) {
        if (getConfig(bizConsumer, KAFKA_UNCATCHED_EXCEPTION_AS_FAILED, false)) {
            return ConsumeStatus.FAILED;
        } else if (getConfig(bizConsumer, KAFKA_UNCATCHED_EXCEPTION_AS_RECONSUME_LOCAL, false)) {
            return ConsumeStatus.RECONSUME_LOCAL;
        } else if (getConfig(bizConsumer, KAFKA_UNCATCHED_EXCEPTION_AS_RECONSUME_KAFKA, false)) {
            return ConsumeStatus.RECONSUME_KAFKA;
        }
        return ConsumeStatus.FAILED;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getConfig(KafkaConsumer bizConsumer, String key, T defaultValue) {
        return (T) bizConsumer.config().getOrDefault(key, defaultValue);
    }
}
