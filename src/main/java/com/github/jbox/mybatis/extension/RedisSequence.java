package com.github.jbox.mybatis.extension;

import com.github.jbox.mybatis.sequence.Sequence;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 15:05.
 */
public class RedisSequence implements Sequence<Long> {

    private final JedisPool jedisPool;

    public RedisSequence(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public Long nextVal(String sequenceName) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr(prefix() + sequenceName + suffix());
        }
    }

    protected String prefix() {
        return "SEQ:";
    }

    protected String suffix() {
        return "";
    }
}
