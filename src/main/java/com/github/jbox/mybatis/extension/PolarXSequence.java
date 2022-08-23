package com.github.jbox.mybatis.extension;

import com.github.jbox.mybatis.sequence.Sequence;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 阿里云 PolarDB-X 2.0 序列生成器
 *
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 10:34.
 */
// todo: 测试
public class PolarXSequence implements Sequence<Long> {

    private final JdbcTemplate jdbcTemplate;

    public PolarXSequence(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Long nextVal(String sequenceName) {
        return jdbcTemplate.queryForObject("SELECT " + sequenceName + ".NEXTVAL", Long.class);
    }
}
