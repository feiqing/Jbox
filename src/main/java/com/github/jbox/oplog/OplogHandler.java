package com.github.jbox.oplog;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/11/12 4:42 PM.
 */
public interface OplogHandler {

    void handle(Oplog log) throws Throwable;
}
