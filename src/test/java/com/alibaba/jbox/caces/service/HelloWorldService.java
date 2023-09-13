package com.alibaba.jbox.caces.service;

import com.alibaba.jbox.domain.User;
import com.alibaba.jbox.domain.UserWrapper;

import java.util.List;

/**
 * Created by jifang.zjf
 * Since 2017/5/9 上午9:33.
 */
public interface HelloWorldService {

    String sayHello(String name);

    User user(User user) throws Exception;

    List<User> users(List<User> users);

    List<String> strs(String[] strs);

    void userWrapper(UserWrapper wrapper);

    void limit(int page, int size);
}