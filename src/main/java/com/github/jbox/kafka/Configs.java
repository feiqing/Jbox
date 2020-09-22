package com.github.jbox.kafka;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/22 11:27 AM.
 */
public interface Configs {

    // 是否有序消费
    String KAFKA_ORDER_CONSUMER = "jbox.order.consumer";

    // Kafka消息拉取超时时间
    String KAFKA_PULL_TIMEOUT = "jbox.pull.timeout";

    // Kafka消息拉取并发度
    String KAFKA_PULLER_CONCURRENCE = "jbox.puller.concurrence";

    // 是否启动消费者
    String KAFKA_IS_START_CONSUMER = "jbox.is.start.consumer";

    // 消息消费抛出异常后该消息如何处理, 如下 #1. #2. #3
    // #1. 与ConsumeStatus.FAILED行为相同(默认行为)
    String KAFKA_UNCATCHED_EXCEPTION_AS_FAILED = "FAILED";
    // #2. 与ConsumeStatus.RECONSUME_LOCAL行为相同
    String KAFKA_UNCATCHED_EXCEPTION_AS_RECONSUME_LOCAL = "RECONSUME_LOCAL";
    // #3. 与ConsumeStatus.RECONSUME_KAFKA行为相同
    String KAFKA_UNCATCHED_EXCEPTION_AS_RECONSUME_KAFKA = "RECONSUME_KAFKA";
        // -- 在#3为true时: 配置暂存失败消息的Kafka-topic, 默认为DEFAULT_EXCHANGE_LETTER_TOPIC
        String KAFKA_EXCHANGE_LETTER_TOPIC = "jbox.exchange.letter.topic";
        // -- 在#3为true时: 配置消费失败消息的Kafka-group, 默认与Consumer的group相同
        String KAFKA_EXCHANGE_LETTER_GROUP = "jbox.exchange.letter.group";

    // constants
    String DEFAULT_EXCHANGE_LETTER_TOPIC = "_exchange_letter";

    int DEFAULT_PULLER_CONCURRENCE = 1;

    long DEFAULT_PULL_TIMEOUT = 5000L;

    long DEFAULT_EXCHANGE_PULL_TIMEOUT = 1000L;
}
