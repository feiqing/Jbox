package com.github.jbox.utils;

import org.springframework.util.ClassUtils;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/8/22 16:21:00.
 */
public class ProxyTargetUtils {

    public static Object getProxyTarget(Object proxy) {
        if (proxy == null) {
            return null;
        }

        Class<?> clazz = proxy.getClass();
        if (java.lang.reflect.Proxy.isProxyClass(clazz)) {
            return getJdkProxyTarget(proxy);
        } else if (net.sf.cglib.proxy.Proxy.isProxyClass(clazz)
            || ClassUtils.isCglibProxyClass(clazz)) {
            return getCglibProxyTarget(proxy);
        } else {
            return proxy;
        }
    }

    /**
     * 此处只针对{@link java.lang.reflect.InvocationHandler}实现中包含target属性的情况
     */
    private static final String JDK_CALLBACK = "h";

    private static final String JDK_TARGET = "target";

    public static Object getJdkProxyTarget(Object proxy) {
        return JboxUtils.getFieldValue(proxy, JDK_CALLBACK, JDK_TARGET);
    }

    /**
     * 此处只针对{@link net.sf.cglib.proxy.Callback}、{@link net.sf.cglib.proxy.InvocationHandler}实现中包含target属性的情况
     */
    private static final String CGLIB_CALLBACK = "CGLIB$CALLBACK_0";

    private static final String CGLIB_TARGET = "target";

    public static Object getCglibProxyTarget(Object proxy) {
        return JboxUtils.getFieldValue(proxy, CGLIB_CALLBACK, CGLIB_TARGET);
    }
}
