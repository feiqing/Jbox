package com.github.jbox.rpc.akka;

import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 10:35 AM.
 */
@FunctionalInterface
public interface Callback {

    void call(RpcParam param, RpcResult result);
}
