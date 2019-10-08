package com.github.jbox.rpc.proto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 11:40 AM.
 */
@Data
public class RpcResult implements Serializable {

    private static final long serialVersionUID = 130653952174876875L;

    private Object data;

    private Throwable exception;

    public static RpcResult successOf(Object data) {
        RpcResult result = new RpcResult();
        result.setData(data);
        result.setException(null);

        return result;
    }

    public static RpcResult errorOf(Throwable exception) {
        RpcResult result = new RpcResult();
        result.setData(null);
        result.setException(exception);

        return result;
    }
}
