package com.taobao.gateway.ratelimit;

/**
 * 限流器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface RateLimiter {
    
    /**
     * 尝试获取令牌
     * 
     * @param key 限流键（如IP、用户ID、接口路径等）
     * @return 是否允许通过
     */
    boolean tryAcquire(String key);
    
    /**
     * 尝试获取指定数量的令牌
     * 
     * @param key 限流键
     * @param permits 令牌数量
     * @return 是否允许通过
     */
    boolean tryAcquire(String key, int permits);
    
    /**
     * 获取限流器类型
     * 
     * @return 限流器类型
     */
    RateLimiterType getType();
    
    /**
     * 获取限流配置
     * 
     * @return 限流配置
     */
    RateLimitConfig getConfig();
} 