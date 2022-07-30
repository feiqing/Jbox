package com.github.jbox.caces.service.impl;

import com.github.jbox.caces.service.HelloWorldService;
import com.github.jbox.domain.User;
import com.github.jbox.domain.UserWrapper;
import com.github.jbox.trace.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by jifang.zjf
 * Since 2017/5/9 上午9:34.
 */
public class HelloWorldServiceImpl implements HelloWorldService {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldService.class);

    @Trace
    @Override
    public String sayHello(String name) {
        if (true) {
            throw new RuntimeException("woca");
        }
        return "hello : " + name;
    }

    @Trace
    @Override
    public User user(User user) throws Exception {
        return user;
    }

    @Trace
    @Override
    public List<User> users(List<User> users) {
        return users;
    }

    @Trace
    @Override
    public List<String> strs(String[] strs) {
        return Collections.emptyList();
    }

    @Override
    public void userWrapper(UserWrapper wrapper) {
        // throw new RuntimeException(wrapper.toString());
    }

    @Trace
    @Override
    public void limit(@Min(1) int page, @Max(50) int size) {
    }
}
