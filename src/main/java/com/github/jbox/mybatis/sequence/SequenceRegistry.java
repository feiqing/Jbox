package com.github.jbox.mybatis.sequence;

import org.apache.ibatis.builder.annotation.ProviderContext;

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

    public static final String DEFAULT_TABLE = "_DEFUALT_TABLE_";

    private static final ConcurrentMap<String, Function<String, ? extends Serializable>> sequenceManager = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, Optional<Function<String, ? extends Serializable>>> sequenceCache = new ConcurrentHashMap<>();

    // todo 后续可以把注册的name细化
    public static void registerDefault(Function<String, ? extends Serializable> sequence) {
        register(DEFAULT_TABLE, sequence);
    }

    public static void register(String table, Function<String, ? extends Serializable> sequence) {
        sequenceManager.put(table, sequence);
    }


    public static Function<String, ? extends Serializable> getSequence(ProviderContext context, String table) {
        Optional<Function<String, ? extends Serializable>> optional = sequenceCache.computeIfAbsent(context.getMapperType(), _k -> Optional.ofNullable(selectSequence(context, table)));
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new IllegalStateException("could not found sequence for table:[" + table + "]");
        }
    }

    private static Function<String, ? extends Serializable> selectSequence(ProviderContext context, String table) {
        Function<String, ? extends Serializable> sequence = sequenceManager.get(context.getMapperType().getName());
        if (sequence == null) {
            sequence = sequenceManager.get(context.getMapperMethod().getName());
        }
        if (sequence == null) {
            sequence = sequenceManager.get(table);
        }
        if (sequence == null) {
            sequence = sequenceManager.get(DEFAULT_TABLE);
        }

        return sequence;
    }
}
