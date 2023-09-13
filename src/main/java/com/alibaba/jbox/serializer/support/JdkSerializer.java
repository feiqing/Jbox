package com.alibaba.jbox.serializer.support;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-02 23:47:00.
 */
public class JdkSerializer extends AbstractSerializer {

    @Override
    protected byte[] _serialize(Object obj) {
        return SerializationUtils.serialize((Serializable) obj);
    }

    @Override
    protected Object _deserialize(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
