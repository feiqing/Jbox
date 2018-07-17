package com.github.jbox.caces.service;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.github.jbox.domain.User;
import com.github.jbox.domain.UserWrapper;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by jifang.zjf
 * Since 2017/5/9 上午9:33.
 */
public interface HelloWorldService {

    String sayHello(@NotEmpty(message = "姓名不能为空啊!!!") String name);

    User user(@Valid User user) throws Exception;

    List<User> users(@Valid List<User> users);

    List<String> strs(String[] strs);

    void userWrapper(UserWrapper wrapper);

    void limit(@Min(1) int page, @Max(50) int size);
}