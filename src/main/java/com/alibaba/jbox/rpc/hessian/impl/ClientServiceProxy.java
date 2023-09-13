package com.alibaba.jbox.rpc.hessian.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.jbox.utils.Collections3;
import com.alibaba.jbox.utils.Jbox;
import com.alibaba.jbox.rpc.proto.RpcParam;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

import static com.alibaba.jbox.utils.Collections3.nullToEmpty;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/13 8:08 PM.
 */
@Slf4j(topic = "JboxRpcClient")
public class ClientServiceProxy implements MethodInterceptor {

    private final Class<?> api;

    private final RpcProcessor rpcProcessor;

    private final String servIp;

    public ClientServiceProxy(Class<?> api, RpcProcessor rpcProcessor, String servIp) {
        this.api = api;
        this.rpcProcessor = rpcProcessor;
        this.servIp = servIp;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

        RpcParam msg = new RpcParam();
        msg.setClientIp(Jbox.getLocalIp());
        msg.setServIp(servIp);
        msg.setClassName(api.getName());
        msg.setMethodName(method.getName());
        msg.setArgs(args);
        msg.setMdcContext(Collections3.nullToEmpty(MDC.getCopyOfContextMap()));

        Object result = null;
        Throwable except = null;
        long start = System.currentTimeMillis();
        try {
            result = rpcProcessor.process(msg);
        } catch (Throwable t) {
            except = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                        Thread.currentThread().getName(),
                        Jbox.getLocalIp(),
                        servIp,
                        msg.getClassName(), msg.getMethodName(),
                        cost,
                        JSON.toJSONString(msg.getArgs()),
                        result != null ? JSON.toJSONString(result) : "",
                        except != null ? JSON.toJSONString(except) : "",
                        JSON.toJSONString(msg.getMdcContext())
                );
            }
        }

        return result;
    }
}
