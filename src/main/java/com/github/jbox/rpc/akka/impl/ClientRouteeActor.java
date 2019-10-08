package com.github.jbox.rpc.akka.impl;

import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.google.common.base.Strings;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/7 10:14 PM.
 */
public class ClientRouteeActor extends UntypedActor {

    private static final ConcurrentMap<String, ActorSelection> routees = new ConcurrentHashMap<>();

    private int servPort;

    public ClientRouteeActor(int servPort) {
        this.servPort = servPort;
    }

    @Override
    public void onReceive(Object message) {
        String servIp = ((RpcParam) message).getServIp();
        String failed = Configs.getFailed(servIp);
        if (!Strings.isNullOrEmpty(failed)) {
            getSender().tell(RpcResult.errorOf(new RuntimeException(failed)), getSender());
            return;
        }

        ActorSelection actor = routees.get(servIp);
        if (actor == null) {
            actor = getContext().actorSelection("akka.tcp://RpcActorSystem@" + servIp + ":" + servPort + "/user/ServerRouterActor");
            routees.put(servIp, actor);
        }

        actor.forward(message, getContext());
    }
}
