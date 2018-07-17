package com.github.jbox.trace;

import javax.annotation.Resource;

import com.github.jbox.TestBase;
import com.github.jbox.caces.service.HelloWorldService;

import org.junit.Test;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/28 04:25:00.
 */
public class TLogCompositeTest extends TestBase {

    @Resource
    private HelloWorldService helloWorldService;

    @Test
    public void test() {
        //FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(
        //    "/Users/jifang.zjf/IdeaProjects/jbox/src/test/java/resources/spring/applicationContext.xml");
        //
        //
        //HelloWorldService bean = applicationContext.getBean(HelloWorldService.class);


        helloWorldService.limit(1, 2);
    }
}
