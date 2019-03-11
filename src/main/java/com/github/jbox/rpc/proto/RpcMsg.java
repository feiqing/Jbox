package com.github.jbox.rpc.proto;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/2/21 11:46 AM.
 */
@Data
@NoArgsConstructor
public class RpcMsg implements Serializable {

    private static final long serialVersionUID = 2006423345030709062L;

    private String clientIp;

    private String className;

    private String methodName;

    private Object[] args;

    private Map<String, String> mdcContext;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
