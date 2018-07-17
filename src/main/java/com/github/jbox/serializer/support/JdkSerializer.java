package com.github.jbox.serializer.support;

import com.github.jbox.serializer.AbstractSerializer;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-02 23:47:00.
 */
public class JdkSerializer extends AbstractSerializer {

    @Override
    protected byte[] doSerialize(Object obj) {
        return SerializationUtils.serialize((Serializable) obj);
    }

    @Override
    protected Object doDeserialize(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
