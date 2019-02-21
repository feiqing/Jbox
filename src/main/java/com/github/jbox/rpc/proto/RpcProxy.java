package com.github.jbox.rpc.proto;

import org.slf4j.MDC;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

import static com.github.jbox.utils.Collections3.nullToEmpty;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018/11/13 8:08 PM.
 */
public class RpcProxy implements MethodInterceptor {

    private Class<?> api;

    private RpcProcessor rpcProcessor;

    public RpcProxy(Class<?> api, RpcProcessor rpcProcessor) {
        this.api = api;
        this.rpcProcessor = rpcProcessor;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

        RpcMsg msg = new RpcMsg();
        msg.setClassName(api.getName());
        msg.setMethodName(method.getName());
        msg.setArgs(args);
        msg.setMdcContext(nullToEmpty(MDC.getCopyOfContextMap()));

        return rpcProcessor.process(msg);
    }
}
