package com.github.jbox.mybatis;

import com.github.jbox.mybatis.spring.SqlSessionFactoryBean;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.getAllFields;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2021/3/12 10:12 上午.
 */
@SuppressWarnings("all")
public class SqlProvider {

    public String find(ProviderContext context, Where where) {
        return new StringBuilder("SELECT * FROM ")
                .append(getTable(context))
                .append(getWhere(where))
                .append(getOrderBy(where))
                .append(getLimit(where))
                .toString();
    }

    public String findOne(ProviderContext context, Where where) {
        if (!where.getLike().isEmpty()) {
            throw new RuntimeException("findOne() not support 'LIKE'");
        }

        return new StringBuilder("SELECT * FROM ")
                .append(getTable(context))
                .append(getWhere(where))
                .append(getOrderBy(where))
                .append(getLimit(where))
                .toString();
    }

    public String findByIds(ProviderContext context, Collection<Object> ids) {
        String where = "";
        if (ids != null && !ids.isEmpty()) {
            where = " WHERE id IN( " + ids.stream().filter(Objects::nonNull).map(id -> "'" + id + "'").collect(Collectors.joining(", ")) + " )";
        }

        return new StringBuilder("SELECT * FROM ")
                .append(getTable(context))
                .append(where)
                .toString();
    }

    public String count(ProviderContext context, Where where) {
        return new StringBuilder("SELECT COUNT(*) FROM ")
                .append(getTable(context))
                .append(getWhere(where))
                .toString();
    }

    public String insert(ProviderContext context, BaseEntity<? extends Serializable> entity) {
        List<String> colNames = new LinkedList<>();
        List<String> colVals = new LinkedList<>();

        // todo: 在这里基于@Table注解判断是否添加`id`字段, 添加的同时通过生成sequence数值
        if (entity.getId() != null) {
            colNames.add("`id`");
            colVals.add("#{id}");
        }

        Table tableAnno = getTableAnno(entity);
        SqlSessionFactoryBean.sequence(tableAnno.sequence());

        if (tableAnno != null && tableAnno.sequence() != "__AUTO_INCREMENT__") {
            colNames.add("`id`");
            colVals.add("#{id}");
            // todo 设计一个SequenceHolder
            // entity.setId();
        }


        if (tableAnno == null || tableAnno.gmtCreate()) {
            colNames.add("`gmt_create`");
            colVals.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (tableAnno == null || tableAnno.gmtModified()) {
            colNames.add("`gmt_modified`");
            colVals.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
        }

        col2fields(entity.getClass()).forEach((col, field) -> {
            colNames.add(col);
            colVals.add(field);
        });

        return new StringBuilder("INSERT INTO ")
                .append(getTable(context))
                .append("(")
                .append(Joiner.on(", ").join(colNames))
                .append(") ")
                .append("VALUES(")
                .append(Joiner.on(", ").join(colVals))
                .append(")")
                .toString();
    }

    private static final ConcurrentMap<Class<?>, Optional<Table>> class2table = new ConcurrentHashMap<>();

    private static Table getTableAnno(BaseEntity<?> entity) {
        return class2table.computeIfAbsent(entity.getClass(), clazz -> Optional.ofNullable(clazz.getAnnotation(Table.class))).orElse(null);
    }


    public String upsert(ProviderContext context, BaseEntity<?> entity) {
        List<String> colNames = new LinkedList<>();
        List<String> namTails = new LinkedList<>();
        List<String> colVals = new LinkedList<>();

        final Table tableAnno = getTableAnno(entity);
        if (tableAnno == null || tableAnno.gmtCreate()) {
            colNames.add("`gmt_create`");
            colVals.add(entity.getGmtCreate() != null ? "#{gmtCreate}" : "NOW()");
        }

        if (tableAnno == null || tableAnno.gmtModified()) {
            colNames.add("`gmt_modified`");
            namTails.add("`gmt_modified`");
            colVals.add(entity.getGmtModified() != null ? "#{gmtModified}" : "NOW()");
        }

        if (entity.getId() != null) {
            colNames.add("`id`");
            namTails.add("`id`");
            colVals.add("#{id}");
        }

        col2fields(entity.getClass()).forEach((col, field) -> {
            colNames.add(col);
            namTails.add(col);
            colVals.add(field);
        });

        return new StringBuilder("INSERT INTO ")
                .append(getTable(context))
                .append("(")
                .append(Joiner.on(", ").join(colNames))
                .append(") ")
                .append("VALUES(")
                .append(Joiner.on(", ").join(colVals))
                .append(")")
                .append(" ON DUPLICATE KEY UPDATE ")
                .append(namTails.stream().map(n -> String.format("%s = VALUES(%s)", n, n)).collect(Collectors.joining(", ")))
                .toString();
    }

    public String updateById(ProviderContext context, BaseEntity<?> entity) {
        StringBuilder sb = new StringBuilder("UPDATE ")
                .append(getTable(context));

        Table tableAnno = getTableAnno(entity);
        if (tableAnno == null || tableAnno.gmtModified()) {
            sb.append(String.format(" SET gmt_modified = %s", entity.getGmtModified() != null ? "${gmtModified}" : "NOW()"));
        }

        col2fields(entity.getClass()).forEach((col, field) ->
                sb.append(", ").append(col).append(" = ").append(field)
        );

        return sb.append(" WHERE `id` = #{id}").toString();
    }

    public String delete(ProviderContext context, Where where) {
        return new StringBuilder("DELETE FROM ")
                .append(getTable(context))
                .append(getWhere(where))
                .toString();
    }

    public static String getWhere(Where where) {
        List<String> parts = new ArrayList<>();
        parts.addAll(getIs(where));
        parts.addAll(getLike(where));

        if (parts.isEmpty()) {
            return "";
        } else {
            return " WHERE " + Joiner.on(" AND ").join(parts);
        }
    }

    public static List<String> getIs(Where where) {
        if (where.getIs().isEmpty()) {
            return Collections.emptyList();
        }

        return where
                .getIs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> {
                    // TODO: 后续可以给出配置项过滤掉该查询项
                    if (entry.getValue() == null) {
                        return String.format("`%s` IS NULL", hump2line(entry.getKey()));
                    } else {
                        return String.format("`%s` = #{%s}", hump2line(entry.getKey()), entry.getKey());
                    }

                })
                .collect(Collectors.toList());
    }

    public static List<String> getLike(Where where) {
        if (where.getLike().isEmpty()) {
            return Collections.emptyList();
        }

        return where
                .getLike()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> "`" + hump2line(entry.getKey()) + "` LIKE '%${" + entry.getKey() + "}%'")
                .collect(Collectors.toList());
    }

    public static String getLimit(Where where) {
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

    public static String getOrderBy(Where where) {
        if (where.getOrderBy().isEmpty()) {
            return "";
        }

        return " ORDER BY" + Joiner.on(", ").join(where
                .getOrderBy()
                .entrySet()
                .stream()
                .map(entry -> String.format("`%s` %s", hump2line(entry.getKey()), entry.getValue() ? "ASC" : "DESC"))
                .collect(Collectors.toList())
        );
    }

    private static final ConcurrentMap<Class<?>, Map<String, String>> col2field = new ConcurrentHashMap<>();

    private Map<String, String> col2fields(Class<?> clazz) {
        return col2field
                .computeIfAbsent(clazz, _K -> {
                            Map<String, String> map = new LinkedHashMap<>();
                            getAllFields(clazz, field -> {
                                if (field == null) {
                                    return false;
                                }

                                if (Modifier.isStatic(field.getModifiers())) {
                                    return false;
                                }

                                if (Modifier.isFinal(field.getModifiers())) {
                                    return false;
                                }

                                return !Arrays.asList("id", "gmtCreate", "gmtModified", "_use_gmt_create", "_use_gmt_modified").contains(field.getName());
                            }).forEach(field -> {
                                Column column = field.getAnnotation(Column.class);
                                String name = field.getName();
                                String col = column != null ? column.value() : hump2line(name);
                                map.put("`" + col + "`", "#{" + name + "}");
                            });

                            return map;
                        }
                );
    }

    private static final ConcurrentMap<String, String> hum2line = new ConcurrentHashMap<>();

    public static String hump2line(String hump) {
        return hum2line.computeIfAbsent(hump, _K -> hump.replaceAll("[A-Z]", "_$0").toLowerCase());
    }

    private static final ConcurrentMap<Class<?>, String> mapper2table = new ConcurrentHashMap<>();

    private static String getTable(ProviderContext context) {
        return mapper2table.computeIfAbsent(context.getMapperType(), _K -> {
            Class<?> entityType = (Class<?>) ((ParameterizedType) (context.getMapperType().getGenericInterfaces()[0])).getActualTypeArguments()[0];
            Table table = entityType.getAnnotation(Table.class);
            if (table != null) {
                if (Strings.isNullOrEmpty(table.name()))
                    throw new RuntimeException("no support");
                return table.name();
            }

            String name = entityType.getSimpleName();

            int entity = StringUtils.indexOfIgnoreCase(name, "ENTITY");
            if (entity != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, entity));

            int po = StringUtils.indexOfIgnoreCase(name, "PO");
            if (po != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, po));

            int _do = StringUtils.indexOfIgnoreCase(name, "DO");
            if (_do != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, _do));

            int dto = StringUtils.indexOfIgnoreCase(name, "DTO");
            if (dto != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, dto));

            int model = StringUtils.indexOfIgnoreCase(name, "MODEL");
            if (model != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, model));

            int pojo = StringUtils.indexOfIgnoreCase(name, "POJO");
            if (pojo != -1)
                return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1, pojo));

            return hump2line(String.valueOf(name.charAt(0)).toLowerCase() + StringUtils.substring(name, 1));
        });
    }
}
