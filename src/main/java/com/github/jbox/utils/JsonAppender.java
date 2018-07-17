package com.github.jbox.utils;

import java.util.Collections;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import com.google.common.base.Strings;

/**
 * @author jifang
 * @since 16/8/9 下午2:53.
 */
public class JsonAppender {

    private StringBuilder sb;

    public JsonAppender(String json) {
        if (Strings.isNullOrEmpty(json)) {
            this.sb = new StringBuilder();
        } else {
            this.sb = new StringBuilder(json);
        }
    }

    public JsonAppender append(String name, Object value) {
        if (sb.length() <= 0) {
            sb.append(JSON.toJSONString(Collections.singletonMap(name, value)));
        } else {
            String appendStr;
            if (value instanceof String) {
                appendStr = String.format(",\"%s\":\"%s\"", name, value);
            } else {
                appendStr = String.format(",\"%s\":%s", name, value);
            }

            sb.insert(sb.lastIndexOf("}"), appendStr);
        }
        return this;
    }

    public JsonAppender append(Map<String, Object> map) {
        if (sb.length() <= 0) {
            sb.append(JSON.toJSONString(map));
        } else {
            StringBuilder appendBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                appendBuilder
                    .append(",\"")
                    .append(entry.getKey())
                    .append("\":");

                if (entry.getValue() instanceof String) {
                    appendBuilder.append("\"").append(entry.getValue()).append("\"");
                } else {
                    appendBuilder.append(entry.getValue());
                }
            }

            sb.insert(sb.lastIndexOf("}"), appendBuilder);
        }
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
