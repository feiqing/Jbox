package com.github.jbox.rpc.netty.impl;

import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 5:58 PM.
 */
public class RpcResultEncoder extends MessageToByteEncoder<RpcResult> {

    private static final ISerializer serializer = new Hessian2Serializer();

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcResult msg, ByteBuf out) throws Exception {
        out.writeBytes(serializer.serialize(msg));
    }
}
