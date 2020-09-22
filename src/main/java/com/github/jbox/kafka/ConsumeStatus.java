package com.github.jbox.kafka;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/16 11:25 AM.
 */
public enum ConsumeStatus implements Serializable {
    SUCCESS,            // 消费成功: 与返回NULL相同.
    FAILED,             // 消费失败: 同时直接丢弃该消息, 不再重新投递.
    RECONSUME_LOCAL,    // 消费失败: 同时将该失败消息在本地暂存, 一会儿重新投递(应用重启会导致休息丢失).
    RECONSUME_KAFKA     // 消费失败: 同时将该失败消息在云端Kafka暂存, 一会儿重新投递.
}
