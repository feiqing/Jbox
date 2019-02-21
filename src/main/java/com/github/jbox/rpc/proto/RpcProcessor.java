package com.github.jbox.rpc.proto;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/2/21 11:46 AM.
 */
public interface RpcProcessor {
    /**
     * 所有Rpc接口都会经过该方法
     *
     * @param msg
     * @return
     * @throws Throwable
     */
    Object process(RpcMsg msg) throws Throwable;
}