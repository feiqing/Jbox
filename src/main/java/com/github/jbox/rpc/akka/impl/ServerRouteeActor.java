package com.github.jbox.rpc.akka.impl;

import akka.actor.UntypedActor;
import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.github.jbox.utils.JboxUtils.runWithNewMdcContext;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 6:53 PM.
 */
@Slf4j(topic = "JboxRpcServer")
public class ServerRouteeActor extends UntypedActor {

    private static final ConcurrentMap<String, AtomicReference<Object>> class2bean = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, AtomicReference<Object>> name2bean = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    public ServerRouteeActor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onReceive(Object message) {
        if (!(message instanceof RpcParam)) {
            unhandled(message);
            return;
        }

        RpcParam msg = (RpcParam) message;

        runWithNewMdcContext((Supplier<Object>) () -> {

            long start = System.currentTimeMillis();
            Serializable result = null;
            Throwable except = null;
            try {
                Object bean = getBeanByClass(msg.getClassName());
                if (bean != null) {
                    result = invoke(bean, msg.getMethodName(), msg.getArgs());
                    getSender().tell(RpcResult.successOf(result), getSelf());
                    return null;
                }

                bean = getBeanByName(msg.getClassName());
                if (bean != null) {
                    result = invoke(bean, msg.getMethodName(), msg.getArgs());
                    getSender().tell(RpcResult.successOf(result), getSelf());
                    return null;
                }

                throw new RuntimeException("no bean is fond in spring context by class [" + msg.getClassName() + "].");
            } catch (Throwable t) {
                except = t;
                getSender().tell(RpcResult.errorOf(t), getSelf());
                return null;
            } finally {
                long cost = System.currentTimeMillis() - start;
                if (log.isDebugEnabled()) {
                    log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                            Thread.currentThread().getName(),
                            msg.getClientIp(),
                            IPv4.getLocalIp(),
                            msg.getClassName(), msg.getMethodName(),
                            cost,
                            JSON.toJSONString(msg.getArgs()),
                            result != null ? JSON.toJSONString(result) : "",
                            except != null ? JSON.toJSONString(except) : "",
                            JSON.toJSONString(msg.getMdcContext())
                    );
                }
            }
        }, msg.getMdcContext());
    }


    private Serializable invoke(Object bean, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Method methodInstance = getMethod(bean.getClass(), method);
        return (Serializable) methodInstance.invoke(bean, args);
    }

    private static final ConcurrentMap<Class, Map<String, Method>> cache = new ConcurrentHashMap<>();

    private Method getMethod(Class clazz, String methodName) {
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


    private Object getBeanByClass(String clazz) {
        AtomicReference<Object> optional = class2bean.get(clazz);
        if (optional != null) {
            return optional.get();
        }

        Object bean = doGetBeanByClass(clazz);
        class2bean.put(clazz, new AtomicReference<>(bean));

        return bean;
    }

    private Object doGetBeanByClass(String clazz) {
        try {
            Map<String, ?> beans = applicationContext.getBeansOfType(Class.forName(clazz));
            if (CollectionUtils.isEmpty(beans)) {
                return null;
            }

            if (beans.size() == 1) {
                return beans.values().iterator().next();
            }

            for (Object bean : beans.values()) {
                if (bean.getClass().getName().equals(clazz)) {
                    return bean;
                }
            }
        } catch (Throwable t) {
            log.error("doGetBeanByClass error:[{}]", clazz, t);
        }

        return null;
    }

    private Object getBeanByName(String clazz) {
        AtomicReference<Object> optional = name2bean.get(clazz);
        if (optional != null) {
            return optional.get();
        }

        Object bean = doGetBeanByName(clazz);
        name2bean.put(clazz, new AtomicReference<>(bean));

        return bean;
    }

    private Object doGetBeanByName(String clazz) {
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
