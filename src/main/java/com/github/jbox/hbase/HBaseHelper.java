package com.github.jbox.hbase;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.data.hadoop.hbase.HbaseTemplate;
import org.springframework.data.hadoop.hbase.HbaseUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 20:38:00.
 */
class HBaseHelper {

    private static final ConcurrentMap<Class<?>, String> tableNameCache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Field, String> familyCache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Field, String> qualifierCache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Optional<Field>> fieldCache = new ConcurrentHashMap<>();

    // --- getters ---
    public static String getTableName(Class<?> clazz) {
        return tableNameCache.computeIfAbsent(clazz, (key) -> {

            Table table = clazz.getDeclaredAnnotation(Table.class);
            if (table != null) {
                Preconditions.checkState(StringUtils.isNotBlank(table.value()));
                return table.value();
            }

            String clazzName = clazz.getSimpleName();
            return clazzName.substring(0, 1).toLowerCase() + clazzName.substring(1);
        });
    }

    private static String getFamily(Field field, String defaultFamily) {
        return familyCache.computeIfAbsent(field, (key) -> {
            Qualifier qualifier = field.getDeclaredAnnotation(Qualifier.class);
            if (qualifier != null && StringUtils.isNotBlank(qualifier.family())) {
                return qualifier.family();
            }

            return defaultFamily;
        });
    }

    private static String getQualifier(Field field) {
        return qualifierCache.computeIfAbsent(field, (key) -> {
            Qualifier qualifier = field.getDeclaredAnnotation(Qualifier.class);
            if (qualifier != null) {
                Preconditions.checkState(StringUtils.isNotBlank(qualifier.value()));
                return qualifier.value();
            }

            return field.getName();
        });
    }

    public static Field getFieldByQualifier(Class<?> clazz, String family, String qualifier, String defaultFamily) {
        String key = String.format("%s-%s-%s", clazz.getName(), family, qualifier);
        Optional<Field> optional = fieldCache.computeIfAbsent(key, (k) -> {
            List<Field> fieldsWithAnnotation = FieldUtils.getFieldsListWithAnnotation(clazz, Qualifier.class);
            for (Field field : fieldsWithAnnotation) {
                Qualifier qualifierAnno = field.getDeclaredAnnotation(Qualifier.class);
                if (qualifierAnno.exclude()) {
                    continue;
                }

                if (StringUtils.equals(qualifierAnno.value(), qualifier)) {
                    if (StringUtils.equals(qualifierAnno.family(), family)) {
                        return Optional.of(field);
                    } else if (StringUtils.isBlank(qualifierAnno.family()) && StringUtils.equals(family, defaultFamily)) {
                        return Optional.of(field);
                    }
                }
            }

            List<Field> noneAnnoFields = FieldUtils.getAllFieldsList(clazz);
            noneAnnoFields.removeAll(fieldsWithAnnotation);

            for (Field field : noneAnnoFields) {
                if (StringUtils.equals(family, defaultFamily) && StringUtils.equals(field.getName(), qualifier)) {
                    return Optional.of(field);
                }
            }

            return Optional.empty();
        });

        return optional.orElse(null);
    }

    // ---- invokeOnTable ----
    @SuppressWarnings("all")
    public static <T> T invokeOnTable(String tableName, HbaseTemplate template, TableInvoker<T> tableInvoker) {
        HTableInterface table = HbaseUtils.getHTable(tableName, template.getConfiguration());
        try {
            return tableInvoker.invoke(table);
        } catch (Exception e) {
            throw HbaseUtils.convertHbaseException(e);
        } finally {
            HbaseUtils.releaseTable(tableName, table);
        }
    }

    public static <T> T mapObject(String rowKey, Map<String, Map<String, Object>> columnMap, ObjectMapper<T> mapper) {
        try {
            return mapper.mapObject(rowKey, columnMap);
        } catch (Exception e) {
            throw HbaseUtils.convertHbaseException(e);
        }
    }

    public static <T> List<T> mapObject(List<String> rowKeys, List<Map<String, Map<String, Object>>> columnMaps, ObjectMapper<T> mapper) {
        List<T> results = new ArrayList<>(rowKeys.size());
        for (int i = 0; i < rowKeys.size(); ++i) {
            String rowKey = rowKeys.get(i);
            Map<String, Map<String, Object>> columnMap = columnMaps.get(i);

            results.add(mapObject(rowKey, columnMap, mapper));
        }

        return results;
    }

    public static Map<String/*family*/, Map<String/*qualifier*/, Object>> objToColumnMap(Object obj, String defaultFamily) {
        Map<String, Map<String, Object>> columnMap = new HashMap<>();
        ReflectionUtils.doWithFields(
                obj.getClass(),
                field -> {
                    ReflectionUtils.makeAccessible(field);
                    Object value = ReflectionUtils.getField(field, obj);
                    if (value == null) {
                        return;
                    }

                    String family = getFamily(field, defaultFamily);
                    String qualifier = getQualifier(field);
                    columnMap.computeIfAbsent(family, (key) -> new HashMap<>())
                            .put(qualifier, value);
                },
                field -> {
                    if (Modifier.isStatic(field.getModifiers())) {
                        return false;
                    }

                    Qualifier qualifier = field.getDeclaredAnnotation(Qualifier.class);
                    if (qualifier == null) {
                        return true;
                    }

                    return !qualifier.exclude();
                });

        return columnMap;
    }

    public static <T> Map<String, Map<String, T>> resultToColumnMap(Result result, Function<byte[], T> deSerializer) {
        Map<String, Map<String, T>> columnMap = new TreeMap<>();
        NavigableMap<byte[], NavigableMap<byte[], byte[]>> noVersionMap;
        if (result == null || CollectionUtils.isEmpty(noVersionMap = result.getNoVersionMap())) {
            return Collections.emptyMap();
        }

        for (Map.Entry<byte[], NavigableMap<byte[], byte[]>> familyEntry : noVersionMap.entrySet()) {
            String family = Bytes.toString(familyEntry.getKey());

            for (Map.Entry<byte[], byte[]> qualifierEntry : familyEntry.getValue().entrySet()) {
                String qualifier = Bytes.toString(qualifierEntry.getKey());

                T value = deSerializer.apply(qualifierEntry.getValue());
                if (value == null) {
                    continue;
                }

                columnMap
                        .computeIfAbsent(family, (k) -> new TreeMap<>())
                        .put(qualifier, value);
            }
        }

        return columnMap;
    }
}
