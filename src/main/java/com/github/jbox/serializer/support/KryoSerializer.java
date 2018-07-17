package com.github.jbox.serializer.support;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.github.jbox.serializer.AbstractSerializer;

import java.io.ByteArrayOutputStream;

/**
 * @author jifang.zjf
 * @since 2017/6/19 下午4:53.
 */
public class KryoSerializer extends AbstractSerializer {

    @Override
    protected byte[] doSerialize(Object obj) throws Throwable {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Output output = new Output(bos)) {
            new Kryo().writeClassAndObject(output, obj);
            output.flush();

            return bos.toByteArray();
        }
    }

    @Override
    protected Object doDeserialize(byte[] bytes) {
        try (Input input = new Input(bytes)) {
            return new Kryo().readClassAndObject(input);
        }
    }
}
