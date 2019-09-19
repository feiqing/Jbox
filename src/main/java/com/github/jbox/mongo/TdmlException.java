package com.github.jbox.mongo;

import org.slf4j.helpers.MessageFormatter;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/9/19 5:26 PM.
 */
public class TdmlException extends RuntimeException {

    private static final long serialVersionUID = -6557195512152186534L;

    public TdmlException(String template, Object... args) {
        super(MessageFormatter.arrayFormat(template, args).getMessage());
    }
}
