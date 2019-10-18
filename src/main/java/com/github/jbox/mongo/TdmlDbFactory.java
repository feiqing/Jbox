package com.github.jbox.mongo;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.slf4j.MDC;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/9/19 4:45 PM.
 */
public class TdmlDbFactory<T extends MongoBatis> implements FactoryBean<T> {

    private static final String DEFAULT_STAND_BY_MDC_KEY = "__TDML_PROXY_DB_STAND_BY_MDC_KEY__";

    private static final Set<Method> metas = new HashSet<>();

    static {
        metas.addAll(Arrays.asList(ReflectionUtils.getAllDeclaredMethods(Object.class)));
    }

    // 不能是static, 每个实例单独一个
    private ConcurrentMap<String, Routee> router = new ConcurrentHashMap<>();

    private String slotKey;

    private Class<?> type;

    private T proxy;

    public TdmlDbFactory(String slotKey, List<Routee<T>> routees) {
        precheck(slotKey, routees);

        for (Routee<T> routee : routees) {
            if (routee.isStandby()) {
                router.put(DEFAULT_STAND_BY_MDC_KEY, routee);
            } else {
                routee.getSlots().forEach(slot -> router.put(slot, routee));
            }
        }

        this.slotKey = slotKey;
        this.type = routees.get(0).getTarget().getClass();
        this.proxy = newProxy();
    }

    @SuppressWarnings("unchecked")
    private T newProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setCallback(callback);

        return (T) enhancer.create(new Class[]{MongoOperations.class, SequenceDAO.class}, new Object[]{null, null});
    }

    private Callback callback = new MethodInterceptor() {

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (metas.contains(method)) {
                return proxy.invoke(this, args);
            }

            String slot = MDC.get(slotKey);
            if (Strings.isNullOrEmpty(slot)) {
                throw new TdmlException("could not find slot:[{}] value in mdc-context.", slotKey);
            }

            Routee routee = router.get(slot);
            if (routee == null) {
                routee = router.get(DEFAULT_STAND_BY_MDC_KEY);
            }

            return proxy.invoke(routee.getTarget(), args);
        }
    };

    private void precheck(String mdcKey, List<Routee<T>> routees) {
        if (Strings.isNullOrEmpty(mdcKey)) {
            throw new TdmlException("mdcKey is empty");
        }
        if (CollectionUtils.isEmpty(routees)) {
            throw new TdmlException("mdcKey:[{}]'s routees empty", mdcKey);
        }

        Set<String> ids = new HashSet<>();
        Set<String> slots = new HashSet<>();
        boolean hasStandby = false;
        Class<?> type = null;
        for (Routee<T> routee : routees) {
            if (!ids.add(routee.getId())) {
                throw new TdmlException("routee id:[{}] is duplicated.", routee.getId());
            }

            if (routee.isStandby()) {
                hasStandby = true;
            } else if (!Sets.intersection(slots, routee.getSlots()).isEmpty()) {
                throw new TdmlException("routee id:[{}] slots has duplicate value with before.", routee.getId(), routee.getSlots());
            } else {
                slots.addAll(routee.getSlots());
            }

            Class<? extends MongoBatis> routeeType = routee.getTarget().getClass();
            if (type != null && type != routeeType) {
                throw new TdmlException("routee id:[{}] target type:[{}] is not same as before:[{}].", routee.getId(), routeeType.getName(), type.getName());
            }
            type = routeeType;
        }

        if (!hasStandby) {
            throw new TdmlException("has none standby routee, is very dangerous!!!");
        }
    }

    @Override
    public T getObject() {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
