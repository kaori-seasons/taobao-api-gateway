package com.taobao.gateway.dispatcher;

import com.taobao.gateway.dispatcher.impl.DefaultRequestDispatcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty NIO+Reactor模型的分发层服务器
 * 高性能API网关的核心分发组件
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class NettyDispatcherServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyDispatcherServer.class);

    @Autowired
    private DispatcherConfig dispatcherConfig;

    @Autowired
    private DefaultRequestDispatcher requestDispatcher;

    /**
     * 主Reactor线程组（接收连接的线程）
     */
    private EventLoopGroup mainReactorGroup;

    /**
     * 子Reactor线程组（处理IO的线程）
     */
    private EventLoopGroup subReactorGroup;

    /**
     * 服务器通道
     */
    private Channel serverChannel;

    /**
     * 是否已启动
     */
    private volatile boolean isRunning = false;

    /**
     * 启动分发层服务器
     */
    public void start() throws Exception {
        if (isRunning) {
            logger.warn("分发层服务器已经在运行中");
            return;
        }

        logger.info("正在启动分发层服务器，配置: {}", dispatcherConfig);

        // 创建主Reactor线程组（接收连接的线程）
        mainReactorGroup = new NioEventLoopGroup(dispatcherConfig.getMainReactorThreads());
        
        // 创建子Reactor线程组（处理IO的线程）
        subReactorGroup = new NioEventLoopGroup(dispatcherConfig.getSubReactorThreads());

        try {
            // 创建服务器启动引导类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(mainReactorGroup, subReactorGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 配置Channel管道
                            ch.pipeline()
                                    // HTTP编解码器
                                    .addLast(new HttpServerCodec())
                                    // HTTP消息聚合器
                                    .addLast(new HttpObjectAggregator(dispatcherConfig.getMaxContentLength()))
                                    // 空闲状态检测
                                    .addLast(new IdleStateHandler(
                                            dispatcherConfig.getReadTimeout() / 1000,
                                            dispatcherConfig.getWriteTimeout() / 1000,
                                            0,
                                            TimeUnit.SECONDS))
                                    // 自定义分发处理器
                                    .addLast(new DispatcherChannelHandler(requestDispatcher));
                        }
                    })
                    // 设置TCP选项
                    .option(ChannelOption.SO_BACKLOG, dispatcherConfig.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, dispatcherConfig.isReuseAddr())
                    .childOption(ChannelOption.SO_KEEPALIVE, dispatcherConfig.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, dispatcherConfig.isTcpNoDelay());

            // 如果启用直接内存
            if (dispatcherConfig.isDirectBuffer()) {
                bootstrap.childOption(ChannelOption.ALLOCATOR, 
                        new io.netty.buffer.PooledByteBufAllocator(true));
            }

            // 绑定端口并启动服务器
            serverChannel = bootstrap.bind(dispatcherConfig.getPort()).sync().channel();
            isRunning = true;
            
            logger.info("分发层服务器启动成功，监听端口: {}", dispatcherConfig.getPort());
            logger.info("主Reactor线程数: {}, 子Reactor线程数: {}, 业务线程池大小: {}", 
                    dispatcherConfig.getMainReactorThreads(),
                    dispatcherConfig.getSubReactorThreads(),
                    dispatcherConfig.getBusinessThreadPoolSize());

            // 等待服务器关闭
            serverChannel.closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            shutdown();
        }
    }

    /**
     * 停止分发层服务器
     */
    public void stop() {
        logger.info("正在停止分发层服务器...");
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        shutdown();
        isRunning = false;
        logger.info("分发层服务器已停止");
    }

    /**
     * 优雅关闭线程组
     */
    private void shutdown() {
        if (mainReactorGroup != null) {
            mainReactorGroup.shutdownGracefully();
        }
        if (subReactorGroup != null) {
            subReactorGroup.shutdownGracefully();
        }
        if (requestDispatcher != null) {
            requestDispatcher.shutdown();
        }
    }

    /**
     * 应用关闭时的清理工作
     */
    @PreDestroy
    public void destroy() {
        stop();
    }

    /**
     * 获取服务器状态
     */
    public boolean isRunning() {
        return isRunning && serverChannel != null && serverChannel.isActive();
    }

    /**
     * 获取服务器配置
     */
    public DispatcherConfig getConfig() {
        return dispatcherConfig;
    }

    /**
     * 分发层通道处理器
     * 负责处理HTTP请求并分发给业务处理器
     */
    private static class DispatcherChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final DefaultRequestDispatcher requestDispatcher;

        public DispatcherChannelHandler(DefaultRequestDispatcher requestDispatcher) {
            this.requestDispatcher = requestDispatcher;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            logger.debug("收到HTTP请求: {} {}", request.method(), request.uri());

            // 创建请求上下文
            RequestContext context = new RequestContext(request, ctx.channel());

            // 异步分发请求
            requestDispatcher.dispatch(context)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("请求分发失败: {}", context.getRequestId(), throwable);
                        }
                    });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("分发层通道处理异常", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    logger.warn("连接空闲超时，关闭连接");
                    ctx.close();
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("客户端连接建立: {}", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("客户端连接断开: {}", ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }
    }
} 