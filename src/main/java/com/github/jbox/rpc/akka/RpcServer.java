package com.github.jbox.rpc.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.github.jbox.rpc.akka.impl.Configs;
import com.github.jbox.rpc.akka.impl.ServerRouterActor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/6 6:40 PM.
 */
@Slf4j(topic = "JboxRpcServer")
public class RpcServer implements ApplicationContextAware {

    @Setter
    private int actorSize = 20;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ActorRef actor = Configs.actorSystem.actorOf(Props.create(ServerRouterActor.class, applicationContext, actorSize), "ServerRouterActor");
        log.info("akka rpc server [{}] starting...", actor.toString());
    }
}

