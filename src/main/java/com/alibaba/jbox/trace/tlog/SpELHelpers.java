package com.alibaba.jbox.trace.tlog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/9/23 08:03:00.
 */
public class SpELHelpers {

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private static List<Method> CUSTOM_METHODS = new CopyOnWriteArrayList<>();

    static {
        registerFunctions(SpELHelpers.class, "registerFunction", "registerFunctions");
    }

    /* -- 开放给用户用于导入helper工具方法, 以降低在XML/Groovy/Json配置文件书写的SpEL表达式复杂度 -- */

    public static void registerFunctions(Class<?> clazz) {
        registerFunctions(clazz, new String[0]);
    }

    public static void registerFunctions(Class<?> clazz, String... excludeMethods) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                && Modifier.isPublic(method.getModifiers())
                && !isExclude(method.getName(), excludeMethods)) {
                registerFunction(method);
            }
        }
    }

    public static void registerFunction(Method staticMethod) {
        if (!Modifier.isStatic(staticMethod.getModifiers())
            || !Modifier.isPublic(staticMethod.getModifiers())) {

            throw new IllegalArgumentException(
                String.format("method [%s] is not public static, SpEL is not support.", staticMethod)
            );
        }

        CUSTOM_METHODS.add(staticMethod);
    }

    private static boolean isExclude(String method, String... excludeMethods) {
        if (excludeMethods == null || excludeMethods.length == 0) {
            return false;
        }

        for (String excludeMethod : excludeMethods) {
            if (excludeMethod.equals(method)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 将args、result、placeholder导入环境, 计算spel表达式
     *
     * @param event
     * @param spels
     * @param placeHolder
     * @return
     */
    static List<Object> evalSpelWithEvent(LogEvent event, List<String> spels, String placeHolder) {
        List<Object> values;
        if (spels != null && !spels.isEmpty()) {
            // 将'args'、'result'、'placeholder'、'ph'导入spel执行环境
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable(TlogConstants.KEY_ARGS, event.getArgs());
            context.setVariable(TlogConstants.KEY_RESULT, event.getResult());
            context.setVariable(TlogConstants.KEY_PLACEHOLDER, placeHolder);
            context.setVariable(TlogConstants.KEY_PH, placeHolder);
            // 将自定义函数导入spel执行环境
            prepareFunctions(context);

            values = new ArrayList<>(spels.size());
            for (String spel : spels) {
                Object evalResult = SPEL_PARSER.parseExpression(spel).getValue(context);
                values.add(evalResult);
            }
        } else {
            values = Collections.emptyList();
        }
        return values;
    }

    /**
     * 不将args、result导入环境, 直接针对object计算spel表达式
     *
     * @param obj
     * @param spels
     * @return
     */
    static List<Object> evalSpelWithObject(Object obj, List<String> spels, String placeHolder) {
        List<Object> values;
        if (spels != null && !spels.isEmpty()) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            // 仅将'placeholder'、'ph'导入spel执行环境
            context.setVariable(TlogConstants.KEY_PLACEHOLDER, placeHolder);
            context.setVariable(TlogConstants.KEY_PH, placeHolder);
            // 将自定义函数导入spel执行环境
            prepareFunctions(context);

            values = new ArrayList<>(spels.size());
            for (String spel : spels) {
                Object result = SPEL_PARSER.parseExpression(spel).getValue(context, obj);
                values.add(result);
            }
        } else {
            values = Collections.emptyList();
        }

        return values;
    }

    private static void prepareFunctions(StandardEvaluationContext context) {
        for (Method method : CUSTOM_METHODS) {
            context.registerFunction(method.getName(), method);
        }
    }

    /** ****************************************** **/
    /**  默认注册到SpEL环境中的工具函数(尽量减少额外依赖)  **/
    /************************************************/
    // -> start
    public static <T> Collection<T> isNotEmpty(Collection<T> collection) {
        if (collection != null && !collection.isEmpty()) {
            return collection;
        }
        return null;
    }

    public static <T> T ifNotEmptyGet(List<T> list, int index) {
        list = (List<T>)isNotEmpty(list);
        return list == null ? null : list.get(index);
    }

    public static <T> T ifEmptyGetDefault(List<T> list, int index, T defaultObj) {
        T obj = ifNotEmptyGet(list, index);
        return obj == null ? defaultObj : obj;
    }

    /**
     * 获取target内的property属性值, 以弥补SpEL表达式不支持获取父类属性值得缺陷
     * 如果property属性是存在target的父类中的话, 'target?.property'会抛出异常
     * 可以将其替换为 #getProperty(target, 'property')
     *
     * @param target
     * @param property
     * @return
     */
    public static Object getProperty(Object target, String property) {
        return getPropertyOrDefault(target, property, null);
    }

    public static Object getPropertyOrDefault(Object target, String property, Object defaultValue) {
        return Optional.ofNullable(target)
            .map(t -> doGetProperty(target, property, defaultValue))
            .orElse(defaultValue);
    }

    private static Object doGetProperty(Object target, String property, Object defaultValue) {

        Field field = findField(target.getClass(), property, null);
        if (field == null) {
            return defaultValue;
        }

        makeAccessible(field);
        Object value = getField(field, target);
        return value == null ? defaultValue : value;
    }
    // <- end

    /* -------- copy from spring public to private 降低对其他模块的依赖 ---------- */

    /**
     * Attempt to find a {@link Field field} on the supplied {@link Class} with the
     * supplied {@code name} and/or {@link Class type}. Searches all superclasses
     * up to {@link Object}.
     *
     * @param clazz the class to introspect
     * @param name  the name of the field (may be {@code null} if type is specified)
     * @param type  the type of the field (may be {@code null} if name is specified)
     * @return the corresponding Field object, or {@code null} if not found
     */
    private static Field findField(Class<?> clazz, String name, Class<?> type) {
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName())) &&
                    (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * Make the given field accessible, explicitly setting it accessible if
     * necessary. The {@code setAccessible(true)} method is only called
     * when actually necessary, to avoid unnecessary conflicts with a JVM
     * SecurityManager (if active).
     *
     * @param field the field to make accessible
     * @see java.lang.reflect.Field#setAccessible
     */
    private static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
            !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
            Modifier.isFinal(field.getModifiers()))
            && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /**
     * Get the field represented by the supplied {@link Field field object} on the
     * specified {@link Object target object}. In accordance with {@link Field#get(Object)}
     * semantics, the returned value is automatically wrapped if the underlying field
     * has a primitive type.
     *
     * @param field  the field to get
     * @param target the target object from which to get the field
     * @return the field's current value
     */
    private static Object getField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(
                "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
}
