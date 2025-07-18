package com.taobao.gateway.ratelimit;

/**
 * 限流器类型枚举
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum RateLimiterType {
    
    /**
     * 令牌桶算法
     */
    TOKEN_BUCKET("token_bucket", "令牌桶算法"),
    
    /**
     * 滑动窗口算法
     */
    SLIDING_WINDOW("sliding_window", "滑动窗口算法"),
    
    /**
     * 固定窗口算法
     */
    FIXED_WINDOW("fixed_window", "固定窗口算法"),
    
    /**
     * 漏桶算法
     */
    LEAKY_BUCKET("leaky_bucket", "漏桶算法");
    
    private final String code;
    private final String description;
    
    RateLimiterType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static RateLimiterType fromCode(String code) {
        for (RateLimiterType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown rate limiter type: " + code);
    }
} 