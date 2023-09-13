package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ToDo: 其实叫MySQL Sequence更合适
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/17 13:38.
 */
@Slf4j
public class JdbcSequence implements Sequence<Long> {

    private static final String SQL_CREATE_TABLE = "" +
            "CREATE TABLE IF NOT EXISTS `%s` (\n" +
            "  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
            "  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
            "  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',\n" +
            "  `name` varchar(50) NOT NULL COMMENT '表名',\n" +
            "  `value` bigint(20) unsigned NOT NULL COMMENT 'sequence值',\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  UNIQUE KEY `uk_name` (`name`)\n" +
            ") COMMENT='sequence表'";

    private static final String SQL_INSERT = "INSERT INTO `%s`(`gmt_create`, `gmt_modified`, `name`, `value`) VALUES(NOW(), NOW(), ?, ?)";

    private static final String SQL_SELECT = "SELECT `value` FROM `%s` WHERE `name` = ?";

    private static final String SQL_UPDATE = "UPDATE `%s` SET `gmt_modified` = NOW(), `value` = ? WHERE `name` = ? AND `value` = ?";

    private final ConcurrentMap<String, Range> ranges = new ConcurrentHashMap<>();

    @Setter
    private String sequenceTable = "sequence";

    @Setter
    private int failedTimesLimit = 3;

    @Setter
    private int initValue = 100000;

    private final JdbcTemplate jdbcTemplate;

    private final int step;

    public JdbcSequence(DataSource dataSource, int step) {
        Preconditions.checkArgument(step > 0 && step < 100000);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.step = step;
    }

    @Override
    public Long nextVal(String sequenceId) {
        Long value = getRange(sequenceId).getAndIncrement();

        if (value == null) {
            synchronized (this) {
                while ((value = getRange(sequenceId).getAndIncrement()) == null) {
                    ranges.put(sequenceId, nextRange(sequenceId, 0));
                }
            }
        }

        return value;
    }

    private Range getRange(String sequenceName) {
        return ranges.computeIfAbsent(sequenceName, _K -> new Range());
    }

    private Range nextRange(String sequenceName, int failedTimes) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sequenceName));
        if (failedTimes > failedTimesLimit) {
            throw new RuntimeException(String.format("Sequence:[%s] incr failed too many times (%s).", sequenceName, failedTimes));
        }

        long cursor;
        long nextCursor;
        try {
            cursor = jdbcTemplate.queryForObject(String.format(SQL_SELECT, sequenceTable), new Object[]{sequenceName}, Long.class);
            if (cursor < 0) {
                throw new RuntimeException(String.format("Sequence:[%s] `value` < 0 (%s), please check table:[%s]", sequenceName, cursor, sequenceTable));
            } else if (cursor > Long.MAX_VALUE - step * 10L) {
                throw new RuntimeException(String.format("Sequence:[%s] `value` overflow (%s), please check table:[%s]", sequenceName, cursor, sequenceTable));
            }

            nextCursor = cursor + step;
        } catch (Throwable throwable) {
            if (containsAll(throwable.getMessage(), "Table", sequenceTable, "doesn't exist")) {
                handleTableNotExists();
                handleRecordNotExists(sequenceName);
                return nextRange(sequenceName, failedTimes);
            }

            if (containsAll(throwable.getMessage(), "Incorrect result size", "actual 0")) {
                handleRecordNotExists(sequenceName);
                return nextRange(sequenceName, failedTimes);
            }

            throw new RuntimeException(throwable);
        }

        if (jdbcTemplate.update(String.format(SQL_UPDATE, sequenceTable), nextCursor, sequenceName, cursor) <= 0) {
            return nextRange(sequenceName, failedTimes + 1);
        }
        return new Range(cursor + 1, nextCursor);
    }

    private void handleTableNotExists() {
        try {
            jdbcTemplate.execute(String.format(SQL_CREATE_TABLE, sequenceTable));
        } catch (Throwable t) {
            log.error("SequenceTable:[{}] init error.", sequenceTable, t);
        }
    }

    private void handleRecordNotExists(String sequenceName) {
        try {
            jdbcTemplate.update(String.format(SQL_INSERT, sequenceTable), sequenceName, initValue);
        } catch (Throwable t) {
            log.warn("Sequence:[{}] init failed.", sequenceName, t);
        }
    }

    private boolean containsAll(String message, String... searchs) {
        if (Strings.isNullOrEmpty(message)) {
            return false;
        }

        for (String search : searchs) {
            if (!StringUtils.containsIgnoreCase(message, search)) {
                return false;
            }
        }

        return true;
    }

    // [start, end] 而非 [start, end)
    private static final class Range {

        private final AtomicLong start;

        private final long end;

        Range() {
            this(1, 0);
        }

        Range(long start, long end) {
            this.start = new AtomicLong(start);
            this.end = end;
        }

        public Long getAndIncrement() {
            long value = start.getAndIncrement();
            if (value > end) {
                return null;
            }

            return value;
        }
    }
}
