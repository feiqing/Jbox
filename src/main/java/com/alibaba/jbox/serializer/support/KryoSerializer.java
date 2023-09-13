package com.alibaba.jbox.serializer.support;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayOutputStream;

/**
 * @author jifang.zjf
 * @since 2017/6/19 下午4:53.
 */
public class KryoSerializer extends AbstractSerializer {

    private static final ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        return k;
    });

    @Override
    protected byte[] _serialize(Object obj) throws Throwable {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Output output = new Output(bos)) {
            kryo.get().writeClassAndObject(output, obj);
            output.flush();

            return bos.toByteArray();
        }
    }

    @Override
    protected Object _deserialize(byte[] bytes) {
        try (Input input = new Input(bytes)) {
            return kryo.get().readClassAndObject(input);
        }
    }
}
