package com.taobao.gateway.dispatcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 分发层配置类
 * 定义基于Netty NIO+Reactor模型的分发层核心配置
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.dispatcher")
public class DispatcherConfig {

    /**
     * 是否启用分发层
     */
    private boolean enabled = true;

    /**
     * 主Reactor线程数（接收连接的线程）
     */
    private int mainReactorThreads = 1;

    /**
     * 子Reactor线程数（处理IO的线程）
     */
    private int subReactorThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 业务处理线程池大小
     */
    private int businessThreadPoolSize = Runtime.getRuntime().availableProcessors() * 4;

    /**
     * 连接队列大小
     */
    private int backlog = 1024;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 60000;

    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 60000;

    /**
     * 最大请求体大小（字节）
     */
    private int maxContentLength = 1024 * 1024; // 1MB

    /**
     * 是否启用TCP_NODELAY
     */
    private boolean tcpNoDelay = true;

    /**
     * 是否启用SO_KEEPALIVE
     */
    private boolean keepAlive = true;

    /**
     * 是否启用SO_REUSEADDR
     */
    private boolean reuseAddr = true;

    /**
     * 是否启用零拷贝
     */
    private boolean zeroCopy = true;

    /**
     * 是否启用直接内存
     */
    private boolean directBuffer = true;

    /**
     * 缓冲区大小（字节）
     */
    private int bufferSize = 8192;

    /**
     * 是否启用连接池
     */
    private boolean connectionPoolEnabled = true;

    /**
     * 连接池最大连接数
     */
    private int maxConnections = 10000;

    /**
     * 连接池空闲连接数
     */
    private int idleConnections = 1000;

    /**
     * 连接池连接超时时间（毫秒）
     */
    private int poolConnectionTimeout = 5000;

    /**
     * 是否启用请求限流
     */
    private boolean rateLimitEnabled = true;

    /**
     * 默认QPS限制
     */
    private int defaultQps = 10000;

    /**
     * 是否启用熔断器
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * 是否启用监控统计
     */
    private boolean metricsEnabled = true;

    // Getter和Setter方法
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMainReactorThreads() {
        return mainReactorThreads;
    }

    public void setMainReactorThreads(int mainReactorThreads) {
        this.mainReactorThreads = mainReactorThreads;
    }

    public int getSubReactorThreads() {
        return subReactorThreads;
    }

    public void setSubReactorThreads(int subReactorThreads) {
        this.subReactorThreads = subReactorThreads;
    }

    public int getBusinessThreadPoolSize() {
        return businessThreadPoolSize;
    }

    public void setBusinessThreadPoolSize(int businessThreadPoolSize) {
        this.businessThreadPoolSize = businessThreadPoolSize;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isReuseAddr() {
        return reuseAddr;
    }

    public void setReuseAddr(boolean reuseAddr) {
        this.reuseAddr = reuseAddr;
    }

    public boolean isZeroCopy() {
        return zeroCopy;
    }

    public void setZeroCopy(boolean zeroCopy) {
        this.zeroCopy = zeroCopy;
    }

    public boolean isDirectBuffer() {
        return directBuffer;
    }

    public void setDirectBuffer(boolean directBuffer) {
        this.directBuffer = directBuffer;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isConnectionPoolEnabled() {
        return connectionPoolEnabled;
    }

    public void setConnectionPoolEnabled(boolean connectionPoolEnabled) {
        this.connectionPoolEnabled = connectionPoolEnabled;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public void setIdleConnections(int idleConnections) {
        this.idleConnections = idleConnections;
    }

    public int getPoolConnectionTimeout() {
        return poolConnectionTimeout;
    }

    public void setPoolConnectionTimeout(int poolConnectionTimeout) {
        this.poolConnectionTimeout = poolConnectionTimeout;
    }

    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    public void setRateLimitEnabled(boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    public int getDefaultQps() {
        return defaultQps;
    }

    public void setDefaultQps(int defaultQps) {
        this.defaultQps = defaultQps;
    }

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    @Override
    public String toString() {
        return "DispatcherConfig{" +
                "enabled=" + enabled +
                ", mainReactorThreads=" + mainReactorThreads +
                ", subReactorThreads=" + subReactorThreads +
                ", businessThreadPoolSize=" + businessThreadPoolSize +
                ", backlog=" + backlog +
                ", connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", writeTimeout=" + writeTimeout +
                ", maxContentLength=" + maxContentLength +
                ", tcpNoDelay=" + tcpNoDelay +
                ", keepAlive=" + keepAlive +
                ", reuseAddr=" + reuseAddr +
                ", zeroCopy=" + zeroCopy +
                ", directBuffer=" + directBuffer +
                ", bufferSize=" + bufferSize +
                ", connectionPoolEnabled=" + connectionPoolEnabled +
                ", maxConnections=" + maxConnections +
                ", idleConnections=" + idleConnections +
                ", poolConnectionTimeout=" + poolConnectionTimeout +
                ", rateLimitEnabled=" + rateLimitEnabled +
                ", defaultQps=" + defaultQps +
                ", circuitBreakerEnabled=" + circuitBreakerEnabled +
                ", metricsEnabled=" + metricsEnabled +
                '}';
    }
} 