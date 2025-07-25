package com.taobao.gateway.dispatcher.impl;

import com.taobao.gateway.cache.Cache;
import com.taobao.gateway.cache.CacheManager;
import com.taobao.gateway.circuitbreaker.CircuitBreaker;
import com.taobao.gateway.circuitbreaker.CircuitBreakerOperation;
import com.taobao.gateway.dispatcher.*;
import com.taobao.gateway.filter.FilterChain;
import com.taobao.gateway.loadbalancer.LoadBalancer;
import com.taobao.gateway.loadbalancer.LoadBalancerFactory;
import com.taobao.gateway.loadbalancer.ServiceInstance;
import com.taobao.gateway.ratelimit.RateLimiter;
import com.taobao.gateway.router.RouteManager;
import com.taobao.gateway.router.RouteResult;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 默认请求分发器实现
 * 基于Netty NIO+Reactor模型的高性能请求分发器
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultRequestDispatcher implements RequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRequestDispatcher.class);

    @Autowired
    private DispatcherConfig dispatcherConfig;

    @Autowired
    private RouteManager routeManager;

    @Autowired
    private LoadBalancerFactory loadBalancerFactory;

    @Autowired
    private FilterChain filterChain;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 业务处理线程池
     */
    private final ExecutorService businessExecutor;

    /**
     * 构造函数
     */
    public DefaultRequestDispatcher() {
        this.businessExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 4,
                r -> {
                    Thread t = new Thread(r, "dispatcher-business-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    @Override
    public CompletableFuture<RequestContext> dispatch(RequestContext context) {
        logger.debug("开始分发请求: {}", context.getRequestId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                context.setStatus(RequestContext.RequestStatus.PROCESSING);
                return context;
            } catch (Exception e) {
                logger.error("分发请求时发生错误: {}", context.getRequestId(), e);
                context.setException(e);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                return context;
            }
        }, businessExecutor)
        .thenCompose(this::process)
        .thenCompose(this::route)
        .thenCompose(this::forward)
        .thenCompose(this::handleResponse)
        .exceptionally(throwable -> {
            logger.error("请求分发过程中发生异常: {}", context.getRequestId(), throwable);
            return handleException(context, throwable).join();
        });
    }

    @Override
    public CompletableFuture<RequestContext> process(RequestContext context) {
        logger.debug("处理请求: {}", context.getRequestId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 限流检查
                if (dispatcherConfig.isRateLimitEnabled()) {
                    if (!rateLimiter.tryAcquire()) {
                        logger.warn("请求被限流: {}", context.getRequestId());
                        context.setStatus(RequestContext.RequestStatus.RATE_LIMITED);
                        context.setErrorMessage("请求频率过高，请稍后重试");
                        return context;
                    }
                }

                // 2. 熔断器检查
                if (dispatcherConfig.isCircuitBreakerEnabled()) {
                    CircuitBreakerOperation operation = circuitBreaker.createOperation("default");
                    if (!operation.canExecute()) {
                        logger.warn("服务熔断中: {}", context.getRequestId());
                        context.setStatus(RequestContext.RequestStatus.CIRCUIT_OPEN);
                        context.setErrorMessage("服务暂时不可用，请稍后重试");
                        return context;
                    }
                }

                // 3. 执行过滤器链
                FullHttpRequest request = context.getRequest();
                FullHttpResponse response = filterChain.doFilter(request);
                context.setResponse(response);

                return context;
            } catch (Exception e) {
                logger.error("处理请求时发生错误: {}", context.getRequestId(), e);
                context.setException(e);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                return context;
            }
        }, businessExecutor);
    }

    @Override
    public CompletableFuture<RequestContext> route(RequestContext context) {
        logger.debug("路由请求: {}", context.getRequestId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                FullHttpRequest request = context.getRequest();
                String path = request.uri();

                // 1. 查找路由
                RouteResult routeResult = routeManager.findRoute(path);
                if (routeResult == null || !routeResult.isMatched()) {
                    logger.warn("未找到匹配的路由: {}", path);
                    context.setStatus(RequestContext.RequestStatus.FAILED);
                    context.setErrorMessage("未找到匹配的路由");
                    return context;
                }

                // 2. 设置路由信息
                RequestContext.RouteInfo routeInfo = new RequestContext.RouteInfo(
                        path,
                        routeResult.getServiceName(),
                        routeResult.getTargetUrl(),
                        routeResult.getTimeout()
                );
                context.setRouteInfo(routeInfo);

                // 3. 负载均衡选择
                if (routeResult.getServiceName() != null) {
                    LoadBalancer loadBalancer = loadBalancerFactory.getLoadBalancer(routeResult.getLoadBalancerType());
                    List<ServiceInstance> instances = routeResult.getInstances();
                    
                    if (instances == null || instances.isEmpty()) {
                        logger.warn("没有可用的服务实例: {}", routeResult.getServiceName());
                        context.setStatus(RequestContext.RequestStatus.FAILED);
                        context.setErrorMessage("没有可用的服务实例");
                        return context;
                    }

                    // 生成请求key（用于一致性哈希等策略）
                    String requestKey = generateRequestKey(context);
                    ServiceInstance selectedInstance = loadBalancer.select(
                            routeResult.getServiceName(), 
                            instances, 
                            requestKey
                    );

                    if (selectedInstance == null) {
                        logger.warn("负载均衡器未选择到实例: {}", routeResult.getServiceName());
                        context.setStatus(RequestContext.RequestStatus.FAILED);
                        context.setErrorMessage("负载均衡器未选择到实例");
                        return context;
                    }

                    // 设置负载均衡信息
                    RequestContext.LoadBalanceInfo loadBalanceInfo = new RequestContext.LoadBalanceInfo(
                            routeResult.getLoadBalancerType(),
                            selectedInstance.getId(),
                            requestKey
                    );
                    context.setLoadBalanceInfo(loadBalanceInfo);

                    // 更新目标URL
                    String targetUrl = "http://" + selectedInstance.getHost() + ":" + selectedInstance.getPort() + path;
                    routeInfo.setTargetUrl(targetUrl);
                }

                return context;
            } catch (Exception e) {
                logger.error("路由请求时发生错误: {}", context.getRequestId(), e);
                context.setException(e);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                return context;
            }
        }, businessExecutor);
    }

    @Override
    public CompletableFuture<RequestContext> forward(RequestContext context) {
        logger.debug("转发请求: {}", context.getRequestId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                RequestContext.RouteInfo routeInfo = context.getRouteInfo();
                if (routeInfo == null) {
                    logger.warn("路由信息为空，无法转发: {}", context.getRequestId());
                    context.setStatus(RequestContext.RequestStatus.FAILED);
                    context.setErrorMessage("路由信息为空");
                    return context;
                }

                // 这里应该实现实际的HTTP转发逻辑
                // 为了简化，我们创建一个模拟的响应
                FullHttpResponse response = createMockResponse(context);
                context.setResponse(response);
                context.setStatus(RequestContext.RequestStatus.SUCCESS);

                return context;
            } catch (Exception e) {
                logger.error("转发请求时发生错误: {}", context.getRequestId(), e);
                context.setException(e);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                return context;
            }
        }, businessExecutor);
    }

    @Override
    public CompletableFuture<RequestContext> handleResponse(RequestContext context) {
        logger.debug("处理响应: {}", context.getRequestId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                context.setEndTime(System.currentTimeMillis());

                // 发送响应给客户端
                if (context.getResponse() != null && context.getClientChannel() != null) {
                    context.getClientChannel().writeAndFlush(context.getResponse())
                            .addListener(ChannelFutureListener.CLOSE);
                }

                // 记录处理结果
                if (context.getStatus() == RequestContext.RequestStatus.SUCCESS) {
                    logger.info("请求处理成功: {}, 耗时: {}ms", 
                            context.getRequestId(), context.getProcessingTime());
                } else {
                    logger.warn("请求处理失败: {}, 状态: {}, 耗时: {}ms", 
                            context.getRequestId(), context.getStatus(), context.getProcessingTime());
                }

                return context;
            } catch (Exception e) {
                logger.error("处理响应时发生错误: {}", context.getRequestId(), e);
                context.setException(e);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                return context;
            }
        }, businessExecutor);
    }

    @Override
    public CompletableFuture<RequestContext> handleException(RequestContext context, Throwable throwable) {
        logger.error("处理异常: {}", context.getRequestId(), throwable);

        return CompletableFuture.supplyAsync(() -> {
            try {
                context.setException(throwable);
                context.setStatus(RequestContext.RequestStatus.FAILED);
                context.setErrorMessage(throwable.getMessage());
                context.setEndTime(System.currentTimeMillis());

                // 创建错误响应
                FullHttpResponse errorResponse = createErrorResponse(
                        HttpResponseStatus.INTERNAL_SERVER_ERROR, 
                        "Internal Server Error: " + throwable.getMessage()
                );
                context.setResponse(errorResponse);

                // 发送错误响应
                if (context.getClientChannel() != null) {
                    context.getClientChannel().writeAndFlush(errorResponse)
                            .addListener(ChannelFutureListener.CLOSE);
                }

                return context;
            } catch (Exception e) {
                logger.error("处理异常时发生错误: {}", context.getRequestId(), e);
                return context;
            }
        }, businessExecutor);
    }

    /**
     * 生成请求key
     */
    private String generateRequestKey(RequestContext context) {
        FullHttpRequest request = context.getRequest();
        String uri = request.uri();
        String userAgent = request.headers().get(HttpHeaderNames.USER_AGENT, "");
        String remoteAddress = context.getClientChannel().remoteAddress().toString();
        
        // 简单的key生成策略，实际应用中可以根据需要调整
        return uri + "|" + userAgent + "|" + remoteAddress;
    }

    /**
     * 创建模拟响应
     */
    private FullHttpResponse createMockResponse(RequestContext context) {
        RequestContext.RouteInfo routeInfo = context.getRouteInfo();
        String responseBody = String.format(
                "{\"requestId\":\"%s\",\"targetUrl\":\"%s\",\"status\":\"success\",\"timestamp\":%d}",
                context.getRequestId(),
                routeInfo.getTargetUrl(),
                System.currentTimeMillis()
        );

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.SERVER, "Taobao-API-Gateway");

        return response;
    }

    /**
     * 创建错误响应
     */
    private FullHttpResponse createErrorResponse(HttpResponseStatus status, String message) {
        String responseBody = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                status.reasonPhrase(),
                message,
                System.currentTimeMillis()
        );

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.SERVER, "Taobao-API-Gateway");

        return response;
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        if (businessExecutor != null) {
            businessExecutor.shutdown();
            try {
                if (!businessExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    businessExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                businessExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
} 