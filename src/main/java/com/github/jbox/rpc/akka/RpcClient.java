package com.github.jbox.rpc.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.remote.*;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cglib.proxy.Enhancer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 7:01 PM.
 */
@Slf4j(topic = "JboxRpcClient")
public class RpcClient implements InitializingBean {

    private static final ConcurrentMap<String, Map<Class, Object>> ip2proxy = new ConcurrentHashMap<>();

    @Setter
    private int actorSize = 20;

    @Setter
    private int servPort = 22025;

    @Setter
    private long readTimeout = 200;

    private ActorRef router;

    @Override
    public void afterPropertiesSet() {
        this.router = ActorSystems.actorSystem.actorOf(Props.create(ClientRouterActor.class, servPort, actorSize), "ClientRouterActor");
        ActorRef clientEventActor = ActorSystems.actorSystem.actorOf(Props.create(ClientEventActor.class), "ClientEventActor");
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, AssociatedEvent.class);
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, DisassociatedEvent.class);
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, AssociationErrorEvent.class);
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, RemotingListenEvent.class);
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, RemotingShutdownEvent.class);
        ActorSystems.actorSystem.eventStream().subscribe(clientEventActor, ThisActorSystemQuarantinedEvent.class);
        log.info("akka rpc client [{}] starting...", router.toString());
    }


    /**
     * 用法同HessionRpcClient
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(String servIp, Class<T> api) {
        Object proxy = ip2proxy.getOrDefault(servIp, Collections.emptyMap()).get(api);
        if (proxy != null) {
            return (T) proxy;
        }

        Enhancer en = new Enhancer();
        en.setSuperclass(api);
        en.setCallback(new RpcProxy(api, router, servIp, readTimeout));
        proxy = en.create();

        ip2proxy.computeIfAbsent(servIp, (_k) -> new ConcurrentHashMap<>()).put(api, proxy);

        return (T) proxy;
    }
}
