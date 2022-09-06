package com.github.jbox.mybatis.sequence;

import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.RuntimeSqlException;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2022/8/16 13:25.
 */
public class SequenceRegistry {

    private static final String DEFAULT_SEQUENCE = "_DEFAULT_SEQUENCE_ID_";

    private static final ConcurrentMap<String, Function<String, ? extends Serializable>> manager = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, Optional<Function<String, ? extends Serializable>>> cache = new ConcurrentHashMap<>();

    public static void registerDefault(Function<String, ? extends Serializable> sequence) {
        register(DEFAULT_SEQUENCE, sequence);
    }

    public static void register(String name, Function<String, ? extends Serializable> sequence) {
        manager.put(name, sequence);
    }

    public static Function<String, ? extends Serializable> getSequence(ProviderContext context, String table, String sequenceId) {
        Optional<Function<String, ? extends Serializable>> optional = cache.computeIfAbsent(context.getMapperType(), _k -> Optional.ofNullable(selectSequence(context, table, sequenceId)));
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new RuntimeSqlException("could not found sequence for table:[" + table + "]");
        }
    }

    private static Function<String, ? extends Serializable> selectSequence(ProviderContext context, String table, String sequenceId) {
        Function<String, ? extends Serializable> sequence = manager.get(context.getMapperType().getName());
        if (sequence == null) {
            sequence = manager.get(context.getMapperMethod().getName());
        }
        if (sequence == null) {
            sequence = manager.get(table);
        }
        if (sequence == null) {
            sequence = manager.get(sequenceId);
        }
        if (sequence == null) {
            sequence = manager.get(DEFAULT_SEQUENCE);
        }

        return sequence;
    }
}
