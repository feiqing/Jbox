package com.alibaba.jbox.serializer;

/**
 * @author zhoupan@weidian.com
 * @since 16/7/8.
 */
public interface ISerializer {

    <T> byte[] serialize(T obj);

    <T> T deserialize(byte[] bytes);
}
