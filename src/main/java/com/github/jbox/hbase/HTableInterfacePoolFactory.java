package com.github.jbox.hbase;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTableInterfaceFactory;
import org.apache.hadoop.hbase.client.HTablePool;

import java.io.IOException;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-09-03 21:38:00.
 */
@Slf4j
@SuppressWarnings("all")
public class HTableInterfacePoolFactory implements HTableInterfaceFactory {

    private HTablePool hTablePool;

    public HTableInterfacePoolFactory(HTablePool hTablePool) {
        this.hTablePool = hTablePool;
    }

    @Override
    public HTableInterface createHTableInterface(Configuration config, byte[] tableName) {
        Preconditions.checkArgument(tableName != null);
        return hTablePool.getTable(tableName);
    }

    @Override
    public void releaseHTableInterface(HTableInterface table) throws IOException {
        hTablePool.putTable(table);
    }
}