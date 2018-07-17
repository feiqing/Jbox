package com.github.jbox.hbase.params;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-05 12:58:00.
 */
public class Put extends org.apache.hadoop.hbase.client.Put {

    public static Put newInstance(String rowKey) {
        return new Put(Bytes.toBytes(rowKey));
    }

    public Put(byte[] row) {
        super(row);
    }
}
