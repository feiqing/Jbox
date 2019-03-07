package com.github.jbox.rpc.proto;

import com.alibaba.fastjson.JSON;
import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j(topic = "JboxRpc")
public class RpcProxy implements MethodInterceptor {

    private Class<?> api;

    private RpcProcessor rpcProcessor;

    private String servIp;

    private boolean logParams;

    private boolean logRetObj;

    private boolean logMdcCtx;

    public RpcProxy(Class<?> api, RpcProcessor rpcProcessor, String servIp, boolean logParams, boolean logRetObj, boolean logMdcCtx) {
        this.api = api;
        this.rpcProcessor = rpcProcessor;
        this.servIp = servIp;
        this.logParams = logParams;
        this.logRetObj = logRetObj;
        this.logMdcCtx = logMdcCtx;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

        RpcMsg msg = new RpcMsg();
        msg.setClassName(api.getName());
        msg.setMethodName(method.getName());
        msg.setArgs(args);
        msg.setMdcContext(nullToEmpty(MDC.getCopyOfContextMap()));

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
            log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                    Thread.currentThread().getName(),
                    IPv4.getLocalIp(),
                    servIp,
                    msg.getClassName(), msg.getMethodName(),
                    cost,
                    logParams ? JSON.toJSONString(msg.getArgs()) : "",
                    (result != null && logRetObj) ? JSON.toJSONString(result) : "",
                    except != null ? JSON.toJSONString(except) : "",
                    logMdcCtx ? JSON.toJSONString(msg.getMdcContext()) : ""
            );
        }

        return result;
    }
}
