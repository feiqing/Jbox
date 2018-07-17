package com.github.jbox.spring;

import com.github.jbox.utils.AopTargetUtils;
import com.github.jbox.utils.JboxUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static com.github.jbox.utils.JboxUtils.convertTypeValue;

/**
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.6
 * - 1.0: a simple vitamin faced use {@link PlaceholderConfigurerSupport};
 * - 1.1: replace use {@link PropertySourcesPlaceholderConfigurer}, 从3.1版本起Spring其装配.
 * - 1.2: use {@link 'Diamond'} for replace Vitamin;
 * - 1.3: @deprecated : load yaml file as a config file format;
 * - 1.4: add {@link 'ValueHandler'} when Field value changed.
 * - 1.5: @deprecated {@link 'ValueHandler'}, user {@link Value} method callback.s
 * - 1.6: independent
 * @since 2017/4/5 上午10:35.
 */
public abstract class DynamicPropertySourcesPlaceholder extends PropertySourcesPlaceholderConfigurer implements ApplicationContextAware {

    private static final String SEP_LINE = "\n";

    private static final String SEP_KV = ":";

    private static Multimap<String, Setter> configKey2SetterMap = HashMultimap.create();

    private static Properties totalProperties = new Properties();

    private static Set<Object> externalBeans = new HashSet<>();

    private static ApplicationContext applicationContext;

    private volatile String preConfigData;

    /*  -------------------------------------   */
    /*  --------------- APIs ----------------   */
    /*  -------------------------------------   */
    public static BeanFactory getApplicationContext() {
        return applicationContext;
    }

    public static Object getSpringBean(String id) {
        return applicationContext.getBean(id);
    }

    public static <T> T getSpringBean(Class<T> type) {
        return applicationContext.getBean(type);
    }

    public static void registerExternalBean(Object bean) {
        externalBeans.add(bean);
    }

    public static Properties getProperties() {
        return totalProperties;
    }

    public static Object getProperty(Object key) {
        return totalProperties.get(key);
    }

    protected abstract String getInitConfigData();

    protected void onDynamicConfigData(String dynamicConfigData) {
        try {
            Map<String, String> config = parseConfig(dynamicConfigData);
            SpringLoggerHelper.info("receive dynamic config data: {}", config);
            if (StringUtils.equals(preConfigData, dynamicConfigData)) {
                return;
            }

            // handle current config  changed
            handleDataChange(config);
            // save as pre config
            preConfigData = dynamicConfigData;
        } catch (Exception e) {
            SpringLoggerHelper.error("handle dynamic config data: {{}} error.", dynamicConfigData, e);
        }
    }

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties mergedProperties = super.mergeProperties();

        String initConfigData;
        if (!Strings.isNullOrEmpty(initConfigData = getInitConfigData())) {
            mergedProperties.putAll(parseConfig(initConfigData));
        }

        totalProperties.putAll(mergedProperties);
        return mergedProperties;
    }

    /*  -------------------------------------   */
    /*  ------- Handle Data Changes ---------   */
    /*  -------------------------------------   */
    private void handleDataChange(Map<String, String> config) {
        initSetterMap(applicationContext);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String configKey = entry.getKey();
            String stringValue = entry.getValue();

            // make sure is changed.
            if (stringValue.equals(totalProperties.get(configKey))) {
                SpringLoggerHelper.warn("passed: config [{}]'s value [{}] is equals current value '{}'.",
                        configKey, stringValue, totalProperties.get(configKey));
                continue;
            }

            // make sure has relative bean.
            Collection<Setter> setters = configKey2SetterMap.get(configKey);
            if (isNullOrEmpty(setters)) {
                SpringLoggerHelper.error("passed: config [{}] have none relative bean.", configKey);
                continue;
            }

            // update field changed.
            for (Setter setter : setters) {
                Object value = convertTypeValue(stringValue, setter.getType(), setter.getGenericType());
                setter.set(value);
            }

            // update saved properties.
            totalProperties.put(configKey, stringValue);
        }
    }

    private void initSetterMap(ApplicationContext applicationContext) {
        // init once
        if (!configKey2SetterMap.isEmpty()) {
            return;
        }

        // register spring container bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            // bug fix
            if (bean == null) {
                continue;
            }
            initSetter(AopTargetUtils.getAopTarget(bean));
        }

        // register spring external bean
        for (Object bean : externalBeans) {
            initSetter(AopTargetUtils.getAopTarget(bean));
        }
    }

    private void initSetter(Object bean) {
        if (bean == null) {
            return;
        }

        for (Field field : FieldUtils.getFieldsListWithAnnotation(bean.getClass(), Value.class)) {
            String annotationConfigKey = getFieldAnnotationValue(field);

            configKey2SetterMap.put(annotationConfigKey, Setter.withField(annotationConfigKey, bean, field));
        }

        for (Method method : getMethodsListWithAnnotation(bean.getClass(), Value.class)) {
            String annotationConfigKey = getMethodAnnotationValue(method);

            configKey2SetterMap.put(annotationConfigKey, Setter.withMethod(annotationConfigKey, bean, method));
        }
    }

    private String getFieldAnnotationValue(Field field) {
        ReflectionUtils.makeAccessible(field);
        return getAnnotationValue(field.getAnnotation(Value.class).value());
    }

    private String getMethodAnnotationValue(Method method) {
        ReflectionUtils.makeAccessible(method);
        return getAnnotationValue(method.getAnnotation(Value.class).value());
    }

    private String getAnnotationValue(String value) {
        value = value.trim();
        Preconditions.checkState(!Strings.isNullOrEmpty(value), "@value() config can not be empty.");
        return JboxUtils.trimPrefixAndSuffix(value, "${", "}");
    }

    /* ------ helpers ----- */
    private Map<String, String> parseConfig(String config) {
        List<String> lines = Splitter.on(SEP_LINE).trimResults().omitEmptyStrings().splitToList(config);
        Map<String, String> configs = new HashMap<>();
        if (!isNullOrEmpty(lines)) {
            for (String line : lines) {
                if (line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                List<String> kv = Splitter.on(SEP_KV).trimResults().omitEmptyStrings().splitToList(line);

                if (kv.size() != 2) {
                    String msg = MessageFormatter.arrayFormat("Dynamic data line [{}] is not standard 'key:value' property format.",
                            new Object[]{line}).getMessage();
                    throw new PropertySourcesPlaceholderException(msg);
                }

                String key = kv.get(0);
                String value = trimValueSuffix(kv.get(1));
                if (configs.containsKey(key)) {
                    SpringLoggerHelper.warn("key:[{}] is duplicated, use value:'{}' as default.", key, value);
                }

                configs.put(key, value);
            }
        }

        return configs;
    }

    private String trimValueSuffix(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        int index = value.indexOf("//");
        if (index != -1) {
            value = value.substring(0, index);
        }

        index = value.indexOf("#");
        if (index != -1) {
            value = value.substring(0, index);
        }
        return value.trim();
    }

    private static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    private static boolean isNullOrEmpty(Map map) {
        return map == null || map.isEmpty();
    }

    /**
     * copy from spring
     *
     * @param cls
     * @param annotationCls
     * @return
     */
    public static List<Method> getMethodsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        Validate.isTrue(cls != null, "The class must not be null");
        Validate.isTrue(annotationCls != null, "The annotation class must not be null");
        // replace getMethods() -> getDeclaredMethods()
        final Method[] allMethods = cls.getDeclaredMethods();
        final List<Method> annotatedMethods = new ArrayList<Method>();
        for (final Method method : allMethods) {
            if (method.getAnnotation(annotationCls) != null) {
                annotatedMethods.add(method);
            }
        }
        return annotatedMethods;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DynamicPropertySourcesPlaceholder.applicationContext = applicationContext;
    }

    @Data
    private static class Setter {

        private String propertyName;

        private Object bean;

        private Field field;

        private Method method;

        private Class<?> type;

        private Type genericType;

        private Setter() {
        }

        static Setter withField(String propertyName, Object bean, Field field) {
            Preconditions.checkNotNull(propertyName);
            Preconditions.checkNotNull(bean);
            Preconditions.checkNotNull(field);

            Setter setter = new Setter();
            setter.propertyName = propertyName;
            setter.bean = bean;
            setter.field = field;
            setter.type = field.getType();
            setter.genericType = field.getGenericType();
            return setter;
        }

        static Setter withMethod(String propertyName, Object bean, Method method) {
            Preconditions.checkNotNull(propertyName);
            Preconditions.checkNotNull(bean);
            Preconditions.checkNotNull(method);

            Setter setter = new Setter();
            setter.propertyName = propertyName;
            setter.bean = bean;
            setter.method = method;

            Class<?>[] parameterTypes = method.getParameterTypes();
            Preconditions.checkState(parameterTypes != null && parameterTypes.length == 1);
            setter.type = parameterTypes[0];

            Type[] genericParameterTypes = method.getGenericParameterTypes();
            if (genericParameterTypes == null || genericParameterTypes.length < 1) {
                setter.genericType = null;
            } else {
                setter.genericType = genericParameterTypes[0];
            }

            return setter;
        }

        public void set(Object value) {
            try {
                if (field != null) {
                    field.set(bean, value);
                } else {
                    method.invoke(bean, value);
                }
                SpringLoggerHelper.warn("success: class '{}' property: '{}' updated to [{}].",
                        bean.getClass().getName(), propertyName, value);
            } catch (Throwable e) {
                SpringLoggerHelper.error("failed: class '{}' property: '{}' update occur exception.",
                        bean.getClass().getName(), propertyName, e);
            }
        }
    }

    public static class PropertySourcesPlaceholderException extends RuntimeException {

        private static final long serialVersionUID = 3949739146397637634L;

        PropertySourcesPlaceholderException(String message) {
            super(message);
        }
    }
}
