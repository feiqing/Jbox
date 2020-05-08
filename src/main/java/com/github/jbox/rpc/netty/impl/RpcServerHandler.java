package com.github.jbox.rpc.netty.impl;

import com.alibaba.fastjson.JSON;
import com.github.jbox.rpc.proto.RpcParam;
import com.github.jbox.rpc.proto.RpcResult;
import com.github.jbox.serializer.ISerializer;
import com.github.jbox.serializer.support.Hessian2Serializer;
import com.github.jbox.utils.IPv4;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import static com.github.jbox.utils.JboxUtils.runWithNewMdcContext;
import static com.github.jbox.utils.RpcUtil.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 5:50 PM.
 */
@AllArgsConstructor
@ChannelHandler.Sharable
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcParam> {

    private static final ISerializer serializer = new Hessian2Serializer();

    private ApplicationContext applicationContext;

    private Logger log;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcParam msg) throws Exception {
        runWithNewMdcContext((Supplier<Object>) () -> {

            long start = System.currentTimeMillis();
            Serializable result = null;
            Throwable except = null;
            try {
                Object bean = getBeanByClass(msg.getClassName(), applicationContext, log);
                if (bean != null) {
                    result = invoke(bean, msg.getMethodName(), msg.getArgs());
                    write(RpcResult.successOf(result), ctx);
                    return null;
                }

                bean = getBeanByName(msg.getClassName(), applicationContext, log);
                if (bean != null) {
                    result = invoke(bean, msg.getMethodName(), msg.getArgs());
                    write(RpcResult.successOf(result), ctx);
                    return null;
                }

                throw new RuntimeException("no bean is fond in spring context by class [" + msg.getClassName() + "].");
            } catch (Throwable t) {
                except = t;
                write(RpcResult.errorOf(t), ctx);
                return null;
            } finally {
                long cost = System.currentTimeMillis() - start;
                if (log.isDebugEnabled()) {
                    log.debug("|{}|{}|{}|{}:{}|{}|{}|{}|{}|{}|",
                            Thread.currentThread().getName(),
                            msg.getClientIp(),
                            IPv4.getLocalIp(),
                            msg.getClassName(), msg.getMethodName(),
                            cost,
                            JSON.toJSONString(msg.getArgs()),
                            result != null ? JSON.toJSONString(result) : "",
                            except != null ? JSON.toJSONString(except) : "",
                            JSON.toJSONString(msg.getMdcContext())
                    );
                }
            }
        }, msg.getMdcContext());
    }

    private void write(RpcResult result, ChannelHandlerContext ctx) {
        byte[] serialize = serializer.serialize(result);
        ctx.writeAndFlush(result);
    }

    private Serializable invoke(Object bean, String method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Method methodInstance = getMethod(bean.getClass(), method, log);
        return (Serializable) methodInstance.invoke(bean, args);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        // ctx.close();
    }
}
