package com.github.jbox;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/28 04:22:00.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:resources/spring/applicationContext.xml")
public abstract class TestBase {
}
