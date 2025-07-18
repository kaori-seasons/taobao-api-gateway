package com.taobao.gateway.circuitbreaker;

/**
 * 熔断器操作接口
 * 定义熔断器支持的操作类型
 */
public interface CircuitBreakerOperation<T> {
    
    /**
     * 执行操作
     * @return 操作结果
     * @throws Exception 执行异常
     */
    T execute() throws Exception;
    
    /**
     * 获取操作名称
     * @return 操作名称
     */
    String getOperationName();
    
    /**
     * 获取操作超时时间（毫秒）
     * @return 超时时间
     */
    default long getTimeout() {
        return 5000; // 默认5秒
    }
} 