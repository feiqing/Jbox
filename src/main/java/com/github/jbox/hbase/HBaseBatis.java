package com.github.jbox.hbase;

import com.github.jbox.serializer.ISerializer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.jbox.hbase.HBaseHelper.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 18:49:00.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class HBaseBatis<T extends HBaseMode> {

    private Class<?> type;

    private String tableName;

    private String defaultFamily;

    private HbaseTemplate template;

    private ISerializer serializer;

    private RowMapper<Map<String/*family*/, Map<String/*qualifier*/, Object/*value*/>>> rowMapper = (result, rowNum) -> HBaseHelper.resultToColumnMap(result, serializer::deserialize);

    private RowMapper<Pair<String/*rowKey*/, Map<String/*family*/, Map<String/*qualifier*/, Object/*value*/>>>> rowMapperWithRowKey = (result, rowNum) -> {
        String rowKey = Bytes.toString(result.getRow());
        Map<String, Map<String, Object>> value = HBaseHelper.resultToColumnMap(result, serializer::deserialize);
        return Pair.of(rowKey, value);
    };


    private ObjectMapper<T> objectMapper = new ObjectMapper<T>() {

        @Override
        public T mapObject(String rowKey, Map<String, Map<String, Object>> columnMap) throws Exception {
            T instance = (T) type.newInstance();
            instance.setRowKey(rowKey);

            for (Map.Entry<String, Map<String, Object>> familyEntry : columnMap.entrySet()) {
                String family = familyEntry.getKey();

                for (Map.Entry<String, Object> qualifierEntry : familyEntry.getValue().entrySet()) {
                    String qualifier = qualifierEntry.getKey();
                    Object value = qualifierEntry.getValue();

                    Field field = HBaseHelper.getFieldByQualifier(type, family, qualifier, defaultFamily);

                    if (field == null) {
                        log.debug("table:{}, row:{}, family:{}, qualifier:{} have none relative class:{} field.",
                                tableName, rowKey, family, qualifier, type.getName());
                    } else {
                        FieldUtils.writeField(field, instance, value, true);
                    }
                }
            }

            return instance;
        }
    };

    public HBaseBatis(String tableName, String defaultFamily, HbaseTemplate hbaseTemplate, Class<?> type, ISerializer serializer) {
        this.tableName = tableName;
        this.defaultFamily = defaultFamily;
        this.template = hbaseTemplate;
        this.type = type;
        this.serializer = serializer;
    }

    public HBaseBatis(String defaultFamily, HbaseTemplate hbaseTemplate, ISerializer serializer) {
        Class<T> type = getType();
        this.tableName = getTableName(type);
        this.defaultFamily = defaultFamily;
        this.template = hbaseTemplate;
        this.type = type;
        this.serializer = serializer;
    }

    // --- put ---

    public void put(T obj) {
        Map<String, Map<String, Object>> qualifierMap = objToColumnMap(obj, defaultFamily);
        this.put(obj.getRowKey(), qualifierMap);
    }

    public void put(String rowKey, Map<String, Map<String, Object>> qualifierMap) {
        Preconditions.checkArgument(StringUtils.isNotBlank(rowKey));

        if (CollectionUtils.isEmpty(qualifierMap)) {
            return;
        }

        Put put = qualifierMapToPut(rowKey, qualifierMap);
        invokeOnTable(table -> {
            table.put(put);
            return null;
        });
    }

    // --- puts ---

    public void puts(List<T> objs) {
        if (CollectionUtils.isEmpty(objs)) {
            return;
        }

        List<String> rowKeys = new ArrayList<>(objs.size());
        List<Map<String, Map<String, Object>>> qualifierMaps = new ArrayList<>(objs.size());
        for (T obj : objs) {
            rowKeys.add(obj.getRowKey());
            qualifierMaps.add(objToColumnMap(obj, defaultFamily));
        }

        puts(rowKeys, qualifierMaps);
    }

    public void puts(List<String> rowKeys, List<Map<String, Map<String, Object>>> qualifierMaps) {
        if (CollectionUtils.isEmpty(rowKeys) || CollectionUtils.isEmpty(qualifierMaps)) {
            return;
        }

        Preconditions.checkArgument(rowKeys.size() == qualifierMaps.size());

        invokeOnTable(table -> {
            List<org.apache.hadoop.hbase.client.Put> puts = new ArrayList<>();
            for (int i = 0; i < rowKeys.size(); ++i) {
                String rowKey = rowKeys.get(i);
                Map<String, Map<String, Object>> qualifierMap = qualifierMaps.get(i);

                Put put = qualifierMapToPut(rowKey, qualifierMap);
                puts.add(put);
            }

            table.put(puts);
            return null;
        });
    }

    // --- get ---

    public T get(String rowKey) {
        Map<String, Map<String, Object>> familyMap = get(rowKey, null);
        return mapObject(rowKey, familyMap, objectMapper);
    }

    public Map<String, Map<String, Object>> get(String rowKey, Object none) {
        return invokeOnTable(table -> {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.setMaxVersions(1);
            Result result = table.get(get);
            return rowMapper.mapRow(result, 0);
        });
    }

    // --- gets ---

    public List<T> gets(List<String> rowKeys) {
        List<Map<String, Map<String, Object>>> maps = gets(rowKeys, null);
        return mapObject(rowKeys, maps, objectMapper);
    }

    public List<Map<String, Map<String, Object>>> gets(List<String> rowKeys, Object none) {
        return invokeOnTable(table -> {
            List<org.apache.hadoop.hbase.client.Get> gets = new ArrayList<>(rowKeys.size());
            for (String rowKey : rowKeys) {
                Get get = new Get(Bytes.toBytes(rowKey));
                get.setMaxVersions(1);
                gets.add(get);
            }

            Result[] results = table.get(gets);
            List<Map<String, Map<String, Object>>> resultMap = new ArrayList<>();
            for (int i = 0; i < results.length; ++i) {
                resultMap.add(rowMapper.mapRow(results[i], i));
            }

            return resultMap;
        });
    }

    // --- scan ---

    public List<T> scan(String startRow, String stopRow) {
        Scan scan = new Scan();
        scan.setMaxVersions(1);
        if (!Strings.isNullOrEmpty(startRow)) {
            scan.setStartRow(Bytes.toBytes(startRow));
        }
        if (!Strings.isNullOrEmpty(stopRow)) {
            scan.setStopRow(Bytes.toBytes(stopRow));
        }
        List<Pair<String, Map<String, Map<String, Object>>>> pairs = template.find(tableName, scan, rowMapperWithRowKey);
        List<String> rowKeys = new ArrayList<>(pairs.size());
        List<Map<String, Map<String, Object>>> familyMaps = new ArrayList<>();
        for (Pair<String, Map<String, Map<String, Object>>> pair : pairs) {
            rowKeys.add(pair.getLeft());
            familyMaps.add(pair.getRight());
        }

        return mapObject(rowKeys, familyMaps, objectMapper);
    }

    public List<Map<String, Map<String, Object>>> scan(String startRow, String stopRow, Object none) {
        Scan scan = new Scan();
        scan.setMaxVersions(1);
        if (!Strings.isNullOrEmpty(startRow)) {
            scan.setStartRow(Bytes.toBytes(startRow));
        }
        if (!Strings.isNullOrEmpty(stopRow)) {
            scan.setStopRow(Bytes.toBytes(stopRow));
        }

        return template.find(tableName, scan, rowMapper);
    }

    // --- delete ---

    public void delete(String rowKey) {
        invokeOnTable(table -> {
            table.delete(new Delete(Bytes.toBytes(rowKey)));
            return null;
        });
    }

    // --- deletes ---

    public void deletes(List<String> rowKeys) {
        if (CollectionUtils.isEmpty(rowKeys)) {
            return;
        }

        invokeOnTable(table -> {
            List<org.apache.hadoop.hbase.client.Delete> deletes = new ArrayList<>(rowKeys.size());
            table.delete(deletes);
            return null;
        });
    }

    // --- cas ---

    public boolean cas(String rowKey, String family, String qualifier, Object oldVal, Object newVal) {
        return invokeOnTable(table -> {
            byte[] rowKeyBytes = Bytes.toBytes(rowKey);
            byte[] familyBytes = Bytes.toBytes(family);
            byte[] qualifierBytes = Bytes.toBytes(qualifier);
            byte[] oldValBytes = serializer.serialize(oldVal);
            byte[] newValBytes = serializer.serialize(newVal);

            Put put = new Put(rowKeyBytes);
            put.add(familyBytes, qualifierBytes, newValBytes);

            return table.checkAndPut(rowKeyBytes, familyBytes, qualifierBytes, oldValBytes, put);
        });
    }

    // --- cad --

    public boolean cad(String rowKey, String family, String qualifier, Object oldVal) {
        return invokeOnTable(table -> {
            byte[] rowKeyBytes = Bytes.toBytes(rowKey);
            byte[] familyBytes = Bytes.toBytes(family);
            byte[] qualifierBytes = Bytes.toBytes(qualifier);
            byte[] oldValBytes = serializer.serialize(oldVal);

            // match 则删除整行数据
            Delete delete = new Delete(rowKeyBytes);
            return table.checkAndDelete(rowKeyBytes, familyBytes, qualifierBytes, oldValBytes, delete);
        });
    }

    // --- incr ---

    public long incr(String rowKey, String family, String qualifier, long amount) {
        return invokeOnTable(table -> table.incrementColumnValue(Bytes.toBytes(rowKey), Bytes.toBytes(family), Bytes.toBytes(qualifier), amount));
    }

    public Map<String, Map<String, Long>> incrs(String rowKey, Map<String, Map<String, Long>> amounts) {
        if (CollectionUtils.isEmpty(amounts)) {
            return Collections.emptyMap();
        }
        return doIncrements(rowKey, amounts, 1L);
    }

    // ---- decr ----

    public long decr(String rowKey, String family, String qualifier, long amount) {
        return incr(rowKey, family, qualifier, -1 * amount);
    }

    public Map<String, Map<String, Long>> decrs(String rowKey, Map<String, Map<String, Long>> amounts) {
        if (CollectionUtils.isEmpty(amounts)) {
            return Collections.emptyMap();
        }
        return doIncrements(rowKey, amounts, -1L);
    }

    private Map<String, Map<String, Long>> doIncrements(String rowKey, Map<String, Map<String, Long>> amounts, long signed) {
        return invokeOnTable(table -> {
            Increment increment = new Increment(Bytes.toBytes(rowKey));
            for (Map.Entry<String, Map<String, Long>> familyEntry : amounts.entrySet()) {
                byte[] family = Bytes.toBytes(familyEntry.getKey());

                for (Map.Entry<String, Long> qualifierEntry : familyEntry.getValue().entrySet()) {
                    byte[] qualifier = Bytes.toBytes(qualifierEntry.getKey());
                    long amount = signed * qualifierEntry.getValue();
                    increment.addColumn(family, qualifier, amount);
                }
            }

            Result result = table.increment(increment);
            return HBaseHelper.resultToColumnMap(result, Bytes::toLong);
        });
    }

    // --- invokeOnTable ---

    public <V> V invokeOnTable(TableInvoker<V> tableInvoker) {
        return HBaseHelper.invokeOnTable(tableName, template, tableInvoker);
    }

    protected HbaseTemplate getTemplate() {
        return this.template;
    }


    // --- helpers ---
    private Class<T> getType() {
        Type superclass = this.getClass().getGenericSuperclass();
        while (!superclass.equals(Object.class) && !(superclass instanceof ParameterizedType)) {
            superclass = ((Class) superclass).getGenericSuperclass();
        }

        if (!(superclass instanceof ParameterizedType)) {
            throw new RuntimeException(String.format("class:%s extends HBaseBatis<T> not replace generic type <T>", this.getClass().getName()));
        }

        return (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    private Put qualifierMapToPut(String rowKey, Map<String, Map<String, Object>> qualifierMap) {
        Put put = new Put(Bytes.toBytes(rowKey));
        for (Map.Entry<String, Map<String, Object>> familyEntry : qualifierMap.entrySet()) {
            byte[] family = Bytes.toBytes(familyEntry.getKey());
            for (Map.Entry<String, Object> qualifierEntry : familyEntry.getValue().entrySet()) {
                String qualifier = qualifierEntry.getKey();
                byte[] value = serializer.serialize(qualifierEntry.getValue());
                put.add(family, Bytes.toBytes(qualifier), value);
            }
        }

        return put;
    }
}
