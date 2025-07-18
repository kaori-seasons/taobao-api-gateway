package com.taobao.gateway.server;

import com.taobao.gateway.config.NettyConfig;
import com.taobao.gateway.handler.HttpRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
 * Netty服务器类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    @Autowired
    private NettyConfig nettyConfig;

    @Autowired
    private HttpRequestHandler httpRequestHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    /**
     * 启动Netty服务器
     */
    public void start() throws Exception {
        logger.info("正在启动Netty服务器，配置: {}", nettyConfig);

        // 创建Boss线程组（接收连接的线程）
        bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreads());
        
        // 创建Worker线程组（处理IO的线程）
        workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreads());

        try {
            // 创建服务器启动引导类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 配置Channel管道
                            ch.pipeline()
                                    // HTTP编解码器
                                    .addLast(new HttpServerCodec())
                                    // HTTP消息聚合器，限制最大消息大小为1MB
                                    .addLast(new HttpObjectAggregator(1024 * 1024))
                                    // 空闲状态检测
                                    .addLast(new IdleStateHandler(
                                            nettyConfig.getReadTimeout() / 1000,
                                            nettyConfig.getWriteTimeout() / 1000,
                                            0,
                                            TimeUnit.SECONDS))
                                    // 自定义HTTP请求处理器
                                    .addLast(httpRequestHandler);
                        }
                    })
                    // 设置TCP选项
                    .option(ChannelOption.SO_BACKLOG, nettyConfig.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, nettyConfig.isReuseAddr())
                    .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.isKeepAlive())
                    .childOption(ChannelOption.TCP_NODELAY, nettyConfig.isTcpNoDelay());

            // 绑定端口并启动服务器
            serverChannel = bootstrap.bind(nettyConfig.getPort()).sync().channel();
            logger.info("Netty服务器启动成功，监听端口: {}", nettyConfig.getPort());

            // 等待服务器关闭
            serverChannel.closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            shutdown();
        }
    }

    /**
     * 停止Netty服务器
     */
    public void stop() {
        logger.info("正在停止Netty服务器...");
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        shutdown();
        logger.info("Netty服务器已停止");
    }

    /**
     * 优雅关闭线程组
     */
    private void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
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
        return serverChannel != null && serverChannel.isActive();
    }
} 