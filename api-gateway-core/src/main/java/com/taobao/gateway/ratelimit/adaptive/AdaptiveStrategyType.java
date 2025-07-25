package com.taobao.gateway.ratelimit.adaptive;

/**
 * 自适应策略类型枚举
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public enum AdaptiveStrategyType {
    
    /**
     * 基于CPU使用率的自适应策略
     */
    CPU_BASED("基于CPU使用率"),
    
    /**
     * 基于响应时间的自适应策略
     */
    RESPONSE_TIME_BASED("基于响应时间"),
    
    /**
     * 基于错误率的自适应策略
     */
    ERROR_RATE_BASED("基于错误率"),
    
    /**
     * 基于负载分数的自适应策略
     */
    LOAD_SCORE_BASED("基于负载分数"),
    
    /**
     * 综合自适应策略（多指标融合）
     */
    COMPREHENSIVE("综合自适应策略"),
    
    /**
     * 机器学习自适应策略
     */
    MACHINE_LEARNING("机器学习自适应策略");
    
    private final String description;
    
    AdaptiveStrategyType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 