package com.taobao.gateway.circuitbreaker.impl;

import com.taobao.gateway.circuitbreaker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认熔断器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class DefaultCircuitBreaker implements CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultCircuitBreaker.class);
    
    /** 熔断器配置 */
    private final CircuitBreakerConfig config;
    
    /** 当前状态 */
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    
    /** 失败计数 */
    private final AtomicInteger failureCount = new AtomicInteger(0);
    
    /** 成功计数 */
    private final AtomicInteger successCount = new AtomicInteger(0);
    
    /** 总请求计数 */
    private final AtomicInteger totalCount = new AtomicInteger(0);
    
    /** 上次失败时间 */
    private volatile long lastFailureTime = 0;
    
    /** 上次状态变更时间 */
    private volatile long lastStateChangeTime = System.currentTimeMillis();
    
    public DefaultCircuitBreaker() {
        this.config = new CircuitBreakerConfig();
    }
    
    public DefaultCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
    }
    
    @Override
    public <T> T execute(CircuitBreakerOperation<T> operation) throws Exception {
        if (!config.isEnabled()) {
            return operation.execute();
        }
        
        // 检查熔断器状态
        if (state == CircuitBreakerState.OPEN) {
            // 检查是否可以进入半开状态
            if (System.currentTimeMillis() - lastFailureTime >= config.getRecoveryTime()) {
                transitionToHalfOpen();
            } else {
                throw new CircuitBreakerOpenException("熔断器已开启: " + config.getName());
            }
        }
        
        try {
            T result = operation.execute();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
    
    @Override
    public void recordSuccess() {
        if (!config.isEnabled()) {
            return;
        }
        
        totalCount.incrementAndGet();
        
        if (state == CircuitBreakerState.HALF_OPEN) {
            int success = successCount.incrementAndGet();
            if (success >= config.getSuccessThreshold()) {
                transitionToClosed();
            }
        } else {
            // 重置失败计数
            failureCount.set(0);
        }
        
        logger.debug("熔断器记录成功: {}", config.getName());
    }
    
    @Override
    public void recordFailure() {
        if (!config.isEnabled()) {
            return;
        }
        
        totalCount.incrementAndGet();
        int failures = failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        
        if (state == CircuitBreakerState.CLOSED) {
            // 检查是否需要开启熔断
            if (failures >= config.getFailureThreshold()) {
                double failureRate = (double) failures / totalCount.get();
                if (failureRate >= config.getFailureRateThreshold()) {
                    transitionToOpen();
                }
            }
        } else if (state == CircuitBreakerState.HALF_OPEN) {
            // 半开状态下失败，直接开启熔断
            transitionToOpen();
        }
        
        logger.debug("熔断器记录失败: {}, 失败次数: {}", config.getName(), failures);
    }
    
    @Override
    public void recordTimeout() {
        recordFailure();
    }
    
    @Override
    public CircuitBreakerState getState() {
        return state;
    }
    
    @Override
    public CircuitBreakerConfig getConfig() {
        return config;
    }
    
    @Override
    public void reset() {
        state = CircuitBreakerState.CLOSED;
        failureCount.set(0);
        successCount.set(0);
        totalCount.set(0);
        lastFailureTime = 0;
        lastStateChangeTime = System.currentTimeMillis();
        
        logger.info("熔断器重置: {}", config.getName());
    }
    
    /**
     * 转换到开启状态
     */
    private void transitionToOpen() {
        if (state != CircuitBreakerState.OPEN) {
            state = CircuitBreakerState.OPEN;
            lastStateChangeTime = System.currentTimeMillis();
            logger.warn("熔断器开启: {}", config.getName());
        }
    }
    
    /**
     * 转换到半开状态
     */
    private void transitionToHalfOpen() {
        if (state != CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.HALF_OPEN;
            lastStateChangeTime = System.currentTimeMillis();
            successCount.set(0);
            logger.info("熔断器进入半开状态: {}", config.getName());
        }
    }
    
    /**
     * 转换到关闭状态
     */
    private void transitionToClosed() {
        if (state != CircuitBreakerState.CLOSED) {
            state = CircuitBreakerState.CLOSED;
            lastStateChangeTime = System.currentTimeMillis();
            failureCount.set(0);
            successCount.set(0);
            logger.info("熔断器关闭: {}", config.getName());
        }
    }
    
    /**
     * 获取熔断器统计信息
     */
    public CircuitBreakerStats getStats() {
        return new CircuitBreakerStats(
                config.getName(),
                state,
                failureCount.get(),
                successCount.get(),
                totalCount.get(),
                lastFailureTime,
                lastStateChangeTime
        );
    }
    
    /**
     * 熔断器统计信息
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final int totalCount;
        private final long lastFailureTime;
        private final long lastStateChangeTime;
        
        public CircuitBreakerStats(String name, CircuitBreakerState state, int failureCount,
                                 int successCount, int totalCount, long lastFailureTime, long lastStateChangeTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.totalCount = totalCount;
            this.lastFailureTime = lastFailureTime;
            this.lastStateChangeTime = lastStateChangeTime;
        }
        
        // Getter方法
        public String getName() { return name; }
        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public int getTotalCount() { return totalCount; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getLastStateChangeTime() { return lastStateChangeTime; }
        
        public double getFailureRate() {
            return totalCount > 0 ? (double) failureCount / totalCount : 0.0;
        }
    }
} 