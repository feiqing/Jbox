package com.github.jbox.rpc.hessian.impl;

import com.alibaba.fastjson.JSON;
import com.github.jbox.helpers.ThrowableSupplier;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.github.jbox.utils.JboxUtils.runWithNewMdcContext;
import static com.github.jbox.utils.RpcUtil.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/2/21 11:47 AM.
 */
@Slf4j(topic = "JboxRpcServer")
public class RpcProcessorImpl implements RpcProcessor {

    private ApplicationContext applicationContext;

    public RpcProcessorImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Serializable process(RpcParam param) throws Throwable {
        return runWithNewMdcContext((ThrowableSupplier<Serializable>) () -> {

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
                            IPv4.getLocalIp(),
                            param.getClassName(), param.getMethodName(),
                            cost,
                            JSON.toJSONString(param.getArgs()),
                            result != null ? JSON.toJSONString(result) : "",
                            except != null ? JSON.toJSONString(except) : "",
                            JSON.toJSONString(param.getMdcContext())
                    );
                }
            }

        }, param.getMdcContext());
    }

    private Serializable invoke(Object bean, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Method methodInstance = getMethod(bean.getClass(), method, log);
        return (Serializable) methodInstance.invoke(bean, args);
    }
}