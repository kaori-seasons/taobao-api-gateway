package com.taobao.gateway.loadbalancer;

/**
 * 负载均衡器类型枚举
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum LoadBalancerType {
    
    /**
     * 轮询算法
     */
    ROUND_ROBIN("round_robin", "轮询算法"),
    
    /**
     * 权重轮询算法
     */
    WEIGHTED_ROUND_ROBIN("weighted_round_robin", "权重轮询算法"),
    
    /**
     * 最小连接数算法
     */
    LEAST_CONNECTIONS("least_connections", "最小连接数算法"),
    
    /**
     * 随机算法
     */
    RANDOM("random", "随机算法"),
    
    /**
     * 一致性哈希算法
     */
    CONSISTENT_HASH("consistent_hash", "一致性哈希算法");
    
    private final String code;
    private final String description;
    
    LoadBalancerType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static LoadBalancerType fromCode(String code) {
        for (LoadBalancerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown load balancer type: " + code);
    }
} 