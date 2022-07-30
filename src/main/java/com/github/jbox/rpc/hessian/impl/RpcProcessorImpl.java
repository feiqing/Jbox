package com.github.jbox.rpc.hessian.impl;

import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.utils.Jbox;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.github.jbox.rpc.hessian.impl.Utils.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/2/21 11:47 AM.
 */
@Slf4j(topic = "JboxRpcServer")
public class RpcProcessorImpl implements RpcProcessor {

    private final ApplicationContext applicationContext;

    public RpcProcessorImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Serializable process(RpcParam param) throws Throwable {
        MDC.setContextMap(param.getMdcContext());
        long start = System.currentTimeMillis();
        Serializable result = null;
        Throwable except = null;
        try {
            Object bean = getBeanByClass(param.getClassName(), applicationContext, log);
            if (bean != null) {
                result = invoke(bean, param.getMethodName(), param.getArgs());
                return result;
            }

            bean = getBeanByName(param.getClassName(), applicationContext, log);
            if (bean != null) {
                result = invoke(bean, param.getMethodName(), param.getArgs());
                return result;
            }

            throw new RuntimeException("no bean is fond in spring context by class [" + param.getClassName() + "].");
        } catch (Throwable t) {
            except = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                        Thread.currentThread().getName(),
                        param.getClientIp(),
                        Jbox.getLocalIp(),
                        param.getClassName(), param.getMethodName(),
                        cost,
                        JSON.toJSONString(param.getArgs()),
                        result != null ? JSON.toJSONString(result) : "",
                        except != null ? JSON.toJSONString(except) : "",
                        JSON.toJSONString(param.getMdcContext())
                );
            }
            MDC.clear();
        }
    }

    private Serializable invoke(Object bean, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Method methodInstance = getMethod(bean.getClass(), method, log);
        return (Serializable) methodInstance.invoke(bean, args);
    }
}