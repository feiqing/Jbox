package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 15:05.
 */
public class RedisSequence implements Sequence<Long> {

    private final JedisPool jedisPool;

    private final String prefix;

    private final String suffix;

    public RedisSequence(JedisPool jedisPool) {
        this(jedisPool, "SEQ:", "");
    }

    public RedisSequence(JedisPool jedisPool, String prefix, String suffix) {
        this.jedisPool = jedisPool;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public Long nextVal(String sequenceId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(prefix + sequenceId + suffix);
        }
    }
}
