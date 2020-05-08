package com.github.jbox.rpc;

import com.github.jbox.rpc.netty.RpcServer;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 6:45 PM.
 */
public class RpcTest {

    public static void main(String[] args) throws Exception {
        RpcServer rpcServer = new RpcServer();
        rpcServer.afterPropertiesSet();
    }
}
