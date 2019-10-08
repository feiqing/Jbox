package com.github.jbox.rpc.proto;

import akka.serialization.JSerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2019/10/8 3:36 PM.
 */
public class RpcSerializer extends JSerializer {

    private Hessian2Serializer serializer = new Hessian2Serializer();

    @Override
    public int identifier() {
        return 22025;
    }

    @Override
    public byte[] toBinary(Object o) {
        return serializer.serialize(o);
    }

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        return serializer.deserialize(bytes);
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}
