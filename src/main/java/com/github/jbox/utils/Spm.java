package com.github.jbox.utils;

import com.github.jbox.trace.tlog.LogBackHelper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/7 5:43 PM.
 */
public class Spm {

    private static Logger log;

    static {
        String homePath = System.getProperty("user.home");
        String appName = System.getProperty("app.name", "");
        if (Strings.isNullOrEmpty(appName)) {
            appName = System.getProperty("project.name", "");
        }
        String logPath = homePath + "/" + appName + "/logs/spm.log";

        String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{35} %X{traceId} - %msg%n";

        LoggerFactory.getLogger(Spm.class).warn("spm log path:[{}], pattern:[{}]", logPath, pattern);


        log = LogBackHelper.initTLogger("Spm", logPath, "UTF-8", pattern, 1, 0, null);
    }

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

    public static void log(String type, Object slotKey, Boolean isSuccess, Long cost, Object... params) {
        log(type2type.computeIfAbsent(type, _K -> () -> type), slotKey, isSuccess, cost, params);
    }

    private static final ConcurrentMap<String, Type> type2type = new ConcurrentHashMap<>();

    public interface Type {
        String name();
    }
}
