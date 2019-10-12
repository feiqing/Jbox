package com.github.jbox.utils;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/7 5:43 PM.
 */
@Slf4j(topic = "Spm")
public class Spm {

    /**
     * 前五位固定: localIp | type | slotKey(如storeId, userId等) | is_success | cost |
     * 后面可以自由发挥
     *
     * @param type
     * @param slotKey
     * @param isSuccess
     * @param cost
     * @param params
     */
    public static void log(Type type, Object slotKey, Boolean isSuccess, Long cost, Object... params) {
        List<Object> entities = new LinkedList<>();
        entities.add(IPv4.getLocalIp());
        entities.add(type.name());
        entities.add(slotKey);
        entities.add(isSuccess);
        entities.add(cost);
        entities.addAll(Arrays.asList(params));
        log.info("|{}|", Joiner.on('|').useForNull("-").join(entities));
    }

    public interface Type {
        String name();
    }
}
