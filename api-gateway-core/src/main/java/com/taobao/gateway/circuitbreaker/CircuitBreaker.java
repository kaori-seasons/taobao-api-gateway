package com.taobao.gateway.circuitbreaker;

/**
 * 熔断器接口
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface CircuitBreaker {
    
    /**
     * 尝试执行操作
     * 
     * @param operation 操作
     * @return 操作结果
     * @throws Exception 执行异常
     */
    <T> T execute(CircuitBreakerOperation<T> operation) throws Exception;
    
    /**
     * 记录成功
     */
    void recordSuccess();
    
    /**
     * 记录失败
     */
    void recordFailure();
    
    /**
     * 记录超时
     */
    void recordTimeout();
    
    /**
     * 获取熔断器状态
     * 
     * @return 熔断器状态
     */
    CircuitBreakerState getState();
    
    /**
     * 获取熔断器配置
     * 
     * @return 熔断器配置
     */
    CircuitBreakerConfig getConfig();
    
    /**
     * 重置熔断器
     */
    void reset();
} 