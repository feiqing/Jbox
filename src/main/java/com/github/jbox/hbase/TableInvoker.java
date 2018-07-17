package com.github.jbox.hbase;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-05 14:09:00.
 */
@FunctionalInterface
public interface TableInvoker<T> {

    T invoke(org.apache.hadoop.hbase.client.Table table) throws Exception;
}