package com.github.jbox.rpc.netty.impl;

import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/16 11:51 AM.
 */
public class RpcParamCodec extends ByteToMessageCodec<RpcParam> {

    private static final ISerializer serializer = new Hessian2Serializer();

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcParam msg, ByteBuf out) throws Exception {
        out.writeBytes(serializer.serialize(msg));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] array = new byte[in.readableBytes()];
        in.readBytes(array);
        out.add(serializer.deserialize(array));
//        in.skipBytes(in.readableBytes());
    }
}
