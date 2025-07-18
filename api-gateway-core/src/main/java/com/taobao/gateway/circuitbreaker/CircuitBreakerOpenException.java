package com.taobao.gateway.circuitbreaker;

/**
 * 熔断器开启异常
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CircuitBreakerOpenException extends RuntimeException {
    
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
    
    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
} 