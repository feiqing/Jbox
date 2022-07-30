package com.github.jbox.mybatis.spring;

import com.github.jbox.mybatis.sequence.Sequence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/7/29 06:45.
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean {

    // todo: 这个地方其实是个全局的配置, 所以不同的datasource一定得要区分SequenceName, 否则就会被覆盖了
    // 后续可以通过自定义MapperScannerConfigurer来区分
    private static final ConcurrentMap<String, Sequence> name2sequence = new ConcurrentHashMap<>();

    public void setSequence(Map<String, Sequence> sequences) {
        name2sequence.putAll(sequences);
    }

    public static Sequence sequence(String name) {
        return name2sequence.get(name);
    }
}
