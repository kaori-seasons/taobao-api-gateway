package com.taobao.gateway.ratelimit.adaptive;

/**
 * 自适应策略接口
 * 定义自适应算法的核心逻辑
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public interface AdaptiveStrategy {
    
    /**
     * 计算自适应限流阈值
     * 
     * @param metrics 系统指标
     * @param currentLimit 当前限流阈值
     * @return 新的限流阈值
     */
    int calculateLimit(SystemMetrics metrics, int currentLimit);
    
    /**
     * 判断是否需要调整限流策略
     * 
     * @param metrics 系统指标
     * @param currentLimit 当前限流阈值
     * @return 是否需要调整
     */
    boolean shouldAdjust(SystemMetrics metrics, int currentLimit);
    
    /**
     * 获取策略类型
     * 
     * @return 策略类型
     */
    AdaptiveStrategyType getType();
}