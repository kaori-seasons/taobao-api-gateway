package com.taobao.gateway.ratelimit;

/**
 * 限流配置类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class RateLimitConfig {
    
    /**
     * 限流键（如IP、用户ID、接口路径等）
     */
    private String key;
    
    /**
     * 限流器类型
     */
    private RateLimiterType type = RateLimiterType.TOKEN_BUCKET;
    
    /**
     * 限流维度（IP、用户、接口等）
     */
    private String dimension = "IP";
    
    /**
     * 限流阈值（每秒请求数）
     */
    private int limit = 100;
    
    /**
     * 时间窗口（秒）
     */
    private int window = 1;
    
    /**
     * 令牌桶容量
     */
    private int capacity = 100;
    
    /**
     * 令牌桶填充速率（每秒）
     */
    private int refillRate = 100;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    public RateLimitConfig() {
    }
    
    public RateLimitConfig(String key, int limit) {
        this.key = key;
        this.limit = limit;
    }
    
    public RateLimitConfig(String key, RateLimiterType type, int limit) {
        this.key = key;
        this.type = type;
        this.limit = limit;
    }
    
    // Getter和Setter方法
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public RateLimiterType getType() {
        return type;
    }
    
    public void setType(RateLimiterType type) {
        this.type = type;
    }
    
    public String getDimension() {
        return dimension;
    }
    
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getWindow() {
        return window;
    }
    
    public void setWindow(int window) {
        this.window = window;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getRefillRate() {
        return refillRate;
    }
    
    public void setRefillRate(int refillRate) {
        this.refillRate = refillRate;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "key='" + key + '\'' +
                ", type=" + type +
                ", dimension='" + dimension + '\'' +
                ", limit=" + limit +
                ", window=" + window +
                ", capacity=" + capacity +
                ", refillRate=" + refillRate +
                ", enabled=" + enabled +
                '}';
    }
} 