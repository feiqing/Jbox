package com.github.jbox.hbase.params;

import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-06-25 14:05:00.
 */
public class Get extends org.apache.hadoop.hbase.client.Get {

    public static Get newInstance(String rowKey) {
        return new Get(Bytes.toBytes(rowKey));
    }

    public static Get newInstance(String rowKey, int maxVersions) throws IOException {
        Get get = newInstance(rowKey);
        get.setMaxVersions(maxVersions);
        return get;
    }


    public Get(byte[] row) {
        super(row);
    }
}
