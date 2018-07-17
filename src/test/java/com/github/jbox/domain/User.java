package com.github.jbox.domain;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/8/23 13:19:00.
 */
@Data
public class User {

    @NotEmpty(message = "name 不能为空啊")
    private String name;

    @Valid
    @NotNull(message = "不能是null all")
    private Integer id;
}
