package com.github.jbox.trace.tlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.github.jbox.trace.TraceException;
import com.github.jbox.trace.tlog.AbstractTLogConfig.ELConfig;
import com.github.jbox.utils.JboxUtils;

import com.google.common.base.Splitter;

import static com.github.jbox.trace.tlog.ELResolveHelpers.replaceRefToRealString;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/18 18:19:00.
 */
class ELJsonResolver {

    static void resolve(String json, Map<String, List<ELConfig>> methodELMap) {
        Map<String, Object> jsonObject = JSONObject.parseObject(json, Feature.AutoCloseSource);
        // 先将用户自定义def解析出来
        Map<String, String> defs = parseUserDefs(jsonObject);

        // 支持config乱序引用
        Map<String, String> refs = new HashMap<>();
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            // 替换为最终key
            String methodKey = replaceRefToRealString(entry.getKey(), defs);

            List<ELConfig> spels = methodELMap.computeIfAbsent(methodKey, k -> new ArrayList<>());
            Object value = entry.getValue();
            if (value instanceof String) {                                  // 引用的其他配置
                String refConfig = parseRefToConfig((String)value, defs);
                refs.put(methodKey, refConfig);
            } else if (value instanceof JSONArray) {                        // 新创建的配置
                List<ELConfig> newConfigs = ELConfig.parseEntriesFromJsonArray((JSONArray)value);
                spels.addAll(newConfigs);

                // @since 1.2支持methodName缩写引用
                String methodName = Splitter.on(":").splitToList(methodKey).get(1);
                methodELMap.put(methodName, spels);
            } else {                                                        // 错误的配置语法
                throw new TraceException(
                    "your config '" + value
                        + "' is not relative defined config or not a new config. please check your config syntax is "
                        + "correct or mail to jifang.zjf@alibaba-inc.com.");
            }
        }

        // 将引用的配置注册进去
        for (Map.Entry<String, String> entry : refs.entrySet()) {
            List<ELConfig> definedConfig = methodELMap.computeIfAbsent(entry.getValue(),
                (key) -> {
                    throw new TraceException("relative config '" + key + "' is not defined.");
                });

            methodELMap.computeIfAbsent(entry.getKey(), key -> new LinkedList<>()).addAll(definedConfig);
        }
    }

    private static Map<String, String> parseUserDefs(Map<String, Object> jsonObject) {
        Map<String, String> defs = new HashMap<>();
        List<String> needRemoveKeys = new LinkedList<>();
        for (Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();

            if (!key.startsWith(TLogConstants.USER_DEF) || !(entry.getValue() instanceof String)) {
                continue;
            }

            String defKey = key.substring(TLogConstants.USER_DEF.length()).trim();
            defKey = JboxUtils.trimPrefixAndSuffix(defKey, TLogConstants.DEF_PREFIX, TLogConstants.DEF_SUFFIX);
            defs.put(defKey, (String)entry.getValue());

            needRemoveKeys.add(key);
        }

        for (String key : needRemoveKeys) {
            jsonObject.remove(key);
        }

        return defs;
    }

    /**
     * 将ref引用还原
     *
     * @param original
     * @param defs
     * @return
     */
    private static String parseRefToConfig(String original, Map<String, String> defs) {
        // 将original中包含的ref-def字段替换为实际值
        String originalValue = replaceRefToRealString(original, defs);
        if (originalValue.startsWith(TLogConstants.REF_PREFIX) && originalValue.endsWith(TLogConstants.REF_SUFFIX)) {
            String refConfig = JboxUtils.trimPrefixAndSuffix(originalValue, TLogConstants.DEF_PREFIX, TLogConstants.DEF_SUFFIX, false);
            return refConfig;
        } else {
            throw new TraceException(
                "your config '" + original
                    + "' is not relative defined config or not a new config. please check your config syntax is "
                    + "correct or mail to jifang.zjf@alibaba-inc.com.");
        }
    }
}
