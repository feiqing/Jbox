package com.alibaba.jbox.biz;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-03-01 16:57:00.
 */
public interface IErrorCode extends Serializable {

    String getCode();

    String getMsg();
}
