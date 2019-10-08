package com.github.jbox.rpc.akka;

import akka.actor.ActorSystem;
import com.github.jbox.utils.IPv4;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 10:45 AM.
 */
class ActorSystems {

    static final ActorSystem actorSystem;

    static {
        Config config = ConfigFactory
                .parseString("akka.remote.netty.tcp.hostname=\"" + IPv4.getLocalIp() + "\"")
                .withFallback(ConfigFactory.load("akka.conf"));

        actorSystem = ActorSystem.create("RpcActorSystem", config);
    }
}
