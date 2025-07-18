package com.taobao.gateway.sdk.config;

import java.time.Duration;

/**
 * 网关客户端配置
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class GatewayClientConfig {
    
    /**
     * 网关服务器地址
     */
    private String gatewayUrl = "http://localhost:8080";
    
    /**
     * 连接超时时间
     */
    private Duration connectTimeout = Duration.ofSeconds(5);
    
    /**
     * 读取超时时间
     */
    private Duration readTimeout = Duration.ofSeconds(30);
    
    /**
     * 写入超时时间
     */
    private Duration writeTimeout = Duration.ofSeconds(30);
    
    /**
     * 线程池大小
     */
    private int threadPoolSize = 10;
    
    /**
     * 最大连接数
     */
    private int maxConnections = 100;
    
    /**
     * 连接池空闲时间
     */
    private Duration connectionIdleTimeout = Duration.ofMinutes(5);
    
    /**
     * 重试次数
     */
    private int retryCount = 3;
    
    /**
     * 重试间隔
     */
    private Duration retryInterval = Duration.ofSeconds(1);
    
    /**
     * 是否启用压缩
     */
    private boolean enableCompression = true;
    
    /**
     * 是否启用SSL
     */
    private boolean enableSsl = false;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 默认构造函数
     */
    public GatewayClientConfig() {
    }
    
    /**
     * 构造函数
     * 
     * @param gatewayUrl 网关服务器地址
     */
    public GatewayClientConfig(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }
    
    // Getter和Setter方法
    
    public String getGatewayUrl() {
        return gatewayUrl;
    }
    
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }
    
    public Duration getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public Duration getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public Duration getWriteTimeout() {
        return writeTimeout;
    }
    
    public void setWriteTimeout(Duration writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
    
    public int getThreadPoolSize() {
        return threadPoolSize;
    }
    
    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public Duration getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }
    
    public void setConnectionIdleTimeout(Duration connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public Duration getRetryInterval() {
        return retryInterval;
    }
    
    public void setRetryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
    }
    
    public boolean isEnableCompression() {
        return enableCompression;
    }
    
    public void setEnableCompression(boolean enableCompression) {
        this.enableCompression = enableCompression;
    }
    
    public boolean isEnableSsl() {
        return enableSsl;
    }
    
    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public void setAppId(String appId) {
        this.appId = appId;
    }
    
    @Override
    public String toString() {
        return "GatewayClientConfig{" +
                "gatewayUrl='" + gatewayUrl + '\'' +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                ", writeTimeout=" + writeTimeout +
                ", threadPoolSize=" + threadPoolSize +
                ", maxConnections=" + maxConnections +
                ", connectionIdleTimeout=" + connectionIdleTimeout +
                ", retryCount=" + retryCount +
                ", retryInterval=" + retryInterval +
                ", enableCompression=" + enableCompression +
                ", enableSsl=" + enableSsl +
                ", apiKey='" + (apiKey != null ? "***" : null) + '\'' +
                ", appId='" + appId + '\'' +
                '}';
    }
} 