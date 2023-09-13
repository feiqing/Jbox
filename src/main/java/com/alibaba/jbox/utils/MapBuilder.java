package com.alibaba.jbox.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-04-05 20:06:00.
 */
public class MapBuilder<K, V> {

    private Map<K, V> map;

    private MapBuilder(Map<K, V> map) {
        this.map = map;
    }

    public static <K, V> MapBuilder<K, V> newBuilder() {
        return new MapBuilder<>(new HashMap<>());
    }

    public static <K, V> MapBuilder<K, V> newBuilder(Map<K, V> map) {
        return new MapBuilder<>(map);
    }

    public MapBuilder<K, V> append(K key, V value) {
        this.map.put(key, value);
        return this;
    }

    @SuppressWarnings("all")
    public <Key, Value> Map<Key, Value> build() {
        return (Map<Key, Value>) map;
    }
}
