package com.github.jbox.rpc.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.remote.*;
import com.github.jbox.rpc.akka.impl.ClientRouterActor;
import com.github.jbox.rpc.akka.impl.ClientServiceProxy;
import com.github.jbox.rpc.akka.impl.Configs;
import com.github.jbox.rpc.akka.impl.EventMonitorActor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Enhancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static com.github.jbox.rpc.akka.RpcClient.InvokeType.ASYNC;
import static com.github.jbox.rpc.akka.RpcClient.InvokeType.SYNC;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 7:01 PM.
 */
@SuppressWarnings("unchecked")
@Slf4j(topic = "JboxRpcClient")
public class RpcClient implements InitializingBean {

    private static final ConcurrentMap<String, Map<InvokeType, Map<Class, Object>>> ip2proxy = new ConcurrentHashMap<>();

    @Setter
    private int actorSize = 20;

    @Setter
    private int servPort = 22025;

    @Setter
    private long readTimeout = 200;

    private ActorRef router;

    @Override
    public void afterPropertiesSet() {
        this.router = Configs.actorSystem.actorOf(Props.create(ClientRouterActor.class, servPort, actorSize), "ClientRouterActor");
        ActorRef clientEventActor = Configs.actorSystem.actorOf(Props.create(EventMonitorActor.class), "EventMonitorActor");

        Configs.actorSystem.eventStream().subscribe(clientEventActor, DisassociatedEvent.class);
        Configs.actorSystem.eventStream().subscribe(clientEventActor, AssociationErrorEvent.class);

        Configs.actorSystem.eventStream().subscribe(clientEventActor, AssociatedEvent.class);
        Configs.actorSystem.eventStream().subscribe(clientEventActor, RemotingListenEvent.class);
        Configs.actorSystem.eventStream().subscribe(clientEventActor, RemotingShutdownEvent.class);
        Configs.actorSystem.eventStream().subscribe(clientEventActor, ThisActorSystemQuarantinedEvent.class);
        log.info("akka rpc client [{}] starting...", router.toString());
    }


    /**
     * 生成同步调用Proxy
     */
    public <T> T proxy(String servIp, Class<T> api) {
        Object proxy = getClassMap(servIp, SYNC).get(api);
        if (proxy != null) {
            return (T) proxy;
        }

        Enhancer en = new Enhancer();
        en.setSuperclass(api);
        en.setCallback(new ClientServiceProxy(api, SYNC, null, null, router, servIp, readTimeout));
        proxy = en.create();

        getClassMap(servIp, SYNC).put(api, proxy);

        return (T) proxy;
    }

    /**
     * 生成异步调用Proxy, 返回值和异常信息会在callbackExecutor内通过callback#call方法返回
     */
    public <T> T asyncProxy(String servIp, Class<T> api, Callback callback, ExecutorService callbackExecutor) {
        Object proxy = getClassMap(servIp, ASYNC).get(api);
        if (proxy != null) {
            return (T) proxy;
        }

        Enhancer en = new Enhancer();
        en.setSuperclass(api);
        en.setCallback(new ClientServiceProxy(api, ASYNC, callback, callbackExecutor, router, servIp, readTimeout));
        proxy = en.create();

        getClassMap(servIp, ASYNC).put(api, proxy);

        return (T) proxy;
    }

    private Map<Class, Object> getClassMap(String servIp, InvokeType invokeType) {
        return ip2proxy
                .computeIfAbsent(servIp, _K -> new ConcurrentHashMap<>())
                .computeIfAbsent(invokeType, _K -> new ConcurrentHashMap<>());
    }

    public enum InvokeType {
        SYNC,
        ASYNC
    }
}
