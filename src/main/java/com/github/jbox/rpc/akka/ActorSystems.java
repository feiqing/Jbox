package com.github.jbox.rpc.akka;

import akka.actor.ActorSystem;
import com.github.jbox.utils.IPv4;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.concurrent.TimeUnit;

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

    private static final LoadingCache<String, String> failedServs = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) {
                    return "";
                }
            });

    static void putFailed(String servIp, String reason) {
        failedServs.put(servIp, reason);
    }

    static void removeFailed(String servIp) {
        failedServs.invalidate(servIp);
    }

    static String getFailed(String servIp) {
        return failedServs.getUnchecked(servIp);
    }
}
