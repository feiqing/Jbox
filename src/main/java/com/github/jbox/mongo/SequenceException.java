package com.github.jbox.mongo;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/3 5:01 PM.
 */
public class SequenceException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = -7383087459057215862L;

    public SequenceException(String message) {
        super(message);
    }
}
