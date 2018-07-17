package com.github.jbox.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * @author jifang.zjf@albiaba-inc.com
 * @version 1.0
 * @since 2017/8/2 17:15:00.
 */
@SuppressWarnings("all")
public class BeanCopyUtil {

    private static final ConcurrentMap<Class, Supplier<Object>> primitiveSuppliers = new ConcurrentHashMap<Class, Supplier<Object>>() {
        private static final long serialVersionUID = -4085587013134835589L;

        {
            put(byte.class, () -> 0);
            put(Byte.class, () -> new Byte((byte) 0));
            put(short.class, () -> 0);
            put(Short.class, () -> new Short((short) 0));
            put(int.class, () -> 0);
            put(Integer.class, () -> new Integer(0));
            put(long.class, () -> 0L);
            put(Long.class, () -> new Long(0L));
            put(float.class, () -> 0.0F);
            put(Float.class, () -> new Float(0.0f));
            put(double.class, () -> 0.0);
            put(Double.class, () -> new Double(0.0));
            put(boolean.class, () -> false);
            put(Boolean.class, () -> new Boolean(false));
            put(String.class, () -> "");
        }
    };

    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> srcFields = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> dstFields = new ConcurrentHashMap<>();

    public static <T, R> R copy(T srcObject, R dstObject) {
        return copy(srcObject, dstObject, null);
    }

    public static <T, R> R copy(T srcObject, R dstObject, String... excludeFields) {
        Preconditions.checkArgument(srcObject != null, "no input object specified");
        Preconditions.checkArgument(dstObject != null, "no output object specified");

        Class<?> dstClass = dstObject.getClass();
        if (primitiveSuppliers.containsKey(dstClass) && primitiveSuppliers.containsKey(srcObject.getClass())) {
            dstObject = (R) JboxUtils.convertTypeValue(String.valueOf(srcObject), dstClass, null);
            return dstObject;
        }

        ConcurrentMap<String, Field> dstFieldMap = getDstFields(dstObject.getClass());
        if (excludeFields != null) {
            for (String excludeField : excludeFields) {
                if (!Strings.isNullOrEmpty(excludeField)) {
                    dstFieldMap.remove(excludeField);
                }
            }
        }

        ConcurrentMap<String, Field> srcFieldMap = getSrcFields(srcObject.getClass());
        try {
            for (Map.Entry<String, Field> dstFieldEntry : dstFieldMap.entrySet()) {
                String dstFieldName = dstFieldEntry.getKey();
                Field srcField = srcFieldMap.get(dstFieldName);

                Object srcFieldValue;
                // dst类内包含相应属性 且 属性值不为null
                if (srcField != null && (srcFieldValue = FieldUtils.readField(srcField, srcObject, true)) != null) {
                    Object dstFieldValue = makeDstFieldValue(dstObject, srcObject, dstFieldEntry.getValue(), srcField, srcFieldValue);
                    FieldUtils.writeField(dstFieldEntry.getValue(), dstObject, dstFieldValue, true);
                }
            }
        } catch (IllegalAccessException ignored) {
        }

        return dstObject;
    }

    private static Object makeDstFieldValue(Object dstObject, Object srcObject, Field dstField, Field srcField, Object srcFieldValue) {
        Class<?> dstFieldType = dstField.getType();
        Type dstFieldGenericType = dstField.getGenericType();

        Class<?> srcFieldType = srcField.getType();
        Type srcFieldGenericType = srcField.getGenericType();

        // 如果srcFieldType直接是dstFieldType的子类, 则可安全的直接赋值
        if (isInheritType(dstFieldType, dstFieldGenericType, srcFieldType, srcFieldGenericType)) {
            // make sure use clone
            return srcFieldValue;
        }
        // 均是基本类型(or包装类型), 且类型不一致
        else if (primitiveSuppliers.containsKey(dstFieldType) && primitiveSuppliers.containsKey(srcFieldType)) {
            return JboxUtils.convertTypeValue(String.valueOf(srcFieldValue), dstFieldType, dstFieldGenericType);
        }
        // 均是Collection类型
        else if (Collection.class.isAssignableFrom(dstFieldType) && srcFieldValue instanceof Collection) {
            return copyCollectionValues(dstFieldGenericType, (Collection) srcFieldValue);
        }
        // 均是Map类型
        else if (Map.class.isAssignableFrom(dstFieldType) && srcFieldValue instanceof Map) {
            return copyMapValues(dstFieldGenericType, (Map) srcFieldValue);
        } else {
            return recursionCopy(srcFieldValue, dstFieldType);
        }
    }

    /**
     * 递归copy
     *
     * @param srcValue
     * @param dstFieldType
     * @return
     */
    private static Object recursionCopy(Object srcValue, Class<?> dstFieldType) {
        return copy(srcValue, newInstance(dstFieldType));
    }

    /**
     * collection类型copy
     *
     * @param dstFieldGenericType
     * @param srcCollection
     * @return
     */
    private static Collection copyCollectionValues(Type dstFieldGenericType, Collection srcCollection) {
        Class<?> collectionGenericType = getCollectionGenericType(dstFieldGenericType);

        Collection dstCollection = (Collection) newInstance(srcCollection.getClass());
        if (collectionGenericType == null) {
            dstCollection.addAll(srcCollection);
        } else {
            for (Object srcListValue : srcCollection) {
                dstCollection.add(copy(srcListValue, newInstance(collectionGenericType)));
            }
        }

        return dstCollection;
    }

    private static Class getCollectionGenericType(Type dstFieldGenericType) {
        if (dstFieldGenericType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) dstFieldGenericType).getActualTypeArguments()[0];
        }
        return null;
    }

    /**
     * Map 类型copy
     *
     * @param dstFieldGenericType
     * @param srcMap
     * @return
     */
    private static Map copyMapValues(Type dstFieldGenericType, Map srcMap) {
        Class<?>[] mapGenericType = getMapGenericType(dstFieldGenericType);

        Map dstMap = (Map) newInstance(srcMap.getClass());
        if (mapGenericType == null) {
            dstMap.putAll(srcMap);
        } else {
            for (Object entryObj : srcMap.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObj;

                Class<?> dstEntryKeyType = mapGenericType[0];
                Object dstEntryKey = copy(entry.getKey(), newInstance(dstEntryKeyType));

                Class<?> dstEntryValueType = mapGenericType[1];
                Object dstEntryValue = copy(entry.getValue(), newInstance(dstEntryValueType));

                dstMap.put(dstEntryKey, dstEntryValue);
            }
        }
        return dstMap;
    }

    private static Class<?>[] getMapGenericType(Type dstFieldGenericType) {
        if (dstFieldGenericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) dstFieldGenericType).getActualTypeArguments();
            return new Class[]{(Class) actualTypeArguments[0], (Class) actualTypeArguments[1]};
        }

        return null;
    }

    private static boolean isInheritType(Class<?> dstFieldType, Type dstFieldGenericType,
                                         Class<?> srcFieldType, Type srcFieldGenericType) {
        if (dstFieldType.isAssignableFrom(srcFieldType)) {
            if (isGenericInheritType(dstFieldGenericType, srcFieldGenericType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isGenericInheritType(Type dstFieldGenericType, Type srcFieldGenericType) {
        int parameterTypeCount = 0;
        parameterTypeCount += (dstFieldGenericType instanceof ParameterizedType ? 1 : 0);
        parameterTypeCount += (srcFieldGenericType instanceof ParameterizedType ? 1 : 0);

        if (parameterTypeCount == 0) {
            return true;
        } else if (parameterTypeCount == 1) {
            return false;
        } else {
            Type[] dstFieldActualTypeArguments = ((ParameterizedType) dstFieldGenericType).getActualTypeArguments();
            Type[] srcFieldActualTypeArguments = ((ParameterizedType) srcFieldGenericType).getActualTypeArguments();

            if (dstFieldActualTypeArguments.length != srcFieldActualTypeArguments.length) {
                return false;
            }

            for (int i = 0; i < dstFieldActualTypeArguments.length; ++i) {
                Class<?> dstFieldActualTypeClass = (Class<?>) dstFieldActualTypeArguments[i];
                if (!(dstFieldActualTypeClass.isAssignableFrom((Class<?>) srcFieldActualTypeArguments[i]))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static ConcurrentMap<String, Field> getSrcFields(Class<?> clazz) {
        return srcFields.computeIfAbsent(clazz, (key) -> {
            ConcurrentMap<String, Field> outputFieldMap = new ConcurrentHashMap<>();

            List<Field> inputFields = FieldUtils.getAllFieldsList(clazz);

            for (Field inputField : inputFields) {
                outputFieldMap.put(inputField.getName(), inputField);
            }

            return outputFieldMap;
        });
    }

    private static ConcurrentMap<String, Field> getDstFields(Class<?> clazz) {
        return dstFields.computeIfAbsent(clazz, (key) -> {

            ConcurrentMap<String, Field> outputFieldMap = new ConcurrentHashMap<>();
            List<Field> outputFields = FieldUtils.getAllFieldsList(clazz);

            for (Field outputField : outputFields) {
                if (outputNeedCopy(outputField)) {
                    outputFieldMap.put(outputField.getName(), outputField);
                }
            }

            return outputFieldMap;
        });
    }

    private static Object newInstance(Class<?> clazz) {
        return primitiveSuppliers.getOrDefault(clazz, () -> {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new BeanCopyException(e);
            }
        }).get();
    }

    private static boolean outputNeedCopy(Field field) {
        return !Modifier.isFinal(field.getModifiers());
    }

    private static final class BeanCopyException extends RuntimeException {

        private static final long serialVersionUID = 8469477770174284379L;

        BeanCopyException(Throwable cause) {
            super(cause);
        }
    }
}
