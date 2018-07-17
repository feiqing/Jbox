package com.github.jbox.spring;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.github.jbox.utils.JboxUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

import static com.github.jbox.utils.JboxUtils.trimPrefixAndSuffix;

/**
 * 使得非Spring托管的Bean也可以使用{@link Autowired}、{@link Qualifier}、{@link Value}、{@link Resource}注解,
 * 并使{@code Value}可动态生效.
 *
 * @author jifang.zjf@alibaba-inc.com
 * @version 1.0
 * @since 2017/6/7 下午8:28.
 */
public abstract class SpringAutowiredAdaptor {

    private AtomicBoolean init = new AtomicBoolean(false);

    @PostConstruct
    protected void initAutowired() {
        if (!init.compareAndSet(false, true)) {
            return;
        }

        for (Field field : FieldUtils.getAllFieldsList(this.getClass())) {
            ReflectionUtils.makeAccessible(field);

            Value value;
            Qualifier qualifier;
            Resource resource;
            if ((value = field.getAnnotation(Value.class)) != null) {
                String valStr = JboxUtils.trimPrefixAndSuffix(value.value(), "${", "}");
                Object property = DynamicPropertySourcesPlaceholder.getProperty(valStr);
                Preconditions.checkState(property != null,
                    "could not found the config '" + value.value() + "' relative property");

                Object propertyValue = JboxUtils.convertTypeValue(String.valueOf(property), field.getType(),
                    field.getGenericType());
                // set field
                ReflectionUtils.setField(field, this, propertyValue);
                // register external container, usage dynamic setter
                DynamicPropertySourcesPlaceholder.registerExternalBean(this);
                // logger
                SpringLoggerHelper.warn("success: class '{}' field: '{}' value is autowired to [{}]",
                    this.getClass().getName(),
                    field.getName(), propertyValue);
            } else if (field.isAnnotationPresent(Autowired.class)) {
                Object propertyValue;
                if ((qualifier = field.getAnnotation(Qualifier.class)) != null) {
                    propertyValue = DynamicPropertySourcesPlaceholder.getSpringBean(qualifier.value());
                } else {
                    propertyValue = DynamicPropertySourcesPlaceholder.getSpringBean(field.getType());
                }

                ReflectionUtils.setField(field, this, propertyValue);

                // logger
                SpringLoggerHelper.warn("success: class '{}' field: '{}' value is autowired to [{}]",
                    this.getClass().getName(),
                    field.getName(), propertyValue);
            } else if ((resource = field.getAnnotation(Resource.class)) != null) {
                String resourceName = resource.name();
                Object propertyValue;

                if (Strings.isNullOrEmpty(resourceName)) {
                    propertyValue = DynamicPropertySourcesPlaceholder.getSpringBean(field.getType());
                } else {
                    propertyValue = DynamicPropertySourcesPlaceholder.getSpringBean(resourceName);
                }

                ReflectionUtils.setField(field, this, propertyValue);

                // logger
                SpringLoggerHelper.warn("success: class '{}' field: '{}' value is autowired to [{}]",
                    this.getClass().getName(),
                    field.getName(), propertyValue);
            }
        }
    }
}
