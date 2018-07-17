package com.github.jbox.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-07-01 21:41:00.
 */
public abstract class AbstractSerializer implements ISerializer {

    private static final Logger logger = LoggerFactory.getLogger("com.github.jbox.serializer.ISerializer");

    protected abstract byte[] doSerialize(Object obj) throws Throwable;

    protected abstract Object doDeserialize(byte[] bytes) throws Throwable;

    @Override
    public <T> byte[] serialize(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return doSerialize(obj);
        } catch (Throwable t) {
            logger.error("{} serialize error.", this.getClass().getName(), t);
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
            return (T) doDeserialize(bytes);
        } catch (Throwable t) {
            logger.error("{} deserialize error.", this.getClass().getName(), t);
            return null;
        }
    }
}
