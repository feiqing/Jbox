package com.github.jbox.hbase.params;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-05 12:31:00.
 */
public class Delete extends org.apache.hadoop.hbase.client.Delete {

    public static Delete newInstance(String rowKey) {
        return new Delete(Bytes.toBytes(rowKey));
    }

    public Delete(byte[] row) {
        super(row);
    }
}
