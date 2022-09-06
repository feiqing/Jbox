package com.github.jbox.mybatis.extension;

import com.github.jbox.mybatis.sequence.Sequence;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 14:16.
 */
public class Db2Sequence implements Sequence<Long> {

    private final JdbcTemplate jdbcTemplate;

    public Db2Sequence(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long nextVal(String sequenceId) {
        return jdbcTemplate.queryForObject("values nextval for " + sequenceId, Long.class);
    }
}
