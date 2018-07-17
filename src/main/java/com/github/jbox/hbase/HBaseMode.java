package com.github.jbox.hbase;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 20:45:00.
 */
public interface HBaseMode extends Serializable {

    String getRowKey();

    void setRowKey(String rowKey);
}
