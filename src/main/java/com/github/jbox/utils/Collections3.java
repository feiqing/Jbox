package com.github.jbox.utils;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-19 18:32:00.
 */
@SuppressWarnings("unchecked")
public class Collections3 {

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }

        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        }

        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }

        if (obj instanceof Object[]) {
            return ((Object[]) obj).length == 0;
        }

        return false;
    }

    public static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static <T> Set<T> nullToEmpty(Set<T> set) {
        return set == null ? Collections.emptySet() : set;
    }

    public static <K, V> Map<K, V> nullToEmpty(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    public static int size(Map map) {
        if (map == null) {
            return 0;
        }

        return map.size();
    }

    public static int size(Collection collection) {
        if (collection == null) {
            return 0;
        }

        return collection.size();
    }

    public static int size(Object[] array) {
        if (array == null) {
            return 0;
        }

        return array.length;
    }

    public static <T, U> T contains(List<T> list, U u, BiPredicate<T, U> matcher) {
        for (T t : nullToEmpty(list)) {
            if (matcher.test(t, u)) {
                return t;
            }
        }

        return null;
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static <T> void removeAll(Map<T, ?> map, Collection<T> keys) {
        for (T t : keys) {
            map.remove(t);
        }
    }

    public static <Input, Output> Output transfer(Input input, Function<Input, Output> function) {
        if (input == null) {
            return null;
        }

        return function.apply(input);
    }

    public static <Input, Output> List<Output> transfer(Collection<Input> inputs, Function<Input, Output> function) {
        return transfer(inputs, function, ArrayList::new);
    }

    public static <Input, Output, T extends Collection<Output>> T transfer(Collection<Input> inputs,
                                                                           Function<Input, Output> function,
                                                                           Supplier<T> supplier) {

        T outputs;
        if (inputs == null || inputs.isEmpty()) {
            outputs = (T) Collections.emptyList();
        } else {
            outputs = supplier.get();
            for (Input input : inputs) {
                outputs.add(function.apply(input));
            }
        }

        return outputs;
    }
}
