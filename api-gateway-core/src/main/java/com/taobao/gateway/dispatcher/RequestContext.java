package com.taobao.gateway.dispatcher;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求上下文类
 * 用于在分发层中传递请求相关的上下文信息
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RequestContext {

    /**
     * 请求ID生成器
     */
    private static final AtomicLong REQUEST_ID_GENERATOR = new AtomicLong(0);

    /**
     * 请求ID
     */
    private final String requestId;

    /**
     * 原始HTTP请求
     */
    private final FullHttpRequest request;

    /**
     * HTTP响应
     */
    private FullHttpResponse response;

    /**
     * 客户端连接通道
     */
    private final Channel clientChannel;

    /**
     * 请求开始时间戳
     */
    private final long startTime;

    /**
     * 请求结束时间戳
     */
    private long endTime;

    /**
     * 请求处理状态
     */
    private RequestStatus status = RequestStatus.PENDING;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 异常信息
     */
    private Throwable exception;

    /**
     * 自定义属性
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 路由信息
     */
    private RouteInfo routeInfo;

    /**
     * 负载均衡信息
     */
    private LoadBalanceInfo loadBalanceInfo;

    /**
     * 限流信息
     */
    private RateLimitInfo rateLimitInfo;

    /**
     * 熔断器信息
     */
    private CircuitBreakerInfo circuitBreakerInfo;

    /**
     * 构造函数
     */
    public RequestContext(FullHttpRequest request, Channel clientChannel) {
        this.requestId = generateRequestId();
        this.request = request;
        this.clientChannel = clientChannel;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + REQUEST_ID_GENERATOR.incrementAndGet();
    }

    /**
     * 获取请求ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 获取原始HTTP请求
     */
    public FullHttpRequest getRequest() {
        return request;
    }

    /**
     * 获取HTTP响应
     */
    public FullHttpResponse getResponse() {
        return response;
    }

    /**
     * 设置HTTP响应
     */
    public void setResponse(FullHttpResponse response) {
        this.response = response;
    }

    /**
     * 获取客户端连接通道
     */
    public Channel getClientChannel() {
        return clientChannel;
    }

    /**
     * 获取请求开始时间戳
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取请求结束时间戳
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * 设置请求结束时间戳
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取请求处理耗时（毫秒）
     */
    public long getProcessingTime() {
        return endTime - startTime;
    }

    /**
     * 获取请求处理状态
     */
    public RequestStatus getStatus() {
        return status;
    }

    /**
     * 设置请求处理状态
     */
    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 获取异常信息
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * 设置异常信息
     */
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    /**
     * 设置自定义属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取自定义属性（带默认值）
     */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    /**
     * 移除自定义属性
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * 获取路由信息
     */
    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    /**
     * 设置路由信息
     */
    public void setRouteInfo(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
    }

    /**
     * 获取负载均衡信息
     */
    public LoadBalanceInfo getLoadBalanceInfo() {
        return loadBalanceInfo;
    }

    /**
     * 设置负载均衡信息
     */
    public void setLoadBalanceInfo(LoadBalanceInfo loadBalanceInfo) {
        this.loadBalanceInfo = loadBalanceInfo;
    }

    /**
     * 获取限流信息
     */
    public RateLimitInfo getRateLimitInfo() {
        return rateLimitInfo;
    }

    /**
     * 设置限流信息
     */
    public void setRateLimitInfo(RateLimitInfo rateLimitInfo) {
        this.rateLimitInfo = rateLimitInfo;
    }

    /**
     * 获取熔断器信息
     */
    public CircuitBreakerInfo getCircuitBreakerInfo() {
        return circuitBreakerInfo;
    }

    /**
     * 设置熔断器信息
     */
    public void setCircuitBreakerInfo(CircuitBreakerInfo circuitBreakerInfo) {
        this.circuitBreakerInfo = circuitBreakerInfo;
    }

    /**
     * 请求处理状态枚举
     */
    public enum RequestStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        SUCCESS,    // 成功
        FAILED,     // 失败
        TIMEOUT,    // 超时
        RATE_LIMITED, // 限流
        CIRCUIT_OPEN // 熔断
    }

    /**
     * 路由信息
     */
    public static class RouteInfo {
        private String path;
        private String targetService;
        private String targetUrl;
        private int timeout;
        private Map<String, String> headers;

        public RouteInfo(String path, String targetService, String targetUrl, int timeout) {
            this.path = path;
            this.targetService = targetService;
            this.targetUrl = targetUrl;
            this.timeout = timeout;
            this.headers = new ConcurrentHashMap<>();
        }

        // Getter和Setter方法
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getTargetService() { return targetService; }
        public void setTargetService(String targetService) { this.targetService = targetService; }
        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    }

    /**
     * 负载均衡信息
     */
    public static class LoadBalanceInfo {
        private String loadBalancerType;
        private String selectedInstance;
        private String requestKey;

        public LoadBalanceInfo(String loadBalancerType, String selectedInstance, String requestKey) {
            this.loadBalancerType = loadBalancerType;
            this.selectedInstance = selectedInstance;
            this.requestKey = requestKey;
        }

        // Getter和Setter方法
        public String getLoadBalancerType() { return loadBalancerType; }
        public void setLoadBalancerType(String loadBalancerType) { this.loadBalancerType = loadBalancerType; }
        public String getSelectedInstance() { return selectedInstance; }
        public void setSelectedInstance(String selectedInstance) { this.selectedInstance = selectedInstance; }
        public String getRequestKey() { return requestKey; }
        public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    }

    /**
     * 限流信息
     */
    public static class RateLimitInfo {
        private boolean limited;
        private String limitType;
        private int currentQps;
        private int maxQps;

        public RateLimitInfo(boolean limited, String limitType, int currentQps, int maxQps) {
            this.limited = limited;
            this.limitType = limitType;
            this.currentQps = currentQps;
            this.maxQps = maxQps;
        }

        // Getter和Setter方法
        public boolean isLimited() { return limited; }
        public void setLimited(boolean limited) { this.limited = limited; }
        public String getLimitType() { return limitType; }
        public void setLimitType(String limitType) { this.limitType = limitType; }
        public int getCurrentQps() { return currentQps; }
        public void setCurrentQps(int currentQps) { this.currentQps = currentQps; }
        public int getMaxQps() { return maxQps; }
        public void setMaxQps(int maxQps) { this.maxQps = maxQps; }
    }

    /**
     * 熔断器信息
     */
    public static class CircuitBreakerInfo {
        private String circuitBreakerState;
        private int failureCount;
        private int threshold;
        private long lastFailureTime;

        public CircuitBreakerInfo(String circuitBreakerState, int failureCount, int threshold, long lastFailureTime) {
            this.circuitBreakerState = circuitBreakerState;
            this.failureCount = failureCount;
            this.threshold = threshold;
            this.lastFailureTime = lastFailureTime;
        }

        // Getter和Setter方法
        public String getCircuitBreakerState() { return circuitBreakerState; }
        public void setCircuitBreakerState(String circuitBreakerState) { this.circuitBreakerState = circuitBreakerState; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public long getLastFailureTime() { return lastFailureTime; }
        public void setLastFailureTime(long lastFailureTime) { this.lastFailureTime = lastFailureTime; }
    }

    @Override
    public String toString() {
        return "RequestContext{" +
                "requestId='" + requestId + '\'' +
                ", status=" + status +
                ", processingTime=" + getProcessingTime() + "ms" +
                ", path='" + (request != null ? request.uri() : "null") + '\'' +
                '}';
    }
}