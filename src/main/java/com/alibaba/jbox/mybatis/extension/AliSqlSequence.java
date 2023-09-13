package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * AliSQL Sequence Engine 序列生成器
 * 参考: https://help.aliyun.com/document_detail/130307.html
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 14:37.
 */
public class AliSqlSequence implements Sequence<Long> {

    private final JdbcTemplate jdbcTemplate;

    public AliSqlSequence(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long nextVal(String sequenceId) {
        return jdbcTemplate.queryForObject("SELECT " + sequenceId + ".NEXTVAL FROM DUAL", Long.class);
    }
}
