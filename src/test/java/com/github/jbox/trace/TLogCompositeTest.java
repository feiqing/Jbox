package com.github.jbox.trace;

import com.github.jbox.caces.service.HelloWorldService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/28 04:25:00.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:applicationContext.xml")
public class TLogCompositeTest {

    @Autowired
    private HelloWorldService helloWorldService;

    @Test
    public void test() {
        //FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(
        //    "/Users/jifang.zjf/IdeaProjects/jbox/src/test/java/resources/spring/applicationContext.xml");
        //
        //
        //HelloWorldService bean = applicationContext.getBean(HelloWorldService.class);


        System.out.println(helloWorldService.sayHello("ff"));
    }
}
