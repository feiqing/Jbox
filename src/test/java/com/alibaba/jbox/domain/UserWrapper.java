package com.alibaba.jbox.domain;

import java.util.List;

import lombok.Data;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/23 08:18:00.
 */
@Data
public class UserWrapper {

    private String prefix;

    List<User> users;

    private String suffix;
}
