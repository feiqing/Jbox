package com.github.jbox.rpc.akka.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.utils.IPv4;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.lang.reflect.Method;

import static com.github.jbox.utils.Collections3.nullToEmpty;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 7:08 PM.
 */
@Slf4j(topic = "JboxRpcClient")
public class ClientServiceProxy implements MethodInterceptor {

    private Class<?> api;

    /**
     * @see ClientRouterActor
     */
    private ActorRef router;

    private String servIp;

    private long readTimeout;

    public ClientServiceProxy(Class<?> api, ActorRef router, String servIp, long readTimeout) {
        this.api = api;
        this.router = router;
        this.servIp = servIp;
        this.readTimeout = readTimeout;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {

        RpcParam msg = new RpcParam();
        msg.setClientIp(IPv4.getLocalIp());
        msg.setServIp(servIp);
        msg.setClassName(api.getName());
        msg.setMethodName(method.getName());
        msg.setArgs(args);
        msg.setMdcContext(nullToEmpty(MDC.getCopyOfContextMap()));

        RpcResult result = null;
        Throwable except = null;
        long start = System.currentTimeMillis();
        try {
            Timeout timeout = new Timeout(Duration.create(readTimeout, "ms"));
            Future<Object> future = Patterns.ask(router, msg, timeout);
            result = (RpcResult) Await.result(future, timeout.duration());

            if (result.getException() != null) {
                throw result.getException();
            }

        } catch (Throwable t) {
            except = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (log.isDebugEnabled()) {
                log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                        Thread.currentThread().getName(),
                        IPv4.getLocalIp(),
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

        return result.getData();
    }
}
