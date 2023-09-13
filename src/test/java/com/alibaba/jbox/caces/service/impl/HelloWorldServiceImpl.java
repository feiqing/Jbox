package com.alibaba.jbox.caces.service.impl;

import com.alibaba.jbox.caces.service.HelloWorldService;
import com.alibaba.jbox.domain.User;
import com.alibaba.jbox.domain.UserWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by jifang.zjf
 * Since 2017/5/9 上午9:34.
 */
public class HelloWorldServiceImpl implements HelloWorldService {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldService.class);

    @Override
    public String sayHello(String name) {
        if (true) {
            throw new RuntimeException("woca");
        }
        return "hello : " + name;
    }

    @Override
    public User user(User user) throws Exception {
        return user;
    }

    @Override
    public List<User> users(List<User> users) {
        return users;
    }

    @Override
    public List<String> strs(String[] strs) {
        return Collections.emptyList();
    }

    @Override
    public void userWrapper(UserWrapper wrapper) {
        // throw new RuntimeException(wrapper.toString());
    }

    @Override
    public void limit(int page, int size) {
    }
}
