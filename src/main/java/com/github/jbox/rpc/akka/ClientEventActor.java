package com.github.jbox.rpc.akka;

import akka.actor.UntypedActor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 2:21 AM.
 */
@Slf4j(topic = "JboxRpcClient")
public class ClientEventActor extends UntypedActor {

    @Override
    public void onReceive(Object message) {
        log.warn("Event: {}", message);
    }
}
