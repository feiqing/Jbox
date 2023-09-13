package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Oracle 序列生成器
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 14:28.
 */
public class OracleSequence implements Sequence<Long> {

    private final JdbcTemplate jdbcTemplate;

    public OracleSequence(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long nextVal(String sequenceId) {
        return jdbcTemplate.queryForObject("SELECT " + sequenceId + ".NEXTVAL FROM DUAL", Long.class);
    }
}
