package com.github.jbox.h2;

import com.github.jbox.serializer.ISerializer;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.sql.Types.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 10:44 AM.
 */
class TinyHelpers {

    private static final ConcurrentMap<Class<? extends ISerializer>, ISerializer> serializers = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, JdbcTemplate> templates = new ConcurrentHashMap<>();

    static JdbcTemplate getTemplate(String h2path) {
        return templates.computeIfAbsent(h2path, path -> {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(String.format("jdbc:h2:%s;AUTO_RECONNECT=TRUE;AUTO_SERVER=TRUE", path));
            dataSource.setUsername("jbox");
            dataSource.setPassword("jbox");
            JdbcTemplate template = new JdbcTemplate(dataSource);
            template.afterPropertiesSet();

            return template;
        });
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> getModelType(Object obj) {
        Type superclass = obj.getClass().getGenericSuperclass();
        while (!superclass.equals(Object.class) && !(superclass instanceof ParameterizedType)) {
            superclass = ((Class) superclass).getGenericSuperclass();
        }

        if (!(superclass instanceof ParameterizedType)) {
            throw new RuntimeException(String.format("class:%s extends H2Batis<T> not replace generic type <T>", obj.getClass().getName()));
        }

        return (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    private static final ConcurrentMap<Class<?>, List<Field>> fields = new ConcurrentHashMap<>();

    static List<Field> getFields(final Class<?> clazz) {
        return fields.computeIfAbsent(clazz, _cls -> {
            List<Field> fields = new ArrayList<>();
            Class<?> tmp = clazz;
            while (tmp != Object.class) {
                for (Field field : tmp.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                tmp = tmp.getSuperclass();
            }

            return fields;
        });
    }

    /*  Java -> SQL */
    private static final ConcurrentMap<Class<?>, String> tableNames = new ConcurrentHashMap<>();

    static String getTableName(Class<?> type) {
        return tableNames.computeIfAbsent(type, _type -> "T_" + type.getName().toUpperCase().replace(".", "_"));
    }

    // 对象属性名 -> 表字段名
    private static final ConcurrentMap<Field, String> field2columnNames = new ConcurrentHashMap<>();

    static String getColumnName(Class<?> type, Field field) {
        String columnName = field2columnNames.computeIfAbsent(field, _field -> _field.getName().toUpperCase());

        columnName2fields.computeIfAbsent(type, _k -> new ConcurrentHashMap<>()).put(columnName, field);
        return columnName;
    }

    // 对象属性名(s) -> 表字段名(s)
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Map<String, String>>> columnsNames = new ConcurrentHashMap<>();

    static Map<String, String> getColumnsNames(Class<?> type, Collection<String> fieldNames) {
        return getColumnsNames(type, fieldNames.toArray(new String[0]));
    }

    static Map<String, String> getColumnsNames(Class<?> type, String[] fieldNames) {
        return columnsNames
                .computeIfAbsent(type, _type -> new ConcurrentHashMap<>())
                .computeIfAbsent(Joiner.on("@").join(fieldNames), _k -> {
                    Map<String, String> columnNames = new LinkedHashMap<>(fieldNames.length);
                    for (String fieldName : fieldNames) {
                        for (Field field : getFields(type)) {
                            if (StringUtils.equals(fieldName, field.getName())) {
                                columnNames.put(fieldName, getColumnName(type, field));
                            }
                        }
                    }

                    Preconditions.checkState(fieldNames.length == columnNames.size());

                    return columnNames;
                });
    }

    // 对象属性类型 -> 表字段类型
    private static final ConcurrentMap<Class<?>, String> java2sqlTypes = new ConcurrentHashMap<>();

    static {
        java2sqlTypes.put(byte.class, "TINYINT");
        java2sqlTypes.put(Byte.class, "TINYINT");
        java2sqlTypes.put(short.class, "SMALLINT");
        java2sqlTypes.put(Short.class, "SMALLINT");
        java2sqlTypes.put(int.class, "INT");
        java2sqlTypes.put(Integer.class, "INT");
        java2sqlTypes.put(long.class, "BIGINT");
        java2sqlTypes.put(Long.class, "BIGINT");
        java2sqlTypes.put(float.class, "REAL");
        java2sqlTypes.put(Float.class, "REAL");
        java2sqlTypes.put(double.class, "DOUBLE");
        java2sqlTypes.put(Double.class, "DOUBLE");
        java2sqlTypes.put(boolean.class, "BOOLEAN");
        java2sqlTypes.put(Boolean.class, "BOOLEAN");
        java2sqlTypes.put(char.class, "CHAR(10)");
        java2sqlTypes.put(Character.class, "CHAR(10)");
        java2sqlTypes.put(String.class, "VARCHAR(255)");
        java2sqlTypes.put(BigDecimal.class, "DECIMAL");
        java2sqlTypes.put(LocalTime.class, "TIME");
        java2sqlTypes.put(LocalDate.class, "DATE");
        java2sqlTypes.put(Date.class, "TIMESTAMP");
        java2sqlTypes.put(Object.class, "BINARY(1000)");
    }

    private static final ConcurrentMap<Field, String> columnTypes = new ConcurrentHashMap<>();

    static String getColumnType(Field field) {
        return columnTypes.computeIfAbsent(field, _field -> {
            Column column;
            if ((column = field.getAnnotation(Column.class)) != null && !Strings.isNullOrEmpty(column.type())) {
                return column.type().toUpperCase();
            }

            String type = java2sqlTypes.get(field.getType());
            if (type == null && (column == null || column.serializer() == ISerializer.class)) {
                throw new RuntimeException("not support type:[" + field.getType().getName() + "] default, need specified 'serializer'");
            }

            return type;
        });
    }

    // 判定属性是否可以为Null
    private static final ConcurrentMap<Field, String> columnNulls = new ConcurrentHashMap<>();

    static String getColumnNull(Field field) {
        return columnNulls.computeIfAbsent(field, _field -> {
            if (field.isAnnotationPresent(Column.class)) {
                return field.getAnnotation(Column.class).nullable() ? "" : "NOT NULL";
            }
            return "";
        });
    }

    // 对象属性值 -> 表字段值
    static Object getColumnValue(Field field, Object model) {
        try {
            Object value = field.get(model);
            if (value == null) {
                return null;
            }

            Column column;
            if ((column = field.getAnnotation(Column.class)) != null && column.serializer() != ISerializer.class) {
                ISerializer serializer = serializers.computeIfAbsent(column.serializer(), _k -> {
                    try {
                        return column.serializer().newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });

                value = serializer.serialize(value);
            }

            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /* SQL -> Java */

    // 表字段 -> 对象属性
    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> columnName2fields = new ConcurrentHashMap<>();

    // 在启动时会自动扫描model并建表, 此时columnName2fields内容就已经构建好了, 直接使用即可
    static Field getColumnField(Class<?> type, String columnName) {
        return columnName2fields.get(type).get(columnName);
    }

    // SqlResult -> 对象属性值
    private interface Function {
        Object apply(ResultSet rs, int columnIdx, Field field) throws SQLException;
    }

    private static final ConcurrentMap<Integer, Function> extractor = new ConcurrentHashMap<>();

    static {
        Function binary = (rs, idx, field) -> {
            Column column;
            Preconditions.checkState((column = field.getAnnotation(Column.class)) != null && column.serializer() != ISerializer.class);

            ISerializer serializer = serializers.computeIfAbsent(column.serializer(), _k -> {
                try {
                    return column.serializer().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

            return serializer.deserialize(rs.getBytes(idx));
        };

        extractor.put(BIT, (rs, idx, field) -> rs.getByte(idx));
        extractor.put(TINYINT, (rs, idx, field) -> rs.getByte(idx));
        extractor.put(SMALLINT, (rs, idx, field) -> rs.getShort(idx));
        extractor.put(INTEGER, (rs, idx, field) -> rs.getInt(idx));
        extractor.put(BIGINT, (rs, idx, field) -> rs.getLong(idx));
        extractor.put(FLOAT, (rs, idx, field) -> rs.getFloat(idx));
        extractor.put(REAL, (rs, idx, field) -> rs.getFloat(idx));
        extractor.put(DOUBLE, (rs, idx, field) -> rs.getDouble(idx));
        extractor.put(BOOLEAN, (rs, idx, field) -> rs.getBoolean(idx));
        extractor.put(CHAR, (rs, idx, field) -> rs.getString(idx).charAt(0));
        extractor.put(VARCHAR, (rs, idx, field) -> rs.getString(idx));
        extractor.put(DECIMAL, (rs, idx, field) -> rs.getBigDecimal(idx));
        extractor.put(TIME, (rs, idx, field) -> rs.getTime(idx).toLocalTime());
        extractor.put(DATE, (rs, idx, field) -> rs.getDate(idx).toLocalDate());
        extractor.put(TIMESTAMP, (rs, idx, field) -> rs.getTimestamp(idx));
        extractor.put(BINARY, binary);
        extractor.put(VARBINARY, binary);
    }

    static Object getColumnValue(ResultSet rs, int idx, int columnType, Field field) {
        try {
            return extractor.get(columnType).apply(rs, idx, field);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
