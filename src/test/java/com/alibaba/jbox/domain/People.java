package com.alibaba.jbox.domain;

import lombok.Data;
import org.slf4j.helpers.Util;

/**
 * @author jifang
 * @since 2017/3/1 下午2:41.
 */
@Data
public class People {
    private String source;
    private String name;
    private Address address;

    public People(String name, Address address) {
        this.address = address;
        this.name = name;
        this.source = Util.getCallingClass().getSimpleName();
    }
}
