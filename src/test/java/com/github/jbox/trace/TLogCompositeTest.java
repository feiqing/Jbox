package com.github.jbox.trace;

import com.github.jbox.caces.service.HelloWorldService;
import com.github.jbox.h2.TestDAO;
import com.github.jbox.h2.TestModel;
import org.junit.Test;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/28 04:25:00.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(locations = "classpath*:resources/spring/applicationContext.xml")
public class TLogCompositeTest {

    // @Autowired
    private HelloWorldService helloWorldService;

    @Test
    public void test() {
        FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(
                "src/test/java/resources/spring/applicationContext.xml");


//        HelloWorldService bean = applicationContext.getBean(HelloWorldService.class);
//
//
//        System.out.println(bean.sayHello("ff"));
        TestDAO testDAO = applicationContext.getBean(TestDAO.class);

        TestModel model = new TestModel();
        model.setName("feiqing3");
        model.setBytes((byte) 1);
        model.setShorts((short) 2);
        model.setInts(3);
        model.setLongs(4);
        model.setFloats(5.5F);
        model.setDoubles(6.6);
        model.setBooleans(true);
        model.setChars('a');
        model.setCharacters('b');
        model.setBigDecimals(new BigDecimal("7.7"));
        model.setLocalTimes(LocalTime.of(17, 30, 50, 1001));
        model.setLocalDates(LocalDate.of(2014, 9, 18));
        model.setLocalDateTimes(LocalDateTime.of(LocalDate.of(2014, 9, 18), LocalTime.of(17, 30, 50, 1001)));

        model.setModel(new TestModel());

        int name = testDAO.upsert(model, "name");

        List<TestModel> all = testDAO.findAll();
        System.out.println(all);
    }
}
