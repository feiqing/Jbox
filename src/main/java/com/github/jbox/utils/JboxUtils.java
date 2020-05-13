package com.github.jbox.utils;

import com.alibaba.fastjson.JSON;
import com.github.jbox.helpers.ExceptionableSupplier;
import com.github.jbox.helpers.ThrowableSupplier;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Jbox内部调用Util方法
 *
 * @author jifang@alibaba-inc.com
 * @since 16/8/18 下午6:09.
 */
public class JboxUtils {

    /* ------- #1 --------- reflection get field value -------------------- */

    public static Object getFieldValue(Object target, String fieldName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fieldName), "field name can not be empty: %s", fieldName);

        if (target == null) {
            return null;
        }

        Field field = ReflectionUtils.findField(target.getClass(), fieldName);

        if (field == null) {
            return null;
        }

        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }

    public static Object getFieldValue(Object target, String outerFieldName, String... innerFieldNames) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(outerFieldName),
                "outer field name can not be empty: %s",
                outerFieldName);

        if (target == null) {
            return null;
        }

        Object outerTarget = getFieldValue(target, outerFieldName);

        Object innerObject = null;
        if (innerFieldNames != null && innerFieldNames.length != 0) {
            for (String innerFieldName : innerFieldNames) {
                if (outerTarget == null) {
                    break;
                }

                Field innerField = ReflectionUtils.findField(outerTarget.getClass(), innerFieldName);
                if (innerField == null) {
                    break;
                }

                ReflectionUtils.makeAccessible(innerField);
                innerObject = ReflectionUtils.getField(innerField, outerTarget);

                outerTarget = innerObject;
            }
        }

        return innerObject;
    }

    /* ------- #2 --------- JoinPoint get current method -------------------- */

    public static Method getAbstractMethod(JoinPoint pjp) {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        return method;
    }

    public static Method getImplMethod(JoinPoint pjp) throws NoSuchMethodException {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            method = pjp.getTarget().getClass().getDeclaredMethod(ms.getName(), method.getParameterTypes());
        }
        return method;
    }

    /* ------- #3 --------- get current thread current stack trace --------------- */

    /**
     * trim Thread.getStackTrace() 、JboxUtils.getStackTrace() stack in return StackTrace
     */
    private static final int STACK_BASE_DEEP = 2;

    public static String getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > STACK_BASE_DEEP) {
            StringBuilder sb = new StringBuilder("current thread [")
                    .append(Thread.currentThread().getName())
                    .append("] : ");

            for (int i = STACK_BASE_DEEP; i < stackTrace.length; ++i) {
                sb.append("\n\t")
                        .append(stackTrace[i]);
            }

            return sb.toString();
        }

        return "";
    }

    /* ------- #5 --------- String utils, trim prefix and suffix --------------- */

    public static String trimPrefixAndSuffix(String value, String prefix, String suffix, boolean isNeedJudge) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        Preconditions.checkArgument(prefix != null, "prefix can not be null");
        Preconditions.checkArgument(suffix != null, "suffix can not be null");

        if (!isNeedJudge) {
            return value.substring(prefix.length(), value.length() - suffix.length());
        } else {
            return trimPrefixAndSuffix(value, prefix, suffix);
        }
    }

    public static String trimPrefixAndSuffix(String value, String prefix, String suffix) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        Preconditions.checkArgument(prefix != null, "prefix can not be null");
        Preconditions.checkArgument(suffix != null, "suffix can not be null");

        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length());
        }
        if (value.endsWith(suffix)) {
            value = value.substring(0, value.length() - suffix.length());
        }

        return value;
    }

    /* ------- #6 --------- convert method to a simplified string -------------------- */

    private static final ConcurrentMap<Method, String> SIMPLIFIED_METHOD_NAME_MAP = new ConcurrentHashMap<>();

    public static String getSimplifiedMethodName(Method method) {
        return SIMPLIFIED_METHOD_NAME_MAP.computeIfAbsent(method, (m) -> {
            try {
                StringBuilder sb = new StringBuilder();

                specificToStringHeader(method, sb);

                sb.append('(');
                separateWithCommas(method.getParameterTypes(), sb);
                sb.append(')');
                if (method.getExceptionTypes().length > 0) {
                    sb.append(" throws ");
                    separateWithCommas(method.getExceptionTypes(), sb);
                }
                return sb.toString();
            } catch (Exception e) {
                return "<" + e + ">";
            }
        });
    }

    private static void specificToStringHeader(Method method, StringBuilder sb) {
        sb.append(trimName(method.getReturnType().getTypeName())).append(' ');
        sb.append(method.getDeclaringClass().getTypeName()).append(':');
        sb.append(method.getName());
    }

    private static void separateWithCommas(Class<?>[] types, StringBuilder sb) {
        for (int j = 0; j < types.length; j++) {
            sb.append(trimName(types[j].getTypeName()));
            if (j < (types.length - 1)) {
                sb.append(",");
            }
        }
    }

    private static String trimName(String name) {
        int index = name.lastIndexOf(".");
        if (index != -1) {
            name = name.substring(index + 1);
        }
        return name;
    }

    /* ------- #6 --------- convert String to Class Object -------------------- */

    private static final ConcurrentMap<Class, Class> primitiveTypes = new ConcurrentHashMap<>();

    static {
        primitiveTypes.put(byte.class, Byte.class);
        primitiveTypes.put(Byte.class, Byte.class);
        primitiveTypes.put(short.class, Short.class);
        primitiveTypes.put(Short.class, Short.class);
        primitiveTypes.put(int.class, Integer.class);
        primitiveTypes.put(Integer.class, Integer.class);
        primitiveTypes.put(long.class, Long.class);
        primitiveTypes.put(Long.class, Long.class);
        primitiveTypes.put(float.class, Float.class);
        primitiveTypes.put(Float.class, Float.class);
        primitiveTypes.put(double.class, Double.class);
        primitiveTypes.put(Double.class, Double.class);
        primitiveTypes.put(boolean.class, Boolean.class);
        primitiveTypes.put(Boolean.class, Boolean.class);
    }

    public static <T> Object convertTypeValue(String strValue, Class<T> type, Type genericType) {
        Object instance;
        Class<?> primitiveType = primitiveTypes.get(type);
        if (primitiveType != null) {
            try {
                instance = primitiveType.getMethod("valueOf", String.class).invoke(null, strValue);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else if (type == Character.class || type == char.class) {
            instance = strValue.charAt(0);
        } else if (type == String.class) {
            instance = strValue;
        } else {
            instance = JSON.parseObject(strValue, genericType);
        }

        return instance;
    }

    public static <T> T runWithNewMdcContext(Supplier<T> supplier, Map<String, String> newMdcContext) {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        if (Collections3.isNotEmpty(newMdcContext)) {
            MDC.setContextMap(newMdcContext);
        }

        try {
            return supplier.get();
        } finally {
            MDC.clear();
            if (Collections3.isNotEmpty(copyOfContextMap)) {
                MDC.setContextMap(copyOfContextMap);
            }
        }
    }

    public static <T> T runWithNewMdcContext(ExceptionableSupplier<T> supplier, Map<String, String> newMdcContext) throws Exception {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        if (Collections3.isNotEmpty(newMdcContext)) {
            MDC.setContextMap(newMdcContext);
        }

        try {
            return supplier.get();
        } finally {
            MDC.clear();
            if (Collections3.isNotEmpty(copyOfContextMap)) {
                MDC.setContextMap(copyOfContextMap);
            }
        }
    }

    public static <T> T runWithNewMdcContext(ThrowableSupplier<T> supplier, Map<String, String> newMdcContext) throws Throwable {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        if (Collections3.isNotEmpty(newMdcContext)) {
            MDC.setContextMap(newMdcContext);
        }

        try {
            return supplier.get();
        } finally {
            MDC.clear();
            if (Collections3.isNotEmpty(copyOfContextMap)) {
                MDC.setContextMap(copyOfContextMap);
            }
        }
    }

    public static <T> T runWithMdcContext(Supplier<T> supplier, Map<String, String> mdcContext) {
        if (Collections3.isNotEmpty(mdcContext)) {
            mdcContext.forEach(MDC::put);
        }
        try {
            return supplier.get();
        } finally {
            if (Collections3.isNotEmpty(mdcContext)) {
                mdcContext.keySet().forEach(MDC::remove);
            }
        }
    }

    public static <T> T runWithMdcContext(ThrowableSupplier<T> supplier, Map<String, String> mdcContext) throws Throwable {
        if (Collections3.isNotEmpty(mdcContext)) {
            mdcContext.forEach(MDC::put);
        }
        try {
            return supplier.get();
        } finally {
            if (Collections3.isNotEmpty(mdcContext)) {
                mdcContext.keySet().forEach(MDC::remove);
            }
        }
    }
}
