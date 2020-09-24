package com.github.jbox.h2;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 3:01 PM.
 */
public class TestDAO extends H2Batis<TestModel> {

    public TestDAO(String h2path) {
        super(h2path);
    }
}
