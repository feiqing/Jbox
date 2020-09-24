package com.github.jbox.h2;

import com.github.jbox.utils.Collections3;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/9/24 10:22 AM.
 */
public class H2Batis<T extends H2BaseMode> {

    private Class<T> type;

    private RowMapper mapper;

    private JdbcTemplate template;

    public H2Batis(String h2path) {
        this.type = TinyHelpers.getModelType(this);
        this.mapper = new RowMapper();
        this.template = TinyHelpers.getTemplate(h2path);
        initTable();
        initIndex();
    }

    public H2Batis(Class<T> type, String h2path) {
        this.type = type;
        this.mapper = new RowMapper();
        this.template = TinyHelpers.getTemplate(h2path);
        initTable();
        initIndex();
    }

    public T selectById(long id) {
        return selectOne(Collections.singletonMap("ID", id));
    }

    public T selectOne(Map<String, Object> where) {
        List<T> rs = select(where);
        if (Collections3.isEmpty(rs)) {
            return null;
        }
        return rs.iterator().next();
    }

    public List<T> selectAll() {
        return select(Collections.emptyMap());
    }

    public List<T> select(Map<String, Object> where) {
        Map<String, String> columnsNames = TinyHelpers.getColumnsNames(type, where.keySet());

        List<String> placeholders = new ArrayList<>(where.size());
        List<Object> values = new ArrayList<>(where.size());
        for (Map.Entry<String, Object> entry : where.entrySet()) {
            placeholders.add(columnsNames.get(entry.getKey()) + " = ?");
            values.add(entry.getValue());
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(TinyHelpers.getTableName(type));
        if (!placeholders.isEmpty()) {
            sql.append(" WHERE ").append(Joiner.on(" AND ").join(placeholders));
        }

        return template.query(sql.toString(), mapper, values.toArray());
    }

    public long insert(T t) {
        List<Field> fields = TinyHelpers.getFields(type);

        List<String> columnNames = new ArrayList<>(fields.size());
        List<Object> columnValues = new ArrayList<>(fields.size());
        List<String> columnPlaceholders = new ArrayList<>(fields.size());
        for (Field field : fields) {
            if (field.getName().equals("id") && TinyHelpers.getColumnValue(field, t) == null) {
                continue;
            } else if (field.getName().equals("gmt_create")) {
                continue;
            } else if (field.getName().equals("gmt_modified")) {
                continue;
            }

            columnNames.add(TinyHelpers.getColumnName(type, field));
            columnValues.add(TinyHelpers.getColumnValue(field, t));
            columnPlaceholders.add("?");
        }
        columnNames.add("GMT_CREATE");
        columnNames.add("GMT_MODIFIED");
        columnPlaceholders.add("NOW()");
        columnPlaceholders.add("NOW()");


        StringBuilder sql = new StringBuilder("INSERT INTO ").append(TinyHelpers.getTableName(type)).append("(").append(Joiner.on(", ").join(columnNames)).append(")")
                .append("\n\tVALUES(").append(Joiner.on(",").join(columnPlaceholders)).append(")");

        Preconditions.checkState(template.update(sql.toString(), columnValues.toArray(new Object[0])) > 0);
        if (t.getId() == null) {
            // todo
            t.setId(template.queryForObject("SELECT MAX(ID) FROM " + TinyHelpers.getTableName(type), Long.class));
        }

        return t.getId();
    }

    public int upsert(T t, String... keys) {
        List<Field> fields = TinyHelpers.getFields(type);

        List<String> columnNames = new ArrayList<>(fields.size());
        List<Object> columnValues = new ArrayList<>(fields.size());
        List<String> columnPlaceholders = new ArrayList<>(fields.size());
        for (Field field : fields) {
            if (field.getName().equals("id") && TinyHelpers.getColumnValue(field, t) == null) {
                continue;
            } else if (field.getName().equals("gmt_create")) {
                continue;
            } else if (field.getName().equals("gmt_modified")) {
                continue;
            }

            columnNames.add(TinyHelpers.getColumnName(type, field));
            columnValues.add(TinyHelpers.getColumnValue(field, t));
            columnPlaceholders.add("?");
        }
        columnNames.add("GMT_CREATE");
        columnNames.add("GMT_MODIFIED");
        columnPlaceholders.add("NOW()");
        columnPlaceholders.add("NOW()");

        StringBuilder sql = new StringBuilder("MERGE INTO ").append(TinyHelpers.getTableName(type)).append("(").append(Joiner.on(", ").join(columnNames)).append(")")
                .append("\n\tKEY(").append(Joiner.on(",").join(TinyHelpers.getColumnsNames(type, keys).values())).append(")")
                .append("\n\tVALUES(").append(Joiner.on(",").join(columnPlaceholders)).append(")");

        return template.update(sql.toString(), columnValues.toArray(new Object[0]));
    }

    public int updateById(T t) {
        return update(t, Collections.singletonMap("ID", t.getId()));
    }

    public int update(T set, Map<String, Object> where) {
        Map<String, Object> setMap = new HashMap<>();
        for (Field field : TinyHelpers.getFields(type)) {
            setMap.put(field.getName(), TinyHelpers.getColumnValue(field, set));
        }

        return update(setMap, where);
    }

    /**
     * @param set:  set.value直接为DB存储的类型值(尤其在column.serializer指定了序列化器的时候需要当心)
     * @param where
     * @return
     */
    public int update(Map<String, Object> set, Map<String, Object> where) {
        Map<String, String> columnsNames = TinyHelpers.getColumnsNames(type, where.keySet());

        List<Object> values = new ArrayList<>(set.size() + where.size());

        List<String> setPhs = new ArrayList<>(set.size());
        for (Map.Entry<String, Object> entry : set.entrySet()) {
//            if (where.containsKey(entry.getKey())) {
//                continue;
//            }
            if (StringUtils.equals(entry.getKey(), "gmt_create") || StringUtils.equals(entry.getKey(), "gmt_modified")) {
                continue;
            }

            setPhs.add(columnsNames.get(entry.getKey()) + " = ?");
            values.add(entry.getValue());
        }
        setPhs.add("GMT_MODIFIED = NOW()");

//        if (setPhs.isEmpty()) {
//            return 0;
//        }

        List<String> wherePhs = new ArrayList<>(where.size());
        for (Map.Entry<String, Object> entry : where.entrySet()) {
            wherePhs.add(columnsNames.get(entry.getKey()) + " = ?");
            values.add(entry.getValue());
        }

        StringBuilder sql = new StringBuilder("UPDATE ").append(TinyHelpers.getTableName(type));
        if (!setPhs.isEmpty()) {
            sql.append(" SET ").append(Joiner.on(", ").join(wherePhs));
        }
        if (!wherePhs.isEmpty()) {
            sql.append(" WHERE ").append(Joiner.on(" AND ").join(wherePhs));
        }

        return template.update(sql.toString(), mapper, values.toArray());
    }

    public int deleteById(long id) {
        return delete(Collections.singletonMap("ID", id));
    }

    public int delete(Map<String, Object> where) {
        Map<String, String> columnsNames = TinyHelpers.getColumnsNames(type, where.keySet());

        List<String> placeholders = new ArrayList<>(where.size());
        List<Object> values = new ArrayList<>(where.size());
        for (Map.Entry<String, Object> entry : where.entrySet()) {
            placeholders.add(columnsNames.get(entry.getKey()) + " = ?");
            values.add(entry.getValue());
        }

        StringBuilder sql = new StringBuilder("DELETE FROM ").append(TinyHelpers.getTableName(type));
        if (!placeholders.isEmpty()) {
            sql.append(" WHERE ").append(Joiner.on(" and ").join(placeholders));
        }

        return template.update(sql.toString(), mapper, values.toArray());
    }

    private void initTable() {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(TinyHelpers.getTableName(type))
                .append("(\n");

        List<String> columns = new LinkedList<>();
        for (Field field : TinyHelpers.getFields(type)) {
            columns.add("\t" + TinyHelpers.getColumnName(type, field) +
                    "\t" + TinyHelpers.getColumnType(field) +
                    "\t" + TinyHelpers.getColumnNull(field));
        }

        sql.append(Joiner.on(",\n").join(columns));
        sql.append("\n);");

        template.execute(sql.toString());
    }

    private void initIndex() {
        for (Index index : type.getDeclaredAnnotationsByType(Index.class)) {
            StringBuilder sql = new StringBuilder("CREATE")
                    .append(index.unique() ? " UNIQUE " : " ")
                    .append("INDEX IF NOT EXISTS ")
                    .append(index.name())
                    .append(" ON ")
                    .append(TinyHelpers.getTableName(type))
                    .append("(")
                    .append(Joiner.on(", ").join(TinyHelpers.getColumnsNames(type, index.fields()).values()))
                    .append(")");

            template.execute(sql.toString());
        }
    }

    private class RowMapper implements org.springframework.jdbc.core.RowMapper<T> {

        @Override
        public T mapRow(ResultSet rs, int rowNum) throws SQLException {
            T obj;
            try {
                obj = type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); ++i) {
                Field field = TinyHelpers.getColumnField(type, meta.getColumnName(i));
                Object columnValue = TinyHelpers.getColumnValue(rs, i, meta.getColumnType(i), field);

                try {
                    field.set(obj, columnValue);
                } catch (IllegalAccessException ignored) {
                    // no case
                }
            }

            return obj;
        }
    }
}
