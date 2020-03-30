package com.github.jbox.rpc.akka.impl;

import akka.actor.UntypedActor;
import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import static com.github.jbox.utils.RpcUtil.*;
import static com.github.jbox.utils.JboxUtils.runWithNewMdcContext;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 6:53 PM.
 */
@Slf4j(topic = "JboxRpcServer")
public class ServerRouteeActor extends UntypedActor {

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
                Object bean = getBeanByClass(msg.getClassName(), applicationContext, log);
                if (bean != null) {
                    result = invoke(bean, msg.getMethodName(), msg.getArgs());
                    getSender().tell(RpcResult.successOf(result), getSelf());
                    return null;
                }

                bean = getBeanByName(msg.getClassName(), applicationContext, log);
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
        Method methodInstance = getMethod(bean.getClass(), method, log);
        return (Serializable) methodInstance.invoke(bean, args);
    }
}
