package com.alibaba.jbox.mybatis.extension;

import com.alibaba.jbox.mybatis.sequence.Sequence;

import java.util.UUID;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/18 13:54.
 */
public class UuidSequence implements Sequence<String> {

    @Override
    public String nextVal(String sequenceId) {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
