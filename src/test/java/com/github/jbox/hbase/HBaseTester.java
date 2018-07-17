package com.github.jbox.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Function;

import static org.apache.hadoop.hbase.filter.CompareFilter.CompareOp.EQUAL;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-06-21 15:48:00.
 */
public class HBaseTester {

    private Connection connection;

    private HTable table;

    private HbaseTemplate template;

    private Table t;

    private Table t2;

    private Configuration conf;

    private Admin admin;

    @Before
    public void setUp() throws IOException {
        conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "127.0.0.1");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.client.pause", "50");
        conf.set("hbase.client.retries.number", "3");
        conf.set("hbase.rpc.timeout", "2000");
        conf.set("hbase.client.operation.timeout", "3000");
        conf.set("hbase.client.scanner.timeout.period", "10000");
        connection = ConnectionFactory.createConnection(conf);
        template = new HbaseTemplate(conf);
        //table = connection.getTable(TableName.valueOf("test"));
        table = new HTable(conf, TableName.valueOf("test"));
        t = connection.getTable(TableName.valueOf("t"));
        t2 = connection.getTable(TableName.valueOf("t2"));
        admin = connection.getAdmin();
    }

    @Test
    public void testPool() {
        HTablePool pool = new HTablePool(conf, 5);
        for (int i = 0; i < 10; i++) {
            System.out.println(pool.getTable("counter"));
        }

        System.out.println(conf);
    }


    @Test
    public void testTemplate() {
        Scan scan = new Scan();
        scan.setFilter(new RowFilter(EQUAL, new BinaryPrefixComparator(Bytes.toBytes("key2"))));
        template.find("test", scan, new RowMapper<Void>() {
            @Override
            public Void mapRow(Result result, int rowNum) throws Exception {
                printNoVersionMap(result.getRow(), result.getNoVersionMap(), Bytes::toString);
                return null;
            }
        });
    }

    @Test
    public void testColumnFilter() throws IOException {
        Get get  = new Get(Bytes.toBytes("key1"));
        get.setMaxVersions(1);
        get.setFilter(new QualifierFilter(EQUAL, new BinaryComparator("fq2".getBytes())));
        Result result = table.get(get);
        printAllMap(result.getRow(), result.getMap(), Bytes::toString);
    }

    @Test
    public void testSingleColumn() {

        Scan scan = new Scan();
        scan.setFilter(new PrefixFilter("key8".getBytes()));

        template.find("test", scan, new RowMapper<Void>() {
            @Override
            public Void mapRow(Result result, int rowNum) throws Exception {
                printNoVersionMap(result.getRow(), result.getNoVersionMap()
                        , Bytes::toString);
                return null;
            }
        });
    }

    @Test
    public void testPuts() throws IOException {
        List<Put> puts = new ArrayList<>();
        Put put1 = new Put("key2".getBytes());
        put1.addColumn("cf".getBytes(), "column1".getBytes(), "value1".getBytes());
        puts.add(put1);

        Put put2 = new Put("key3".getBytes());
        put2.addColumn("cf2".getBytes(), "column2".getBytes(), "value2".getBytes());
        puts.add(put2);

        try {
            t.put(puts);
        } catch (Exception t) {
            t.printStackTrace();
        }

        System.out.println("--------");

        Put put = new Put("key4".getBytes());
        put.addColumn("cf".getBytes(), "column1".getBytes(), "value retry".getBytes());
        t.put(put);
        System.out.println("put success.");
    }

    @Test
    public void testGetMaxVersion() throws IOException {
        Get get = new Get("key1".getBytes());
        get.setMaxVersions();
        get.addColumn("a".getBytes(), "column2".getBytes());
        Result result = t2.get(get);
        printAllMap(result.getRow(), result.getMap(), Bytes::toString);
    }

    @Test
    public void testFilter() throws IOException {
        RowFilter filter = new RowFilter(EQUAL, new RegexStringComparator("key6"));

        Scan scan = new Scan();
        scan.addColumn(Bytes.toBytes("family1"), Bytes.toBytes("fq1"));
        scan.setFilter(filter);
        ResultScanner scanner = table.getScanner(scan);
        for (Result result : scanner) {
            // printAllMap(result.getRow(), result.get(), Bytes::toString);
            printNoVersionMap(result.getRow(), result.getNoVersionMap(), Bytes::toString);
        }

    }

    private <T> void printNoVersionMap(byte[] rowKey,
                                       NavigableMap<byte[], NavigableMap<byte[], byte[]>> noVersionMap,
                                       Function<byte[], T> deSerialize) {
        String row = Bytes.toString(rowKey);
        // family -> qualifier -> value
        noVersionMap.forEach((familyBytes, qualifierMap) -> {
            String family = Bytes.toString(familyBytes);

            qualifierMap.forEach((qualifierBytes, valueBytes) -> {
                String qualifier = Bytes.toString(qualifierBytes);
                T obj = deSerialize.apply(valueBytes);

                System.out.println(String.format("row:[%s] -> col:[%s:%s]: '%s'", row, family, qualifier, obj));
            });
        });
    }

    private <T> void printAllMap(byte[] rowKey,
                                 NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap,
                                 Function<byte[], T> deSerialize) {
        if (CollectionUtils.isEmpty(familyMap)) {
            return;
        }

        String row = Bytes.toString(rowKey);
        // family -> qualifier -> timestamp -> value
        for (Map.Entry<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyEntry : familyMap.entrySet()) {
            String family = Bytes.toString(familyEntry.getKey());
            NavigableMap<byte[], NavigableMap<Long, byte[]>> qualifierMap = familyEntry.getValue();
            // qualifier -> timestamp -> value
            for (Map.Entry<byte[], NavigableMap<Long, byte[]>> qualifierEntry : qualifierMap.entrySet()) {
                String qualifier = Bytes.toString(qualifierEntry.getKey());
                NavigableMap<Long, byte[]> timestampMap = qualifierEntry.getValue();

                timestampMap.forEach((timestamp, valueBytes) -> System.out.println(String.format("row:[%s] -> col:[%s:%s]: '%s'(%s)", row, family, qualifier, deSerialize.apply(valueBytes), timestamp)));
            }
        }
    }

    @Test
    public void templatePut() {
        // template.put("test", "key1", "fam1");
    }


    @Test
    public void testPut() throws IOException {
        Table table = connection.getTable(TableName.valueOf("test"));
        Put put = new Put(Bytes.toBytes("key3"));
        put.addColumn(Bytes.toBytes("family1"), Bytes.toBytes("fq1"), Bytes.toBytes(1));
        table.put(put);
    }

    @Test
    public void testGet() throws IOException {
        Get get = new Get(Bytes.toBytes("key3"));
        get.addFamily(Bytes.toBytes("family1"));
        Result result = table.get(get);
        byte[] value = result.getValue(Bytes.toBytes("family1"), Bytes.toBytes("fq2"));
        if (value == null) {
            System.out.println("NONE");
        } else {
            int va = Bytes.toInt(value);
            System.out.println(va);
        }
    }

    @Test
    public void testDelete() throws IOException {
        Delete delete = new Delete("key1".getBytes());
        delete.addColumn("family1".getBytes(), "fq2".getBytes());
        table.delete(delete);
    }

    @Test
    public void testFind() {
        String s = template.get("test", "key1", (result, rowNum) -> Bytes.toString(result.getValue(Bytes.toBytes("family1"), Bytes.toBytes("col1"))));
        System.out.println(s);
    }

    @Test
    public void addKeyValue() throws IOException {
        KeyValue keyValue = new KeyValue(Bytes.toBytes("key4"), Bytes.toBytes("family1"), Bytes.toBytes("fq1"), Bytes.toBytes("woca3"));
        Put key4 = new Put(Bytes.toBytes("key4"));
        key4.add(keyValue);
        table.put(key4);
    }

    @Test
    public void testMaxVersion() throws IOException {
        Get get = new Get(Bytes.toBytes("key4"));
        get.addColumn(Bytes.toBytes("family1"), Bytes.toBytes("fq1")).setMaxVersions();

        Result result = table.get(get);
        System.out.println(result);
    }

    @Test
    public void testRest() {
    }

    @Test
    public void testPutAll() throws IOException {
        List<Put> puts = new ArrayList<>();
        for (int i = 11; i < 1000000; ++i) {
            Put put = new Put(Bytes.toBytes("key" + i));
            put.addColumn(Bytes.toBytes("family1"), Bytes.toBytes("fq1"), Bytes.toBytes("wocalai-" + i));

            puts.add(put);
        }

        long start = System.currentTimeMillis();
        table.put(puts);
        System.out.println((System.currentTimeMillis() - start) * 1.0 / 1000);
    }

    @Test
    public void testHBase() throws IOException {


        Table table = connection.getTable(TableName.valueOf("test"));
        ResultScanner scanner = table.getScanner(new Scan());
        for (Result rs : scanner) {
            System.out.println("RowKey为：" + new String(rs.getRow()));
            // 按cell进行循环
            for (Cell cell : rs.rawCells()) {
                System.out.println("列簇为：" + new String(CellUtil.cloneFamily(cell)));
                //System.out.println("列修饰符为：" + new String(CellUtil.cloneQualifier(cell)));
                System.out.println("值为：" + new String(CellUtil.cloneValue(cell)));
            }
            System.out.println("=============================================");
        }
    }

    @Test
    public void testAdmin() throws IOException {
        HTableDescriptor hTableDescriptor = new HTableDescriptor(Bytes.toBytes("test_table"));
        hTableDescriptor.addFamily(new HColumnDescriptor("fm"));

        admin.createTable(hTableDescriptor);

        System.out.println(admin.isTableAvailable(TableName.valueOf("test_table")));
    }

    @Test
    public void testAdminList() throws IOException {
        for (HTableDescriptor hTableDescriptor : admin.listTables()) {
            System.out.println(hTableDescriptor);
        }
    }

    @Test
    public void testAdminDelete() throws IOException {
        admin.disableTable(TableName.valueOf("test_table"));
        admin.deleteTable(TableName.valueOf("test_table"));
    }

    @Test
    public void ss() throws IOException {
        //admin.split(TableName.valueOf("test"), "key446311".getBytes());
        // System.out.println(admin.getClusterStatus());
        // table = new HTable(conf, "page");
        Pair<byte[][], byte[][]> startEndKeys = table.getStartEndKeys();
        for (int i = 0; i < startEndKeys.getFirst().length; ++i) {
            byte[] startKeys = startEndKeys.getFirst()[i];
            byte[] endKeys = startEndKeys.getSecond()[i];
            System.out.println(Bytes.toString(startKeys) + "," + Bytes.toString(endKeys));
        }
    }
}
