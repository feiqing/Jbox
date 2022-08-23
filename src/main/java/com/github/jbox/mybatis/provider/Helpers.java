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
                    Table annotation = clazz.getAnnotation(Table.class);
                    TableAnno tableAnno = new TableAnno();
                    if (annotation != null) {
                        tableAnno.table = Strings.isNullOrEmpty(annotation.name()) ? annotation.value() : annotation.name();
                        tableAnno.primaryKey = annotation.primaryKey();
                        tableAnno.sequence = annotation.sequence();
                        tableAnno.gmtCreate = annotation.gmtCreate();
                        tableAnno.gmtModified = annotation.gmtModified();
                    }

                    if (!Strings.isNullOrEmpty(tableAnno.table)) {
                        return tableAnno;
                    }

                    String entityName = entityType.getSimpleName();
                    for (String suffix : ENTITY_SUFFIX) {
                        int idx = StringUtils.indexOfIgnoreCase(entityName, suffix);
                        if (idx != -1) {
                            tableAnno.table = hump2line(String.valueOf(entityName.charAt(0)).toLowerCase() + StringUtils.substring(entityName, 1, idx));
                            return tableAnno;
                        }
                    }

                    tableAnno.table = hump2line(String.valueOf(entityName.charAt(0)).toLowerCase() + StringUtils.substring(entityName, 1));

                    if (Strings.isNullOrEmpty(tableAnno.sequence)) {
                        tableAnno.sequence = tableAnno.table;
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
                    || Modifier.isFinal(field.getModifiers()) // todo ?
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
                columnName = Strings.isNullOrEmpty(annotation.name()) ? annotation.value() : annotation.name();
            }

            if (Strings.isNullOrEmpty(columnName)) {
                columnName = hump2line(field.getName());
            }

            columnMap.put("`" + columnName + "`", "#{" + field.getName() + "}");
        }

        return columnMap;
    }


    static String hump2line(String hump) {
        return hump2line.computeIfAbsent(hump, _k -> hump.replaceAll("[A-Z]", "_$0").toLowerCase());
    }
}
