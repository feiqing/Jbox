package com.github.jbox.utils;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/3/30 5:10 PM.
 */
public class RpcUtil {

    private static final ConcurrentMap<Class, Map<String, Method>> cache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, AtomicReference<Object>> class2bean = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, AtomicReference<Object>> name2bean = new ConcurrentHashMap<>();

    public static Method getMethod(Class clazz, String methodName, Logger log) {
        Map<String, Method> methods = cache.get(clazz);

        if (methods == null) {
            Map<String, Method> methodMap = new ConcurrentHashMap<>();
            for (Method method : ReflectionUtils.getAllDeclaredMethods(clazz)) {
                methodMap.put(method.getName(), method);
            }

            cache.put(clazz, methodMap);
            methods = methodMap;
        }

        Method method = methods.get(methodName);
        if (method == null) {
            log.error("can't find method:[{}] from class:[{}]", methodName, clazz);
        }

        return method;
    }


    public static Object getBeanByClass(String clazz, ApplicationContext applicationContext, Logger log) throws ClassNotFoundException {
        AtomicReference<Object> optional = class2bean.get(clazz);
        if (optional != null) {
            return optional.get();
        }

        Object bean = doGetBeanByClass(clazz, applicationContext, log);
        class2bean.put(clazz, new AtomicReference<>(bean));

        return bean;
    }

    private static Object doGetBeanByClass(String className, ApplicationContext applicationContext, Logger log) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(className);
        try {
            Map<String, ?> beans = applicationContext.getBeansOfType(clazz);
            if (CollectionUtils.isEmpty(beans)) {
                return null;
            }

            if (beans.size() == 1) {
                return beans.values().iterator().next();
            }

            for (Object bean : beans.values()) {
                if (clazz.isInstance(bean)) {
                    return bean;
                }
            }
        } catch (Throwable t) {
            log.error("doGetBeanByClass error:[{}]", className, t);
        }

        return null;
    }

    public static Object getBeanByName(String clazz, ApplicationContext applicationContext, Logger log) {
        AtomicReference<Object> optional = name2bean.get(clazz);
        if (optional != null) {
            return optional.get();
        }

        Object bean = doGetBeanByName(clazz, applicationContext, log);
        name2bean.put(clazz, new AtomicReference<>(bean));

        return bean;
    }

    private static Object doGetBeanByName(String clazz, ApplicationContext applicationContext, Logger log) {
        String beanName = clazz.substring(clazz.lastIndexOf(".") + 1);
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        try {
            return applicationContext.getBean(beanName);
        } catch (Throwable t) {
            log.error("doGetBeanByName error:[{}]", beanName, t);
        }

        return null;
    }
}
