package com.github.jbox.rpc.netty.impl;

import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 5:46 PM.
 */
public class RpcParamDecoder extends ByteToMessageDecoder {

    private static final ISerializer serializer = new Hessian2Serializer();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] array = new byte[in.readableBytes()];
        in.getBytes(in.readerIndex(), array);
        Object deserialize = serializer.deserialize(array);
        if (!(deserialize instanceof RpcParam)) {
            // todo
            throw new RuntimeException("fuck");
        }
        out.add(deserialize);
        // todo ? ReferenceCountUtil.release(in);
    }
}
