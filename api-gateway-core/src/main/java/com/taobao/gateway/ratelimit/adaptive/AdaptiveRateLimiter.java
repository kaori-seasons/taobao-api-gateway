package com.taobao.gateway.ratelimit.adaptive;

import com.taobao.gateway.ratelimit.RateLimiter;

/**
 * 自适应限流器接口
 * 基于系统负载、响应时间、错误率等指标动态调整限流策略
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AdaptiveRateLimiter extends RateLimiter {
    
    /**
     * 获取自适应限流策略
     * 
     * @return 自适应策略
     */
    AdaptiveStrategy getStrategy();
    
    /**
     * 更新系统指标
     * 
     * @param metrics 系统指标
     */
    void updateMetrics(SystemMetrics metrics);
    
    /**
     * 获取当前限流配置
     * 
     * @return 当前限流配置
     */
    AdaptiveRateLimitConfig getAdaptiveConfig();
    
    /**
     * 获取限流统计信息
     * 
     * @return 限流统计
     */
    AdaptiveRateLimitStats getStats();
}