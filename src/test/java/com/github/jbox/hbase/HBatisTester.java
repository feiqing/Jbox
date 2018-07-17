package com.github.jbox.hbase;

import com.github.jbox.serializer.support.Hession2Serializer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.hadoop.hbase.HbaseTemplate;

import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-09 21:17:00.
 */
public class HBatisTester {

    private HbaseTemplate template;

    private HBaseBatis<TForward> tForwardBatis;

    @Before
    public void setUp() {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "127.0.0.1");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.client.pause", "50");
        conf.set("hbase.client.retries.number", "3");
        conf.set("hbase.rpc.timeout", "2000");
        conf.set("hbase.client.operation.timeout", "3000");
        conf.set("hbase.client.scanner.timeout.period", "10000");

        template = new HbaseTemplate(conf);
        tForwardBatis = new HBaseBatis<TForward>("cf1", template, new Hession2Serializer()) {
        };
    }

    @Test
    public void testBatisPut() {
        TForward forward = TForward.newForward("18:65:90:d3:15:27", "10.67.150.5", 1122);
        forward.setExtendMap(Collections.singletonMap("key", "woca-value"));
        forward.setId(2L);
        tForwardBatis.put(forward);
    }

    @Test
    public void testBatisGet() {
        TForward forward = tForwardBatis.get("1");
        System.out.println(forward);


        TForward forward1 = tForwardBatis.get("2");
        System.out.println(forward1);
    }

    @com.github.jbox.hbase.Table("page")
    private static class Page implements HBaseMode {

        @Qualifier(value = "value", family = "cf")
        private String rowKey;

        @Override
        public String getRowKey() {
            return rowKey;
        }

        @Override
        public void setRowKey(String rowKey) {
            this.rowKey = rowKey;
        }
    }

    @Test
    public void testPagePuts() {
        HBaseBatis<Page> hBaseBatis = new HBaseBatis<Page>("cf1", template, new Hession2Serializer()) {
        };

        List<Page> types = new ArrayList<>();
        for (int i = 101; i <= 200; ++i) {
            Page page = new Page();
            page.setRowKey(String.valueOf(i));

            types.add(page);
        }
        hBaseBatis.puts(types);
    }

    @Test
    public void testPageScan() {
        HBaseBatis<Page> hBaseBatis = new HBaseBatis<Page>("cf1", template, new Hession2Serializer()) {
        };

        hBaseBatis.invokeOnTable(hTable -> {
            PageFilter filter = new PageFilter(10);
            byte[] lastRow = null;

            while (true) {
                Scan scan = new Scan();
                scan.setFilter(filter);

                if (lastRow != null) {
                    byte[] startRow = Bytes.add(lastRow, Bytes.toBytes(1));
                    scan.setStartRow(startRow);
                    System.out.println("startRow: " + Bytes.toString(startRow));
                }

                ResultScanner scanner = hTable.getScanner(scan);
                long count = 0;
                for (Result result : scanner) {
                    lastRow = result.getRow();
                    Map<String, Map<String, Object>> stringMapMap = HBaseHelper.resultToColumnMap(result, new Hession2Serializer()::deserialize);
                    System.out.println(stringMapMap);
                    ++count;
                }
                System.out.println("endRow: " + Bytes.toString(lastRow));

                if (count == 0) {
                    break;
                }
            }

            return null;
        });
    }

    @com.github.jbox.hbase.Table("incr")
    private static class Incr implements HBaseMode {

        @Qualifier(value = "value", family = "cf")
        private String rowKey;

        @Override
        public String getRowKey() {
            return rowKey;
        }

        @Override
        public void setRowKey(String rowKey) {
            this.rowKey = rowKey;
        }
    }

    @Test
    public void testIncr() {
        HBaseBatis<Incr> hBaseBatis = new HBaseBatis<Incr>("cf1", template, new Hession2Serializer()) {
        };

        System.out.println(hBaseBatis.incr("key1", "family", "column1", 50));
    }

    @Test
    public void testIncrs() {
        HBaseBatis<Incr> hBaseBatis = new HBaseBatis<Incr>("cf1", template, new Hession2Serializer()) {
        };

        Map<String, Map<String, Long>> amounts = new HashMap<>();
        amounts.computeIfAbsent("family", (key) -> new HashMap<>()).put("column2", 88L);
        amounts.computeIfAbsent("family", (key) -> new HashMap<>()).put("column3", 99L);
        Map<String, Map<String, Long>> key2 = hBaseBatis.decrs("key2", amounts);
        System.out.println(key2);
    }
}
