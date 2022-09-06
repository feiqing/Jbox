package com.github.jbox.serializer.support;

import com.github.jbox.serializer.ISerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 21:41:00.
 */
@Slf4j(topic = "com.github.jbox.serializer")
abstract class AbstractSerializer implements ISerializer {

    protected abstract byte[] _serialize(Object obj) throws Throwable;

    protected abstract Object _deserialize(byte[] bytes) throws Throwable;

    @Override
    public <T> byte[] serialize(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return _serialize(obj);
        } catch (Throwable t) {
            log.error("{} serialize error.", this.getClass().getName(), t);
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            return (T) _deserialize(bytes);
        } catch (Throwable t) {
            log.error("{} deserialize error.", this.getClass().getName(), t);
            return null;
        }
    }
}
