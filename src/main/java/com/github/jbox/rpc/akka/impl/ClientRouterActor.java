package com.github.jbox.rpc.akka.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 2:05 AM.
 */
public class ClientRouterActor extends UntypedActor {

    private int servPort;

    private int actorSize;

    /**
     * @see ClientRouteeActor
     */
    private Router router;

    public ClientRouterActor(int servPort, int actorSize) {
        this.servPort = servPort;
        this.actorSize = actorSize;
    }

    @Override
    public void preStart() {
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < actorSize; ++i) {
            ActorRef actor = getContext().actorOf(Props.create(ClientRouteeActor.class, servPort), "ClientRouteeActor:" + i);
            routees.add(new ActorRefRoutee(actor));
        }
        this.router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    @Override
    public void onReceive(Object message) {
        router.route(message, getSender());
    }
}
