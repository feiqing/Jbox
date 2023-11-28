package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 14:26.
 */
public class H2Sequence implements Sequence<Long> {

    private final JdbcTemplate jdbcTemplate;

    public H2Sequence(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long nextVal(String sequenceId) {
        return jdbcTemplate.queryForObject("select nextval('" + sequenceId + "')", Long.class);
    }
}