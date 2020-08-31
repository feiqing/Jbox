package com.github.jbox.rpc.akka.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 1:25 AM.
 */
public class ServerRouterActor extends UntypedActor {

    private ApplicationContext applicationContext;

    private int actorSize;

    /**
     * @see ServerRouteeActor
     */
    private Router router;

    public ServerRouterActor(ApplicationContext applicationContext, int actorSize) {
        this.applicationContext = applicationContext;
        this.actorSize = actorSize;
    }


    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>(actorSize);
        for (int i = 0; i < actorSize; ++i) {
            Props props = Props.create(ServerRouteeActor.class, applicationContext).withDispatcher("akka-server-routee-dispatcher");

            ActorRef target = getContext().actorOf(props, "ServerRouteeActor:" + i);
            routees.add(new ActorRefRoutee(target));
        }

        this.router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object message) {
        router.route(message, getSender());
    }
}
