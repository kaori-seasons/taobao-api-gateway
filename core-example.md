# 核心代码示例

## 1. Netty服务器配置

### 1.1 高性能Netty配置类
```java
package com.taobao.gateway.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Netty服务器配置类
 * 负责配置高性能的Netty服务器，支持百万级QPS
 */
@Configuration
public class NettyConfig {
    
    @Value("${netty.boss-threads:1}")
    private int bossThreads;
    
    @Value("${netty.worker-threads:16}")
    private int workerThreads;
    
    @Value("${netty.backlog:1024}")
    private int backlog;
    
    @Value("${netty.port:8080}")
    private int port;
    
    /**
     * Boss线程组 - 负责接收客户端连接
     * 通常只需要1个线程，因为只需要处理连接建立
     */
    @Bean(name = "bossGroup")
    public EventLoopGroup bossGroup() {
        return new NioEventLoopGroup(bossThreads, new DefaultThreadFactory("boss"));
    }
    
    /**
     * Worker线程组 - 负责处理客户端请求
     * 线程数通常设置为CPU核心数的2倍
     */
    @Bean(name = "workerGroup")
    public EventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerThreads, new DefaultThreadFactory("worker"));
    }
    
    /**
     * 服务器启动配置
     * 包含各种性能优化参数
     */
    @Bean
    public ServerBootstrap serverBootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 设置线程组
        bootstrap.group(bossGroup(), workerGroup())
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            
            // 服务器端配置
            .option(ChannelOption.SO_BACKLOG, backlog)           // 连接队列大小
            .option(ChannelOption.SO_REUSEADDR, true)           // 地址重用
            .option(ChannelOption.SO_KEEPALIVE, true)           // 保持连接
            
            // 客户端连接配置
            .childOption(ChannelOption.SO_KEEPALIVE, true)      // 保持连接
            .childOption(ChannelOption.TCP_NODELAY, true)       // 禁用Nagle算法
            .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)    // 接收缓冲区32KB
            .childOption(ChannelOption.SO_SNDBUF, 32 * 1024)    // 发送缓冲区32KB
            .childOption(ChannelOption.SO_REUSEADDR, true)      // 地址重用
            
            // 内存池配置 - 减少GC压力
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
            
            // 设置处理器
            .childHandler(new GatewayChannelInitializer());
            
        return bootstrap;
    }
}
```

### 1.2 通道初始化器
```java
package com.taobao.gateway.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 网关通道初始化器
 * 负责配置HTTP编解码器和业务处理器
 */
@Component
public class GatewayChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    @Autowired
    private HttpRequestHandler httpRequestHandler;
    
    @Autowired
    private MetricsHandler metricsHandler;
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            // 空闲连接检测 - 60秒没有读写操作则关闭连接
            .addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS))
            
            // HTTP编解码器
            .addLast(new HttpServerCodec())                      // HTTP请求解码器
            .addLast(new HttpObjectAggregator(65536))           // HTTP消息聚合器
            .addLast(new ChunkedWriteHandler())                 // 分块传输处理器
            
            // 业务处理器
            .addLast(metricsHandler)                            // 指标收集
            .addLast(httpRequestHandler);                       // HTTP请求处理
    }
}
```

## 2. HTTP请求处理器

### 2.1 高性能请求处理器
```java
package com.taobao.gateway.handler;

import com.taobao.gateway.filter.FilterChain;
import com.taobao.gateway.router.RouteManager;
import com.taobao.gateway.util.HttpUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP请求处理器
 * 负责处理所有HTTP请求，包括路由转发、限流、熔断等
 */
@Slf4j
@Component
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    @Autowired
    private RouteManager routeManager;
    
    @Autowired
    private FilterChain filterChain;
    
    @Autowired
    private AsyncRequestProcessor asyncProcessor;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 请求预处理
            if (!preprocessRequest(request)) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid request");
                return;
            }
            
            // 2. 异步处理请求
            CompletableFuture<FullHttpResponse> future = asyncProcessor.processRequest(request);
            
            // 3. 处理响应
            future.thenAccept(response -> {
                // 记录响应时间
                long responseTime = System.currentTimeMillis() - startTime;
                log.info("Request processed in {}ms", responseTime);
                
                // 发送响应
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }).exceptionally(throwable -> {
                // 处理异常
                log.error("Request processing failed", throwable);
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
                return null;
            });
            
        } catch (Exception e) {
            log.error("Request handling error", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
    
    /**
     * 请求预处理
     * 检查请求格式、方法等
     */
    private boolean preprocessRequest(FullHttpRequest request) {
        // 检查HTTP方法
        if (request.method() != HttpMethod.GET && 
            request.method() != HttpMethod.POST && 
            request.method() != HttpMethod.PUT && 
            request.method() != HttpMethod.DELETE) {
            return false;
        }
        
        // 检查请求路径
        String uri = request.uri();
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        
        // 检查Content-Length
        if (request.content().readableBytes() > 10 * 1024 * 1024) { // 10MB限制
            return false;
        }
        
        return true;
    }
    
    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            status,
            HttpUtil.createByteBuf(message)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Channel exception", cause);
        ctx.close();
    }
}
```

## 3. 异步请求处理器

### 3.1 异步处理链
```java
package com.taobao.gateway.handler;

import com.taobao.gateway.filter.FilterChain;
import com.taobao.gateway.router.RouteManager;
import com.taobao.gateway.util.HttpUtil;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 异步请求处理器
 * 使用CompletableFuture实现异步处理链，提高并发性能
 */
@Slf4j
@Component
public class AsyncRequestProcessor {
    
    @Autowired
    private FilterChain filterChain;
    
    @Autowired
    private RouteManager routeManager;
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    /**
     * 异步处理请求
     * 实现完整的处理链：验证 -> 过滤 -> 路由 -> 调用 -> 响应
     */
    public CompletableFuture<FullHttpResponse> processRequest(FullHttpRequest request) {
        return CompletableFuture.supplyAsync(() -> validateRequest(request), 
                threadPoolManager.getRequestExecutor())
            .thenComposeAsync(validRequest -> filterChain.doFilter(validRequest), 
                threadPoolManager.getRequestExecutor())
            .thenComposeAsync(filteredRequest -> routeRequest(filteredRequest), 
                threadPoolManager.getIoExecutor())
            .thenComposeAsync(routedRequest -> callBackendService(routedRequest), 
                threadPoolManager.getIoExecutor())
            .thenApplyAsync(this::formatResponse, 
                threadPoolManager.getRequestExecutor())
            .exceptionally(this::handleException);
    }
    
    /**
     * 请求验证
     */
    private FullHttpRequest validateRequest(FullHttpRequest request) {
        // 验证请求头
        if (!request.headers().contains(HttpHeaderNames.HOST)) {
            throw new IllegalArgumentException("Missing Host header");
        }
        
        // 验证Content-Type
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (request.method() == HttpMethod.POST && contentType == null) {
            throw new IllegalArgumentException("Missing Content-Type header");
        }
        
        return request;
    }
    
    /**
     * 路由请求
     */
    private CompletableFuture<FullHttpRequest> routeRequest(FullHttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // 查找路由
            String path = request.uri();
            Route route = routeManager.findRoute(path);
            
            if (route == null) {
                throw new RuntimeException("Route not found: " + path);
            }
            
            // 设置目标服务信息
            request.headers().set("X-Target-Service", route.getTargetService());
            request.headers().set("X-Target-Path", route.getTargetPath());
            
            return request;
        }, threadPoolManager.getIoExecutor());
    }
    
    /**
     * 调用后端服务
     */
    private CompletableFuture<HttpResponse> callBackendService(FullHttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            String targetService = request.headers().get("X-Target-Service");
            String targetPath = request.headers().get("X-Target-Path");
            
            // 使用HTTP客户端调用后端服务
            return httpClient.call(targetService, targetPath, request);
        }, threadPoolManager.getIoExecutor());
    }
    
    /**
     * 格式化响应
     */
    private FullHttpResponse formatResponse(HttpResponse backendResponse) {
        // 将后端响应转换为网关响应
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            backendResponse.status(),
            backendResponse.content()
        );
        
        // 复制响应头
        backendResponse.headers().forEach(entry -> 
            response.headers().set(entry.getKey(), entry.getValue()));
        
        // 添加网关标识
        response.headers().set("X-Gateway", "taobao-api-gateway");
        response.headers().set("X-Response-Time", String.valueOf(System.currentTimeMillis()));
        
        return response;
    }
    
    /**
     * 异常处理
     */
    private FullHttpResponse handleException(Throwable throwable) {
        log.error("Request processing exception", throwable);
        
        String errorMessage = "Internal server error";
        if (throwable instanceof IllegalArgumentException) {
            errorMessage = throwable.getMessage();
        }
        
        return HttpUtil.createErrorResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }
}
```

## 4. 线程池管理器

### 4.1 多级线程池配置
```java
package com.taobao.gateway.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 * 提供多级线程池，分别处理不同类型的任务
 */
@Slf4j
@Configuration
public class ThreadPoolManager {
    
    /**
     * 请求处理线程池
     * 处理业务逻辑，如验证、过滤、响应格式化等
     */
    @Bean("requestExecutor")
    public ExecutorService requestExecutor() {
        return new ThreadPoolExecutor(
            32,                           // 核心线程数
            64,                           // 最大线程数
            60L,                          // 空闲线程存活时间
            TimeUnit.SECONDS,             // 时间单位
            new LinkedBlockingQueue<>(10000), // 工作队列
            new ThreadFactoryBuilder()
                .setNameFormat("request-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> 
                    log.error("Request thread exception", e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    /**
     * IO处理线程池
     * 处理网络IO操作，如HTTP调用、数据库查询等
     */
    @Bean("ioExecutor")
    public ExecutorService ioExecutor() {
        return new ThreadPoolExecutor(
            16,                           // 核心线程数
            32,                           // 最大线程数
            60L,                          // 空闲线程存活时间
            TimeUnit.SECONDS,             // 时间单位
            new LinkedBlockingQueue<>(5000),  // 工作队列
            new ThreadFactoryBuilder()
                .setNameFormat("io-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> 
                    log.error("IO thread exception", e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    /**
     * 定时任务线程池
     * 处理定时任务，如指标上报、健康检查等
     */
    @Bean("scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(
            4,                            // 线程数
            new ThreadFactoryBuilder()
                .setNameFormat("scheduled-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> 
                    log.error("Scheduled thread exception", e))
                .build()
        );
    }
    
    /**
     * 监控线程池
     * 处理监控相关的异步任务
     */
    @Bean("monitorExecutor")
    public ExecutorService monitorExecutor() {
        return new ThreadPoolExecutor(
            4,                            // 核心线程数
            8,                            // 最大线程数
            60L,                          // 空闲线程存活时间
            TimeUnit.SECONDS,             // 时间单位
            new LinkedBlockingQueue<>(1000),   // 工作队列
            new ThreadFactoryBuilder()
                .setNameFormat("monitor-pool-%d")
                .setUncaughtExceptionHandler((t, e) -> 
                    log.error("Monitor thread exception", e))
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
    
    /**
     * 获取请求处理线程池
     */
    public ExecutorService getRequestExecutor() {
        return requestExecutor();
    }
    
    /**
     * 获取IO处理线程池
     */
    public ExecutorService getIoExecutor() {
        return ioExecutor();
    }
    
    /**
     * 获取定时任务线程池
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor();
    }
    
    /**
     * 获取监控线程池
     */
    public ExecutorService getMonitorExecutor() {
        return monitorExecutor();
    }
}
```

## 5. 限流器实现

### 5.1 令牌桶限流器
```java
package com.taobao.gateway.limiter;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器
 * 基于Guava RateLimiter实现，支持按路径、用户、IP等维度限流
 */
@Slf4j
@Component
public class RateLimiter {
    
    // 限流器缓存，key为限流维度，value为对应的限流器
    private final ConcurrentHashMap<String, com.google.common.util.concurrent.RateLimiter> limiters = 
        new ConcurrentHashMap<>();
    
    // 请求计数器
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong rejectCount = new AtomicLong(0);
    
    /**
     * 尝试获取令牌
     * @param key 限流维度（如路径、用户ID、IP等）
     * @param qps 每秒允许的请求数
     * @return 是否允许请求
     */
    public boolean tryAcquire(String key, double qps) {
        // 获取或创建限流器
        com.google.common.util.concurrent.RateLimiter limiter = limiters.computeIfAbsent(
            key, k -> com.google.common.util.concurrent.RateLimiter.create(qps));
        
        // 尝试获取令牌
        if (limiter.tryAcquire()) {
            requestCount.incrementAndGet();
            return true;
        } else {
            rejectCount.incrementAndGet();
            log.warn("Rate limit exceeded for key: {}, qps: {}", key, qps);
            return false;
        }
    }
    
    /**
     * 按路径限流
     */
    public boolean tryAcquireByPath(String path, double qps) {
        return tryAcquire("path:" + path, qps);
    }
    
    /**
     * 按用户限流
     */
    public boolean tryAcquireByUser(String userId, double qps) {
        return tryAcquire("user:" + userId, qps);
    }
    
    /**
     * 按IP限流
     */
    public boolean tryAcquireByIp(String ip, double qps) {
        return tryAcquire("ip:" + ip, qps);
    }
    
    /**
     * 获取请求统计
     */
    public RateLimitStats getStats() {
        return RateLimitStats.builder()
            .requestCount(requestCount.get())
            .rejectCount(rejectCount.get())
            .acceptRate(calculateAcceptRate())
            .build();
    }
    
    /**
     * 计算接受率
     */
    private double calculateAcceptRate() {
        long total = requestCount.get() + rejectCount.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) requestCount.get() / total;
    }
    
    /**
     * 清理限流器
     */
    public void clearLimiters() {
        limiters.clear();
        log.info("Rate limiters cleared");
    }
    
    /**
     * 限流统计信息
     */
    @lombok.Data
    @lombok.Builder
    public static class RateLimitStats {
        private long requestCount;    // 总请求数
        private long rejectCount;     // 拒绝请求数
        private double acceptRate;    // 接受率
    }
}
```

## 6. 熔断器实现

### 6.1 熔断器状态机
```java
package com.taobao.gateway.circuit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 熔断器实现
 * 基于状态机模式，支持自动熔断和恢复
 */
@Slf4j
@Component
public class CircuitBreaker {
    
    // 熔断器状态枚举
    public enum CircuitState {
        CLOSED,     // 关闭状态：正常处理请求
        OPEN,       // 开启状态：拒绝所有请求
        HALF_OPEN   // 半开状态：允许部分请求试探
    }
    
    // 熔断器缓存，key为服务名称
    private final ConcurrentHashMap<String, BreakerInfo> breakers = new ConcurrentHashMap<>();
    
    // 默认配置
    private static final int DEFAULT_FAILURE_THRESHOLD = 10;    // 失败阈值
    private static final int DEFAULT_SUCCESS_THRESHOLD = 5;     // 成功阈值
    private static final long DEFAULT_TIMEOUT_MS = 60000;       // 超时时间（毫秒）
    private static final double DEFAULT_HALF_OPEN_RATIO = 0.1;  // 半开状态请求比例
    
    /**
     * 是否允许请求通过
     */
    public boolean allowRequest(String serviceName) {
        BreakerInfo breaker = getOrCreateBreaker(serviceName);
        
        switch (breaker.getState()) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - breaker.getLastFailureTime() > breaker.getTimeoutMs()) {
                    // 超时后转为半开状态
                    breaker.setState(CircuitState.HALF_OPEN);
                    log.info("Circuit breaker for {} changed to HALF_OPEN", serviceName);
                    return true;
                }
                return false;
            case HALF_OPEN:
                // 半开状态下按比例允许请求
                return Math.random() < breaker.getHalfOpenRatio();
            default:
                return false;
        }
    }
    
    /**
     * 记录成功
     */
    public void recordSuccess(String serviceName) {
        BreakerInfo breaker = getOrCreateBreaker(serviceName);
        
        breaker.getSuccessCount().incrementAndGet();
        
        if (breaker.getState() == CircuitState.HALF_OPEN && 
            breaker.getSuccessCount().get() >= breaker.getSuccessThreshold()) {
            // 半开状态下成功次数达到阈值，转为关闭状态
            breaker.setState(CircuitState.CLOSED);
            breaker.getFailureCount().set(0);
            breaker.getSuccessCount().set(0);
            log.info("Circuit breaker for {} changed to CLOSED", serviceName);
        }
    }
    
    /**
     * 记录失败
     */
    public void recordFailure(String serviceName) {
        BreakerInfo breaker = getOrCreateBreaker(serviceName);
        
        breaker.getFailureCount().incrementAndGet();
        breaker.setLastFailureTime(System.currentTimeMillis());
        
        if (breaker.getState() == CircuitState.CLOSED && 
            breaker.getFailureCount().get() >= breaker.getFailureThreshold()) {
            // 关闭状态下失败次数达到阈值，转为开启状态
            breaker.setState(CircuitState.OPEN);
            log.warn("Circuit breaker for {} changed to OPEN", serviceName);
        } else if (breaker.getState() == CircuitState.HALF_OPEN) {
            // 半开状态下失败，立即转为开启状态
            breaker.setState(CircuitState.OPEN);
            log.warn("Circuit breaker for {} changed to OPEN from HALF_OPEN", serviceName);
        }
    }
    
    /**
     * 获取或创建熔断器
     */
    private BreakerInfo getOrCreateBreaker(String serviceName) {
        return breakers.computeIfAbsent(serviceName, k -> new BreakerInfo());
    }
    
    /**
     * 获取熔断器状态
     */
    public CircuitState getState(String serviceName) {
        BreakerInfo breaker = breakers.get(serviceName);
        return breaker != null ? breaker.getState() : CircuitState.CLOSED;
    }
    
    /**
     * 手动重置熔断器
     */
    public void reset(String serviceName) {
        BreakerInfo breaker = breakers.get(serviceName);
        if (breaker != null) {
            breaker.setState(CircuitState.CLOSED);
            breaker.getFailureCount().set(0);
            breaker.getSuccessCount().set(0);
            log.info("Circuit breaker for {} manually reset", serviceName);
        }
    }
    
    /**
     * 熔断器信息
     */
    private static class BreakerInfo {
        private volatile CircuitState state = CircuitState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;
        
        // 配置参数
        private final int failureThreshold = DEFAULT_FAILURE_THRESHOLD;
        private final int successThreshold = DEFAULT_SUCCESS_THRESHOLD;
        private final long timeoutMs = DEFAULT_TIMEOUT_MS;
        private final double halfOpenRatio = DEFAULT_HALF_OPEN_RATIO;
        
        // Getter和Setter方法
        public CircuitState getState() { return state; }
        public void setState(CircuitState state) { this.state = state; }
        public AtomicInteger getFailureCount() { return failureCount; }
        public AtomicInteger getSuccessCount() { return successCount; }
        public long getLastFailureTime() { return lastFailureTime; }
        public void setLastFailureTime(long lastFailureTime) { this.lastFailureTime = lastFailureTime; }
        public int getFailureThreshold() { return failureThreshold; }
        public int getSuccessThreshold() { return successThreshold; }
        public long getTimeoutMs() { return timeoutMs; }
        public double getHalfOpenRatio() { return halfOpenRatio; }
    }
}
```

## 7. 监控指标收集

### 7.1 性能指标收集器
```java
package com.taobao.gateway.metrics;

import com.codahale.metrics.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能指标收集器
 * 使用Dropwizard Metrics收集各种性能指标
 */
@Slf4j
@Component
public class MetricsCollector {
    
    // 指标注册表
    private final MetricRegistry registry = new MetricRegistry();
    
    // 计数器
    private final Counter requestCounter = registry.counter("requests.total");
    private final Counter errorCounter = registry.counter("requests.errors");
    private final Counter timeoutCounter = registry.counter("requests.timeouts");
    
    // 计时器
    private final Timer responseTimeTimer = registry.timer("response.time");
    private final Timer processingTimeTimer = registry.timer("processing.time");
    
    // 直方图
    private final Histogram requestSizeHistogram = registry.histogram("request.size");
    private final Histogram responseSizeHistogram = registry.histogram("response.size");
    
    // 仪表
    private final Gauge<Integer> activeConnectionsGauge = registry.register("connections.active", 
        () -> getActiveConnections());
    
    // 统计信息
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalTimeouts = new AtomicLong(0);
    
    /**
     * 记录请求
     */
    public void recordRequest() {
        requestCounter.inc();
        totalRequests.incrementAndGet();
    }
    
    /**
     * 记录错误
     */
    public void recordError() {
        errorCounter.inc();
        totalErrors.incrementAndGet();
    }
    
    /**
     * 记录超时
     */
    public void recordTimeout() {
        timeoutCounter.inc();
        totalTimeouts.incrementAndGet();
    }
    
    /**
     * 记录响应时间
     */
    public void recordResponseTime(long duration) {
        responseTimeTimer.update(duration, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录处理时间
     */
    public void recordProcessingTime(long duration) {
        processingTimeTimer.update(duration, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录请求大小
     */
    public void recordRequestSize(long size) {
        requestSizeHistogram.update(size);
    }
    
    /**
     * 记录响应大小
     */
    public void recordResponseSize(long size) {
        responseSizeHistogram.update(size);
    }
    
    /**
     * 获取活跃连接数
     */
    private int getActiveConnections() {
        // 这里需要从Netty获取实际的活跃连接数
        // 简化实现，实际应该从ChannelGroup获取
        return 0;
    }
    
    /**
     * 获取性能报告
     */
    public PerformanceReport getPerformanceReport() {
        Snapshot responseTimeSnapshot = responseTimeTimer.getSnapshot();
        Snapshot processingTimeSnapshot = processingTimeTimer.getSnapshot();
        
        return PerformanceReport.builder()
            .totalRequests(totalRequests.get())
            .totalErrors(totalErrors.get())
            .totalTimeouts(totalTimeouts.get())
            .errorRate(calculateErrorRate())
            .avgResponseTime(responseTimeSnapshot.getMean())
            .p95ResponseTime(responseTimeSnapshot.get95thPercentile())
            .p99ResponseTime(responseTimeSnapshot.get99thPercentile())
            .avgProcessingTime(processingTimeSnapshot.getMean())
            .p95ProcessingTime(processingTimeSnapshot.get95thPercentile())
            .qps(calculateQps())
            .build();
    }
    
    /**
     * 计算错误率
     */
    private double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalErrors.get() / total;
    }
    
    /**
     * 计算QPS
     */
    private double calculateQps() {
        // 这里需要实现QPS计算逻辑
        // 可以使用滑动窗口或计数器
        return 0.0;
    }
    
    /**
     * 获取指标注册表
     */
    public MetricRegistry getRegistry() {
        return registry;
    }
    
    /**
     * 性能报告
     */
    @lombok.Data
    @lombok.Builder
    public static class PerformanceReport {
        private long totalRequests;       // 总请求数
        private long totalErrors;         // 总错误数
        private long totalTimeouts;       // 总超时数
        private double errorRate;         // 错误率
        private double avgResponseTime;   // 平均响应时间
        private double p95ResponseTime;   // P95响应时间
        private double p99ResponseTime;   // P99响应时间
        private double avgProcessingTime; // 平均处理时间
        private double p95ProcessingTime; // P95处理时间
        private double qps;               // 每秒请求数
    }
}
```

这些核心代码示例展示了如何实现一个高性能的API网关，包括：

1. **Netty服务器配置**: 优化的网络参数和内存池配置
2. **异步请求处理**: 使用CompletableFuture实现异步处理链
3. **多级线程池**: 不同类型的任务使用不同的线程池
4. **限流熔断**: 完善的保护机制
5. **监控指标**: 全面的性能监控

通过这些优化措施，可以实现百万级QPS的处理能力。 