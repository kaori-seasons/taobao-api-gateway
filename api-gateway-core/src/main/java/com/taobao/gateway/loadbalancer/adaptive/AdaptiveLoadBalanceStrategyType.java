package com.taobao.gateway.loadbalancer.adaptive;

/**
 * 自适应负载均衡策略类型枚举
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public enum AdaptiveLoadBalanceStrategyType {
    
    /**
     * 基于响应时间的自适应策略
     */
    RESPONSE_TIME_BASED("基于响应时间"),
    
    /**
     * 基于负载的自适应策略
     */
    LOAD_BASED("基于负载"),
    
    /**
     * 基于错误率的自适应策略
     */
    ERROR_RATE_BASED("基于错误率"),
    
    /**
     * 基于综合评分的自适应策略
     */
    SCORE_BASED("基于综合评分"),
    
    /**
     * 基于机器学习的自适应策略
     */
    MACHINE_LEARNING("基于机器学习"),
    
    /**
     * 混合自适应策略
     */
    HYBRID("混合自适应策略");
    
    private final String description;
    
    AdaptiveLoadBalanceStrategyType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}