package com.github.jbox.mybatis.provider;

import com.github.jbox.mybatis.Column;
import com.github.jbox.mybatis.Table;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.jbox.mybatis.Table.PRI_AUTO_INCREMENT;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/16 10:55.
 */
class Helpers {

    private static final ConcurrentMap<Class<?>, Class<?>> mapperType2entityType = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, TableAnno> entityType2tableAnno = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, Map<String, String>> entityType2columnMap = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, String> hump2line = new ConcurrentHashMap<>();

    private static final List<String> ENTITY_SUFFIX = new LinkedList<>();

    private static final Set<String> EXCLUDE_FIELDS = Sets.newHashSet("id", "gmtCreate", "gmtModified");

    public static void addSuffix(String suffix) {
        ENTITY_SUFFIX.add(suffix);
    }

    static {
        addSuffix("ENTITY");
        addSuffix("PO");
        addSuffix("DO");
        addSuffix("DTO");
        addSuffix("MODEL");
        addSuffix("POJO");
    }

    static TableAnno tableAnno(ProviderContext context) {
        Class<?> entityType = mapperType2entityType.computeIfAbsent(context.getMapperType(), clazz -> (Class<?>) ((ParameterizedType) (clazz.getGenericInterfaces()[0])).getActualTypeArguments()[1]);
        return tableAnno(entityType);
    }

    static TableAnno tableAnno(Class<?> entityType) {
        return entityType2tableAnno
                .computeIfAbsent(entityType, clazz -> {
                    TableAnno tableAnno = new TableAnno();

                    Table table = clazz.getAnnotation(Table.class);
                    if (table == null) {
                        tableAnno.table = "";
                        tableAnno.primaryKey = PRI_AUTO_INCREMENT;
                        tableAnno.sequenceId = "";
                        tableAnno.useGmtCreate = true;
                        tableAnno.useGmtModified = true;
                    } else {
                        tableAnno.table = !Strings.isNullOrEmpty(table.table()) ? table.table() : table.value();
                        tableAnno.primaryKey = table.primaryKey();
                        tableAnno.sequenceId = table.sequenceId();
                        tableAnno.useGmtCreate = table.useGmtCreate();
                        tableAnno.useGmtModified = table.useGmtModified();
                    }

                    if (Strings.isNullOrEmpty(tableAnno.table)) {
                        boolean founded = false;
                        String entityTypeName = entityType.getSimpleName();
                        for (String suffix : ENTITY_SUFFIX) {
                            int idx = StringUtils.indexOfIgnoreCase(entityTypeName, suffix);
                            if (idx != -1) {
                                tableAnno.table = hump2line(String.valueOf(entityTypeName.charAt(0)).toLowerCase() + StringUtils.substring(entityTypeName, 1, idx));
                                founded = true;
                                break;
                            }
                        }

                        if (!founded) {
                            tableAnno.table = hump2line(String.valueOf(entityTypeName.charAt(0)).toLowerCase() + StringUtils.substring(entityTypeName, 1));
                        }
                    }

                    if (Strings.isNullOrEmpty(tableAnno.sequenceId)) {
                        tableAnno.sequenceId = tableAnno.table;
                    }

                    return tableAnno;
                });
    }


    static Map<String, String> entityColumnMap(Class<?> entityType) {
        return entityType2columnMap.computeIfAbsent(entityType, Helpers::_entityColumnMap);
    }

    private static Map<String, String> _entityColumnMap(Class<?> entityType) {
        Map<String, String> columnMap = new LinkedHashMap<>();
        List<Field> fields = getAllFieldsList(entityType);
        for (Field field : fields) {
            if (field == null
                    || Modifier.isStatic(field.getModifiers())
                    // || Modifier.isFinal(field.getModifiers()) todo ?
                    || EXCLUDE_FIELDS.contains(field.getName())
            ) {
                continue;
            }

            Column annotation = field.getAnnotation(Column.class);
            if (annotation != null && annotation.exclude()) {
                continue;
            }

            String columnName = null;
            if (annotation != null) {
                columnName = Strings.isNullOrEmpty(annotation.column()) ? annotation.value() : annotation.column();
            }

            if (Strings.isNullOrEmpty(columnName)) {
                columnName = hump2line(field.getName());
            }

            columnMap.put(columnName, field.getName());
        }

        return columnMap;
    }

    static String hump2line(String hump) {
        return hump2line.computeIfAbsent(hump, _k -> hump.replaceAll("[A-Z]", "_$0").toLowerCase());
    }
}
