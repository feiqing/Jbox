package com.github.jbox.hbase;

import org.apache.hadoop.hbase.client.HTableInterface;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-05 14:09:00.
 */
@SuppressWarnings("all")
@FunctionalInterface
public interface TableInvoker<T> {

    T invoke(HTableInterface table) throws Exception;
}