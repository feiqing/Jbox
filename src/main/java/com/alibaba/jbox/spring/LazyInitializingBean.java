package com.alibaba.jbox.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-08 22:00:00.
 */
public interface LazyInitializingBean extends ApplicationListener<ContextRefreshedEvent> {

    default void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            afterApplicationInitiated(event.getApplicationContext());
        }
    }

    void afterApplicationInitiated(ApplicationContext applicationContext);
}
