package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 阿里云 PolarDB-X 2.0 批量序列生成器
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 10:36.
 */
@Slf4j
public class PolarDbXBatchSequence implements Sequence<Long> {

    private final ConcurrentHashMap<String, Queue<Long>> queues = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;

    private final int step;

    public PolarDbXBatchSequence(DataSource dataSource, int step) {
        Preconditions.checkState(step > 0);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.step = step;
    }

    @Override
    public Long nextVal(String sequenceId) {
        Long item = getQueue(sequenceId).poll();
        if (item == null) {
            synchronized (this) {
                while ((item = getQueue(sequenceId).poll()) == null) {
                    queues.put(sequenceId, nextRange(sequenceId));
                }
            }
        }

        return item;
    }

    private Queue<Long> getQueue(String sequenceId) {
        return queues.computeIfAbsent(sequenceId, _k -> new ConcurrentLinkedQueue<>());
    }

    private Queue<Long> nextRange(String sequenceName) {
        return new ConcurrentLinkedQueue<>(jdbcTemplate.queryForList("SELECT " + sequenceName + ".NEXTVAL FROM DUAL WHERE `count` = " + step, Long.class));
    }
}
