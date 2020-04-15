package com.github.jbox.rpc.akka.impl;

import akka.actor.ActorRef;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.akka.Callback;
import com.github.jbox.rpc.akka.RpcClient;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.utils.IPv4;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

import static com.github.jbox.utils.Collections3.nullToEmpty;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 7:08 PM.
 */
@Slf4j(topic = "JboxRpcClient")
public class ClientServiceProxy implements MethodInterceptor {

    private Class<?> api;

    private RpcClient.InvokeType invokeType;

    private Callback callback;

    private ExecutorService callbackExecutor;

    /**
     * @see ClientRouterActor
     */
    private ActorRef router;

    private String servIp;

    private long readTimeout;

    public ClientServiceProxy(Class<?> api,
                              RpcClient.InvokeType invokeType, Callback callback, ExecutorService callbackExecutor,
                              ActorRef router, String servIp, long readTimeout) {
        this.api = api;
        this.invokeType = invokeType;
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
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
            if (invokeType == RpcClient.InvokeType.ASYNC) {
                onAsync(future, msg);
            } else {
                result = onSync(future, timeout);
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

        return result == null ? null : result.getData();
    }

    private RpcResult onSync(Future<Object> future, Timeout timeout) throws Throwable {
        RpcResult result = (RpcResult) Await.result(future, timeout.duration());

        if (result.getException() != null) {
            throw result.getException();
        }

        return result;
    }

    private void onAsync(Future<Object> future, RpcParam msg) {
        if (callback == null) {
            return;
        }

        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object obj) throws Throwable {
                RpcResult result = (RpcResult) obj;
                if (result == null) {
                    result = new RpcResult();
                }
                if (result.getException() != null) {
                    throw result.getException();
                }
                if (failure != null) {
                    result.setException(failure);
                }

                callback.call(msg, result);
            }
        }, getCallbackExecutor());
    }

    private ExecutionContext getCallbackExecutor() {
        if (callbackExecutor == null) {
            callbackExecutor = MoreExecutors.newDirectExecutorService();
        }
        return ExecutionContexts.fromExecutor(callbackExecutor);
    }
}
