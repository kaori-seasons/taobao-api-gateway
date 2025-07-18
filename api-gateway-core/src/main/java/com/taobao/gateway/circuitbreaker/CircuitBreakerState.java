package com.taobao.gateway.circuitbreaker;

/**
 * 熔断器状态枚举
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum CircuitBreakerState {
    
    /**
     * 关闭状态：正常执行
     */
    CLOSED("closed", "关闭状态"),
    
    /**
     * 开启状态：快速失败
     */
    OPEN("open", "开启状态"),
    
    /**
     * 半开状态：尝试恢复
     */
    HALF_OPEN("half_open", "半开状态");
    
    private final String code;
    private final String description;
    
    CircuitBreakerState(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static CircuitBreakerState fromCode(String code) {
        for (CircuitBreakerState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown circuit breaker state: " + code);
    }
} 