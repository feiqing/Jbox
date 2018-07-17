package com.github.jbox.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/3 15:54:00.
 */
public abstract class AbstractApplicationContextAware implements ApplicationContextAware {

    protected static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AbstractApplicationContextAware.applicationContext = applicationContext;
        SpringLoggerHelper.info("{}'s applicationContext been autowired.", this.getClass().getName());
    }
}
