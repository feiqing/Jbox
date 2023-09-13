package com.alibaba.jbox.rpc.hessian.impl;

import com.alibaba.jbox.rpc.proto.RpcParam;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/2/21 11:46 AM.
 */
public interface RpcProcessor {
    
    Object process(RpcParam param) throws Throwable;
}