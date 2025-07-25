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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多Reactor分发层服务器
 * 支持百亿级别请求的高性能多Reactor模型
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
@Component
public class MultiReactorDispatcherServer {

    private static final Logger logger = LoggerFactory.getLogger(MultiReactorDispatcherServer.class);

    @Autowired
    private MultiReactorDispatcherConfig config;

    @Autowired
    private DefaultRequestDispatcher requestDispatcher;

    /**
     * Reactor实例映射
     */
    private final ConcurrentHashMap<String, ReactorInstance> reactorInstances;

    /**
     * 负载均衡器
     */
    private final ReactorLoadBalancer loadBalancer;

    /**
     * 是否已启动
     */
    private volatile boolean isRunning = false;

    /**
     * 构造函数
     */
    public MultiReactorDispatcherServer() {
        this.reactorInstances = new ConcurrentHashMap<>();
        this.loadBalancer = new ReactorLoadBalancer();
    }

    /**
     * 启动多Reactor分发层服务器
     */
    public void start() throws Exception {
        if (isRunning) {
            logger.warn("多Reactor分发层服务器已经在运行中");
            return;
        }

        if (!config.isEnabled()) {
            logger.info("多Reactor分发层服务器已禁用");
            return;
        }

        logger.info("正在启动多Reactor分发层服务器，配置: {}", config);

        List<MultiReactorDispatcherConfig.ReactorConfig> reactors = config.getReactors();
        if (reactors == null || reactors.isEmpty()) {
            logger.error("未配置Reactor实例");
            return;
        }

        // 启动所有Reactor实例
        for (MultiReactorDispatcherConfig.ReactorConfig reactorConfig : reactors) {
            if (reactorConfig.isEnabled()) {
                startReactor(reactorConfig);
            }
        }

        isRunning = true;
        logger.info("多Reactor分发层服务器启动成功，共启动 {} 个Reactor实例", reactorInstances.size());
    }

    /**
     * 启动单个Reactor实例
     */
    private void startReactor(MultiReactorDispatcherConfig.ReactorConfig reactorConfig) throws Exception {
        String reactorId = reactorConfig.getId();
        int port = reactorConfig.getPort();

        logger.info("启动Reactor实例: {}，端口: {}", reactorId, port);

        // 创建主Reactor线程组
        EventLoopGroup mainReactorGroup = new NioEventLoopGroup(reactorConfig.getMainReactorThreads());
        
        // 创建子Reactor线程组
        EventLoopGroup subReactorGroup = new NioEventLoopGroup(reactorConfig.getSubReactorThreads());

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
                                    .addLast(new HttpObjectAggregator(config.getPerformance().getMemoryPoolSize() * 1024 * 1024))
                                    // 空闲状态检测
                                    .addLast(new IdleStateHandler(
                                            config.getConnectionPool().getConnectionTimeout() / 1000,
                                            config.getConnectionPool().getConnectionTimeout() / 1000,
                                            0,
                                            TimeUnit.SECONDS))
                                    // 自定义分发处理器
                                    .addLast(new MultiReactorChannelHandler(requestDispatcher, reactorId));
                        }
                    })
                    // 设置TCP选项
                    .option(ChannelOption.SO_BACKLOG, reactorConfig.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // 性能优化配置
            if (config.getPerformance().isDirectBuffer()) {
                bootstrap.childOption(ChannelOption.ALLOCATOR, 
                        new io.netty.buffer.PooledByteBufAllocator(true));
            }

            // 绑定端口并启动服务器
            Channel serverChannel = bootstrap.bind(port).sync().channel();
            
            // 创建Reactor实例
            ReactorInstance instance = new ReactorInstance(
                    reactorId, 
                    port, 
                    mainReactorGroup, 
                    subReactorGroup, 
                    serverChannel,
                    reactorConfig.getWeight()
            );
            
            reactorInstances.put(reactorId, instance);
            loadBalancer.addReactor(instance);

            logger.info("Reactor实例启动成功: {}，端口: {}，权重: {}", 
                    reactorId, port, reactorConfig.getWeight());

        } catch (Exception e) {
            logger.error("启动Reactor实例失败: {}", reactorId, e);
            mainReactorGroup.shutdownGracefully();
            subReactorGroup.shutdownGracefully();
            throw e;
        }
    }

    /**
     * 停止多Reactor分发层服务器
     */
    public void stop() {
        logger.info("正在停止多Reactor分发层服务器...");
        
        // 停止所有Reactor实例
        for (ReactorInstance instance : reactorInstances.values()) {
            stopReactor(instance);
        }
        
        reactorInstances.clear();
        loadBalancer.clear();
        isRunning = false;
        logger.info("多Reactor分发层服务器已停止");
    }

    /**
     * 停止单个Reactor实例
     */
    private void stopReactor(ReactorInstance instance) {
        logger.info("停止Reactor实例: {}", instance.getId());
        
        if (instance.getServerChannel() != null) {
            instance.getServerChannel().close();
        }
        
        if (instance.getMainReactorGroup() != null) {
            instance.getMainReactorGroup().shutdownGracefully();
        }
        
        if (instance.getSubReactorGroup() != null) {
            instance.getSubReactorGroup().shutdownGracefully();
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
        return isRunning && !reactorInstances.isEmpty();
    }

    /**
     * 获取活跃的Reactor实例数量
     */
    public int getActiveReactorCount() {
        return reactorInstances.size();
    }

    /**
     * 获取负载均衡器
     */
    public ReactorLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * 获取配置
     */
    public MultiReactorDispatcherConfig getConfig() {
        return config;
    }

    /**
     * Reactor实例
     */
    public static class ReactorInstance {
        private final String id;
        private final int port;
        private final EventLoopGroup mainReactorGroup;
        private final EventLoopGroup subReactorGroup;
        private final Channel serverChannel;
        private final int weight;
        private final AtomicInteger activeConnections;

        public ReactorInstance(String id, int port, EventLoopGroup mainReactorGroup, 
                             EventLoopGroup subReactorGroup, Channel serverChannel, int weight) {
            this.id = id;
            this.port = port;
            this.mainReactorGroup = mainReactorGroup;
            this.subReactorGroup = subReactorGroup;
            this.serverChannel = serverChannel;
            this.weight = weight;
            this.activeConnections = new AtomicInteger(0);
        }

        // Getter方法
        public String getId() { return id; }
        public int getPort() { return port; }
        public EventLoopGroup getMainReactorGroup() { return mainReactorGroup; }
        public EventLoopGroup getSubReactorGroup() { return subReactorGroup; }
        public Channel getServerChannel() { return serverChannel; }
        public int getWeight() { return weight; }
        public int getActiveConnections() { return activeConnections.get(); }
        
        public void incrementConnections() { activeConnections.incrementAndGet(); }
        public void decrementConnections() { activeConnections.decrementAndGet(); }
    }

    /**
     * Reactor负载均衡器
     */
    public static class ReactorLoadBalancer {
        private final ConcurrentHashMap<String, ReactorInstance> reactors;
        private final AtomicInteger currentIndex;

        public ReactorLoadBalancer() {
            this.reactors = new ConcurrentHashMap<>();
            this.currentIndex = new AtomicInteger(0);
        }

        public void addReactor(ReactorInstance reactor) {
            reactors.put(reactor.getId(), reactor);
        }

        public void removeReactor(String reactorId) {
            reactors.remove(reactorId);
        }

        public void clear() {
            reactors.clear();
        }

        /**
         * 轮询选择Reactor
         */
        public ReactorInstance selectRoundRobin() {
            if (reactors.isEmpty()) {
                return null;
            }
            
            ReactorInstance[] instances = reactors.values().toArray(new ReactorInstance[0]);
            int index = currentIndex.getAndIncrement() % instances.length;
            return instances[index];
        }

        /**
         * 最少连接数选择Reactor
         */
        public ReactorInstance selectLeastConnections() {
            return reactors.values().stream()
                    .min((r1, r2) -> Integer.compare(r1.getActiveConnections(), r2.getActiveConnections()))
                    .orElse(null);
        }

        /**
         * 权重选择Reactor
         */
        public ReactorInstance selectWeighted() {
            if (reactors.isEmpty()) {
                return null;
            }

            int totalWeight = reactors.values().stream().mapToInt(ReactorInstance::getWeight).sum();
            int random = (int) (Math.random() * totalWeight);
            
            int currentWeight = 0;
            for (ReactorInstance reactor : reactors.values()) {
                currentWeight += reactor.getWeight();
                if (random < currentWeight) {
                    return reactor;
                }
            }
            
            return reactors.values().iterator().next();
        }

        public int getReactorCount() {
            return reactors.size();
        }
    }

    /**
     * 多Reactor通道处理器
     */
    private static class MultiReactorChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final DefaultRequestDispatcher requestDispatcher;
        private final String reactorId;

        public MultiReactorChannelHandler(DefaultRequestDispatcher requestDispatcher, String reactorId) {
            this.requestDispatcher = requestDispatcher;
            this.reactorId = reactorId;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            logger.debug("Reactor {} 收到HTTP请求: {} {}", reactorId, request.method(), request.uri());

            // 创建请求上下文
            RequestContext context = new RequestContext(request, ctx.channel());
            context.setAttribute("reactorId", reactorId);

            // 异步分发请求
            requestDispatcher.dispatch(context)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Reactor {} 请求分发失败: {}", reactorId, context.getRequestId(), throwable);
                        }
                    });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("Reactor {} 通道处理异常", reactorId, cause);
            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("Reactor {} 客户端连接建立: {}", reactorId, ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            logger.debug("Reactor {} 客户端连接断开: {}", reactorId, ctx.channel().remoteAddress());
            super.channelInactive(ctx);
        }
    }
} 