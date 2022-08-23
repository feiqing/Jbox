package com.github.jbox.mybatis.extension;

import com.github.jbox.mybatis.sequence.Sequence;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 阿里云 PolarDB-X 2.0 批量序列生成器
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 10:36.
 */
@Slf4j
// todo 测试
public class PolarXSequenceBatch implements Sequence<Long> {

    private volatile Queue<Long> queue = new ConcurrentLinkedQueue<>();

    private final JdbcTemplate jdbcTemplate;

    private final int step;

    public PolarXSequenceBatch(DataSource dataSource, int step) {
        Preconditions.checkState(step > 0);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.step = step;

        if (step < 100) {
            // todo
            log.warn("step 过小, 有可能导致性能较差");
        }
    }

    @Override
    public Long nextVal(String sequenceName) {
        Long item = queue.poll();
        if (item == null) {
            synchronized (this) {
                while ((item = queue.poll()) == null) {
                    queue = new ConcurrentLinkedQueue<>(nextRange(sequenceName));
                }
            }
        }

        return item;
    }

    private List<Long> nextRange(String sequenceName) {
        return jdbcTemplate.queryForList("SELECT " + sequenceName + ".NEXTVAL FROM DUAL WHERE `count` = " + step, Long.class);
    }
}
