package com.github.jbox.rpc.netty;

import com.github.jbox.rpc.netty.impl.RpcParamCodec;
import com.github.jbox.rpc.netty.impl.RpcResultCodec;
import com.github.jbox.rpc.netty.impl.RpcServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.net.InetSocketAddress;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2020/4/15 5:30 PM.
 */
@Slf4j(topic = "JboxRpcServer")
public class RpcServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    @Setter
    private int parentThreadSize = 3;

    @Setter
    private int childThreadSize = Runtime.getRuntime().availableProcessors() * 2;

    @Setter
    private int servPort = 48042;

    private ApplicationContext applicationContext;

    private EventLoopGroup parrentGroup;

    private EventLoopGroup childGroup;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RpcServerHandler handler = new RpcServerHandler(applicationContext, log);

        this.parrentGroup = new NioEventLoopGroup(parentThreadSize);
        this.childGroup = new NioEventLoopGroup(childThreadSize);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(this.parrentGroup, this.childGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(servPort))
                // .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                // .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                // .option(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024)
                // .option(ChannelOption.SO_SNDBUF, 8 * 1024 * 1024)
                // .childOption(ChannelOption.SO_RCVBUF, 8 * 1024 * 1024)
                // .childOption(ChannelOption.SO_SNDBUF, 8 * 1024 * 1024)
                // .childOption(ChannelOption.SO_KEEPALIVE, true)
                // .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("RpcParamDecoder", new RpcParamCodec())
//                                .addLast(new ChannelOutboundHandlerAdapter())
                                .addLast("RpcResultEncoder", new RpcResultCodec())

                                .addLast("RpcServerHandler", handler);


                    }
                });

        serverBootstrap.bind().sync();


    }

    @Override
    public void destroy() throws Exception {
        if (parrentGroup != null) {
            parrentGroup.shutdownGracefully().sync();
        }
        if (childGroup != null) {
            childGroup.shutdownGracefully().sync();
        }
    }
}
