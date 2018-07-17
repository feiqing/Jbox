package com.github.jbox.executor;

import lombok.Data;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/10/10 11:36:00.
 */
@Data
public class AsyncContext {

    private Thread parent;

    private String group;

    private Map<String, String> mdcContext;

    private Map<String, Object> extContext;

    public static AsyncContext newContext(String group) {
        AsyncContext context = new AsyncContext();
        context.setGroup(group);
        context.setParent(Thread.currentThread());
        context.setMdcContext(MDC.getCopyOfContextMap());

        return context;
    }

    public void putExtContext(String key, Object value) {
        if (extContext == null) {
            extContext = new HashMap<>();
        }
        extContext.put(key, value);
    }

    public Map<String, Object> getExtContext() {
        return extContext == null ? extContext = new HashMap<>() : extContext;
    }

    public Map<String, String> getMdcContext() {
        return mdcContext == null ? mdcContext = new HashMap<>() : mdcContext;
    }
}
