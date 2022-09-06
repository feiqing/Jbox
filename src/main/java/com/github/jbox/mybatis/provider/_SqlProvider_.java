package com.github.jbox.mybatis.provider;

import com.github.jbox.mybatis.BaseEntity;
import com.github.jbox.mybatis.Table;
import com.github.jbox.mybatis.Where;
import com.github.jbox.mybatis.sequence.SequenceRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.RuntimeSqlException;

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

    /* select */

    public static String select(ProviderContext context, Where where) {
        Preconditions.checkArgument(where != null);

        return new StringBuilder("SELECT * FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .append(getOrderBy(where))
                .append(getLimit(where))
                .toString();
    }

    public static String selectByIds(ProviderContext context, Collection<Object> ids) {
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        return new StringBuilder("SELECT * FROM ")
                .append(tableAnno(context).table)
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }

    /* count */

    public static String count(ProviderContext context, Where where) {
        Preconditions.checkArgument(where != null);

        return new StringBuilder("SELECT COUNT(*) FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .toString();
    }

    /* insert、upsert */

    public static String insert(ProviderContext context, BaseEntity<? extends Serializable> entity) {
        Preconditions.checkArgument(entity != null);

        List<String> columns = new LinkedList<>();
        List<String> fields = new LinkedList<>();

        TableAnno table = tableAnno(entity.getClass());

        if (entity.getId() != null) {
            columns.add("`id`");
            fields.add("#{id}");
        } else if (table.primaryKey == Table.PRI_SEQUENCE_INSTANCE) {
            ((BaseEntity) entity).setId(SequenceRegistry.getSequence(context, table.table, table.sequenceId).apply(table.sequenceId));
            columns.add("`id`");
            fields.add("#{id}");
        }

        if (table.useGmtCreate) {
            columns.add("`gmt_create`");
            fields.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (table.useGmtModified) {
            columns.add("`gmt_modified`");
            fields.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
        }

        entityColumnMap(entity.getClass()).forEach((column, field) -> {
            columns.add("`" + column + "`");
            fields.add("#{" + field + "}");
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

    public static String upsert(ProviderContext context, BaseEntity<? extends Serializable> entity) {
        Preconditions.checkArgument(entity != null);

        List<String> columns = new LinkedList<>();
        List<String> fields = new LinkedList<>();
        List<String> updates = new LinkedList<>();

        TableAnno table = tableAnno(entity.getClass());
        if (table.useGmtCreate) {
            columns.add("`gmt_create`");
            fields.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (table.useGmtModified) {
            columns.add("`gmt_modified`");
            fields.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
            updates.add("`gmt_modified`");
        }

        if (entity.getId() != null) {
            columns.add("`id`");
            fields.add("#{id}");
            updates.add("`id`");
        }

        entityColumnMap(entity.getClass()).forEach((column, field) -> {
            columns.add("`" + column + "`");
            fields.add("#{" + field + "}");
            updates.add("`" + column + "`");
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

    /* update */

    public static String update(ProviderContext context, BaseEntity<? extends Serializable> entity, Where where) {
        Preconditions.checkArgument(entity != null);
        Preconditions.checkArgument(where != null);

        TableAnno table = tableAnno(entity.getClass());

        StringBuilder sb = new StringBuilder("UPDATE ").append(table.table);

        if (table.useGmtModified) {
            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "#{entity.gmtModified}" : "NOW()"));
            entityColumnMap(entity.getClass()).forEach((column, field) -> {
                sb.append(", ").append("`" + column + "`").append(" = ").append("#{entity." + field + "}");
            });
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : entityColumnMap(entity.getClass()).entrySet()) {
                if (first) {
                    sb.append(" SET ");
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("`" + entry.getKey() + "`").append(" = ").append("#{entity." + entry.getValue() + "}");
            }
        }

        return sb.append(getWhere(where, "where")).toString();
    }

    public static String updateByIds(ProviderContext context, BaseEntity<? extends Serializable> entity, Collection<Object> ids) {
        Preconditions.checkArgument(entity != null);
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        TableAnno table = tableAnno(entity.getClass());

        StringBuilder sb = new StringBuilder("UPDATE ").append(table.table);

        if (table.useGmtModified) {
            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "#{entity.gmtModified}" : "NOW()"));
            entityColumnMap(entity.getClass()).forEach((column, field) -> {
                sb.append(", ").append("`" + column + "`").append(" = ").append("#{entity." + field + "}");
            });
        } else {
            boolean first = true;
            for (Map.Entry<String, String> entry : entityColumnMap(entity.getClass()).entrySet()) {
                if (first) {
                    sb.append(" SET ");
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("`" + entry.getKey() + "`").append(" = ").append("#{entity." + entry.getValue() + "}");
            }
        }

        return sb
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }



    /* delete */

    public static String delete(ProviderContext context, Where where) {
        Preconditions.checkArgument(where != null);
        
        return new StringBuilder("DELETE FROM ")
                .append(tableAnno(context).table)
                .append(getWhere(where))
                .toString();
    }

    public static String deleteByIds(ProviderContext context, Collection<Object> ids) {
        Preconditions.checkArgument(ids != null && !ids.isEmpty());

        return new StringBuilder("DELETE FROM ")
                .append(tableAnno(context).table)
                .append(" WHERE `id` IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(joining(", ")) + " )")
                .toString();
    }

    /* helper */

    private static String getWhere(Where where) {
        return getWhere(where, null);
    }

    private static String getWhere(Where where, String prefix) {
        List<String> parts = new LinkedList<>();
        parts.addAll(getIs(where, prefix));
        parts.addAll(getLike(where, prefix));

        if (parts.isEmpty()) {
            return "";
        } else {
            return " WHERE " + Joiner.on(" AND ").join(parts);
        }
    }

    private static List<String> getIs(Where where, String prefix) {
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
                    } else if (prefix != null) {
                        return String.format("`%s` = #{%s.%s}", hump2line(entry.getKey()), prefix, entry.getKey());
                    } else {
                        return String.format("`%s` = #{%s}", hump2line(entry.getKey()), entry.getKey());
                    }

                })
                .collect(Collectors.toList());
    }

    private static List<String> getLike(Where where, String prefix) {
        if (where.getLike().isEmpty()) {
            return Collections.emptyList();
        }

        return where
                .getLike()
                .entrySet()
                .stream()
                .filter(entry -> {
                    if (entry.getValue() == null) {
                        throw new RuntimeSqlException("LIKE key `" + entry.getKey() + "`' value is null.");
                    }

                    return true;
                })
                .map(entry -> {
                    if (prefix == null) {
                        return "`" + hump2line(entry.getKey()) + "` LIKE '%${" + entry.getKey() + "}%'";
                    } else {
                        return "`" + hump2line(entry.getKey()) + "` LIKE '%${" + prefix + "." + entry.getKey() + "}%'";
                    }
                })
                .collect(Collectors.toList());
    }

    private static String getLimit(Where where) {
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

        throw new RuntimeSqlException("Unexpected");
    }

    private static String getOrderBy(Where where) {
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
