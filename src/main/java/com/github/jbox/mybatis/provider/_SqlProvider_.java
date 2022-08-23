package com.github.jbox.mybatis.provider;

import com.github.jbox.mybatis.BaseEntity;
import com.github.jbox.mybatis.Table;
import com.github.jbox.mybatis.Where;
import com.github.jbox.mybatis.sequence.SequenceRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.jbox.mybatis.provider.Helpers.*;
import static java.util.stream.Collectors.joining;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 10:12 上午.
 */
@SuppressWarnings("all")
public class _SqlProvider_ {

    public String select(ProviderContext context, Where where) {
        return new StringBuilder("SELECT * FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .append(getOrderBy(where))
                .append(getLimit(where))
                .toString();
    }

    public String selectOne(ProviderContext context, Where where) {
        return new StringBuilder("SELECT * FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .append(getOrderBy(where))
                .append(getLimit(where))
                .toString();
    }

    public String selectByIds(ProviderContext context, Collection<Object> ids) {
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        return new StringBuilder("SELECT * FROM ")
                .append(tableAnno(context).table)
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }

    public String count(ProviderContext context, Where where) {
        return new StringBuilder("SELECT COUNT(*) FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .toString();
    }

    public String insert(ProviderContext context, BaseEntity<? extends Serializable> entity) {
        List<String> columns = new LinkedList<>();
        List<String> fields = new LinkedList<>();

        TableAnno table = tableAnno(entity.getClass());

        if (entity.getId() != null) {
            columns.add("`id`");
            fields.add("#{id}");
        } else if (table.primaryKey == Table.PRI_SEQUENCE_INSTANCE) {
            ((BaseEntity) entity).setId(SequenceRegistry.getSequence(context, table.table).apply(table.sequence));
            columns.add("`id`");
            fields.add("#{id}");
        }

        if (table.gmtCreate) {
            columns.add("`gmt_create`");
            fields.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (table.gmtModified) {
            columns.add("`gmt_modified`");
            fields.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
        }

        entityColumnMap(entity.getClass()).forEach((column, field) -> {
            columns.add(column);
            fields.add(field);
        });

        return new StringBuilder("INSERT INTO ")
                .append(table.table)
                .append("(")
                .append(Joiner.on(", ").join(columns))
                .append(") ")
                .append("VALUES(")
                .append(Joiner.on(", ").join(fields))
                .append(")")
                .toString();
    }

    public String update(ProviderContext context, BaseEntity<? extends Serializable> entity, Where where) {
        TableAnno table = tableAnno(entity.getClass());

        StringBuilder sb = new StringBuilder("UPDATE ").append(table.table);

        if (table.gmtModified) {
            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()"));
        } else {
            sb.append(" SET ");
        }
        // 注意: 此处不会修改id字段
        entityColumnMap(entity.getClass()).forEach((col, field) ->
                sb.append(", ").append(col).append(" = ").append(field)
        );

        return sb.append(getWhere(where)).toString();
    }

    public String updateByIds(ProviderContext context, BaseEntity<? extends Serializable> entity, Collection ids) {
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        TableAnno table = tableAnno(entity.getClass());

        StringBuilder sb = new StringBuilder("UPDATE ").append(table.table);

        if (table.gmtModified) {
            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()"));
        } else {
            sb.append(" SET ");
        }
        // 注意: 此处不会修改id字段
        entityColumnMap(entity.getClass()).forEach((col, field) ->
                sb.append(", ").append(col).append(" = ").append(field)
        );

        return sb
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }

    public String upsert(ProviderContext context, BaseEntity<?> entity) {
        List<String> columns = new LinkedList<>();
        List<String> fields = new LinkedList<>();
        List<String> updates = new LinkedList<>();

        TableAnno table = tableAnno(entity.getClass());
        if (table.gmtCreate) {
            columns.add("`gmt_create`");
            fields.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (table.gmtModified) {
            columns.add("`gmt_modified`");
            fields.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
            updates.add("`gmt_modified`");
        }

        if (entity.getId() != null) {
            columns.add("`id`");
            fields.add("#{id}");
            updates.add("`id`");
        }

        entityColumnMap(entity.getClass()).forEach((col, field) -> {
            columns.add(col);
            fields.add(field);
            updates.add(col);
        });

        return new StringBuilder("INSERT INTO ")
                .append(table.table)
                .append("(")
                .append(Joiner.on(", ").join(columns))
                .append(") ")
                .append("VALUES(")
                .append(Joiner.on(", ").join(fields))
                .append(")")
                .append(" ON DUPLICATE KEY UPDATE ")
                .append(updates.stream().map(n -> String.format("%s = VALUES(%s)", n, n)).collect(joining(", ")))
                .toString();
    }

//    public String updateById(ProviderContext context, BaseEntity<? extends Serializable> entity) {
//        TableAnno table = tableAnno(entity.getClass());
//
//        StringBuilder sb = new StringBuilder("UPDATE ").append(table.table);
//
//        if (table.gmtModified) {
//            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()"));
//        }
//
//        entityColumnMap(entity.getClass()).forEach((col, field) ->
//                sb.append(", ").append(col).append(" = ").append(field)
//        );
//
//        return sb.append(" WHERE `id` = #{id}").toString();
//    }

    public String delete(ProviderContext context, Where where) {
        return new StringBuilder("DELETE FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .toString();
    }

    public String deleteByIds(ProviderContext context, Collection<Object> ids) {
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        return new StringBuilder("DELETE FROM ")
                .append(tableAnno(context).table)
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }

    private String getWhere(Where where) {
        List<String> parts = new LinkedList<>();
        parts.addAll(getIs(where));
        parts.addAll(getLike(where));

        if (parts.isEmpty()) {
            return "";
        } else {
            return " WHERE " + Joiner.on(" AND ").join(parts);
        }
    }

    private List<String> getIs(Where where) {
        if (where.getIs().isEmpty()) {
            return Collections.emptyList();
        }

        return where
                .getIs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null || where.isUseNullValueAsIsNull())
                .map(entry -> {
                    if (entry.getValue() == null) {
                        return String.format("`%s` IS NULL", hump2line(entry.getKey()));
                    } else {
                        return String.format("`%s` = #{%s}", hump2line(entry.getKey()), entry.getKey());
                    }

                })
                .collect(Collectors.toList());
    }

    private List<String> getLike(Where where) {
        if (where.getLike().isEmpty()) {
            return Collections.emptyList();
        }

        return where
                .getLike()
                .entrySet()
                .stream()
                .filter(entry -> {
                    // entry.getValue() != null
                    if (entry.getValue() == null) {
                        throw new RuntimeException("LIKE key `" + entry.getKey() + "`'s value is null.");
                    }

                    return true;
                })
                .map(entry -> "`" + hump2line(entry.getKey()) + "` LIKE '%${" + entry.getKey() + "}%'")
                .collect(Collectors.toList());
    }

    private String getLimit(Where where) {
        Object offset = where.get("offset");
        Object limit = where.get("limit");

        if (offset == null && limit != null) {
            return " LIMIT #{limit}";
        }

        if (offset != null && limit != null) {
            return " LIMIT #{offset}, #{limit}";
        }

        if (offset == null) {
            return "";
        }

        throw new RuntimeException("un supported");
    }

    private String getOrderBy(Where where) {
        if (where.getOrderBy().isEmpty()) {
            return "";
        }

        return " ORDER BY" + where
                .getOrderBy()
                .entrySet()
                .stream()
                .map(entry -> String.format("`%s` %s", hump2line(entry.getKey()), entry.getValue() ? "ASC" : "DESC"))
                .collect(joining(", "));
    }
}
