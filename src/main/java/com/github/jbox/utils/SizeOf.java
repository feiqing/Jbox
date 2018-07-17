package com.github.jbox.utils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * 准确测量对象大小
 * Usage: append -javaagent:${jbox.jar file location} VM options
 * like as -javaagent:/Users/${user}/.m2/com.github.jbox/${jbox-version}/${jbox-version}.jar
 *
 * @author jifang@alibaba-inc.com
 * @since 2016/12/11 上午11:23.
 */
public class SizeOf {

    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        SizeOf.instrumentation = instrumentation;
    }

    public static long sizeOf(Object obj) {
        if (instrumentation == null) {
            throw new IllegalStateException("Can not access instrumentation environment.\n" +
                "Please check if jar file containing SizeOf class is \n" +
                "specified in the java's \"-javaagent\" command line argument.");
        }
        return instrumentation.getObjectSize(obj);
    }

    public static long fullSizeOf(Object obj) {
        Map<Object, Object> visitedSet = new IdentityHashMap<>();
        Queue<Object> queue = new LinkedList<>();
        long result = internalSizeOf(obj, queue, visitedSet);
        while (!queue.isEmpty()) {
            result += internalSizeOf(queue.poll(), queue, visitedSet);
        }

        visitedSet.clear();
        return result;
    }

    private static long internalSizeOf(Object obj, Queue<Object> queue, Map<Object, Object> visitedSet) {

        long result = 0;
        if (!isNeedSkip(obj, visitedSet)) {
            result += sizeOf(obj);
            visitedSet.put(obj, null);

            Class<?> clazz = obj.getClass();
            if (isNotPrimitiveArray(clazz)) {
                // add array item to search queue
                int length = Array.getLength(obj);
                for (int i = 0; i < length; ++i) {
                    queue.add(Array.get(obj, i));
                }
            } else {
                // add field & super class filed to search queue
                fieldsTraversal(obj, clazz, queue);
            }
        }

        return result;
    }

    // 向父类递归搜索, 将属性加入queue
    private static void fieldsTraversal(Object obj, Class<?> clazz, Queue<Object> queue) {
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // add search node list (not static & not primitive & not null)
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    try {
                        Object filedInstance = field.get(obj);
                        if (filedInstance != null) {
                            queue.add(filedInstance);
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static boolean isNeedSkip(Object obj, Map<Object, Object> visited) {
        // null、常量池对象、visited
        return obj == null || (obj instanceof String && obj == ((String)obj).intern()) || visited.containsKey(obj);
    }

    private static boolean isNotPrimitiveArray(Class<?> clazz) {
        return clazz.isArray() && clazz.getName().length() != 2;
    }
}
