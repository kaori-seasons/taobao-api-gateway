package com.taobao.gateway.ratelimit.adaptive.impl;

import com.taobao.gateway.ratelimit.RateLimiterType;
import com.taobao.gateway.ratelimit.adaptive.*;
import com.taobao.gateway.ratelimit.adaptive.strategy.ComprehensiveAdaptiveStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应限流器实现
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveRateLimiterImpl implements AdaptiveRateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveRateLimiterImpl.class);
    
    /**
     * 限流器名称
     */
    private final String name;
    
    /**
     * 自适应配置
     */
    private final AdaptiveRateLimitConfig config;
    
    /**
     * 自适应策略
     */
    private final AdaptiveStrategy strategy;
    
    /**
     * 当前限流阈值
     */
    private final AtomicInteger currentLimit;
    
    /**
     * 令牌桶
     */
    private final ConcurrentHashMap<String, TokenBucket> tokenBuckets;
    
    /**
     * 系统指标收集器
     */
    private final SystemMetricsCollector metricsCollector;
    
    /**
     * 限流统计
     */
    private final AdaptiveRateLimitStats stats;
    
    /**
     * 定时调整任务
     */
    private ScheduledFuture<?> adjustmentTask;
    
    /**
     * 调度器
     */
    private final ScheduledExecutorService scheduler;
    
    /**
     * 构造函数
     */
    public AdaptiveRateLimiterImpl(String name, AdaptiveRateLimitConfig config, 
                                  ScheduledExecutorService scheduler) {
        this.name = name;
        this.config = config;
        this.scheduler = scheduler;
        this.currentLimit = new AtomicInteger(config.getBaseLimit());
        this.tokenBuckets = new ConcurrentHashMap<>();
        this.metricsCollector = new SystemMetricsCollector();
        this.stats = new AdaptiveRateLimitStats();
        
        // 根据配置选择策略
        this.strategy = createStrategy(config.getStrategyType());
        
        // 启动自适应调整任务
        if (config.isEnabled()) {
            startAdjustmentTask();
        }
        
        logger.info("自适应限流器初始化完成: {}, 初始阈值: {}", name, config.getBaseLimit());
    }
    
    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }
    
    @Override
    public boolean tryAcquire(String key, int permits) {
        stats.incrementTotalRequests();
        
        // 获取令牌桶
        TokenBucket bucket = tokenBuckets.computeIfAbsent(key, k -> 
                new TokenBucket(currentLimit.get(), config.getBaseLimit()));
        
        // 尝试获取令牌
        boolean acquired = bucket.tryAcquire(permits);
        
        if (acquired) {
            stats.incrementPassedRequests();
            logger.debug("请求通过限流: key={}, permits={}", key, permits);
        } else {
            stats.incrementBlockedRequests();
            logger.debug("请求被限流: key={}, permits={}", key, permits);
        }
        
        return acquired;
    }
    
    @Override
    public RateLimiterType getType() {
        return RateLimiterType.ADAPTIVE;
    }
    
    @Override
    public AdaptiveRateLimitConfig getConfig() {
        return config;
    }
    
    @Override
    public AdaptiveStrategy getStrategy() {
        return strategy;
    }
    
    @Override
    public void updateMetrics(SystemMetrics metrics) {
        if (metrics != null) {
            metricsCollector.updateMetrics(metrics);
            
            // 更新统计信息
            stats.setCurrentLoadScore(metrics.calculateLoadScore());
            stats.setAvgResponseTime(metrics.getAvgResponseTime());
            stats.setErrorRate(metrics.getErrorRate());
            
            logger.debug("更新系统指标: {}", metrics);
        }
    }
    
    @Override
    public AdaptiveRateLimitConfig getAdaptiveConfig() {
        return config;
    }
    
    @Override
    public AdaptiveRateLimitStats getStats() {
        return stats;
    }
    
    /**
     * 启动自适应调整任务
     */
    private void startAdjustmentTask() {
        adjustmentTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                performAdaptiveAdjustment();
            } catch (Exception e) {
                logger.error("自适应调整任务执行异常", e);
            }
        }, config.getAdjustmentInterval(), config.getAdjustmentInterval(), TimeUnit.MILLISECONDS);
        
        logger.info("启动自适应调整任务，调整间隔: {}ms", config.getAdjustmentInterval());
    }
    
    /**
     * 执行自适应调整
     */
    private void performAdaptiveAdjustment() {
        SystemMetrics currentMetrics = metricsCollector.getCurrentMetrics();
        
        if (currentMetrics == null) {
            logger.warn("系统指标为空，跳过自适应调整");
            return;
        }
        
        int oldLimit = currentLimit.get();
        
        // 检查是否需要调整
        if (strategy.shouldAdjust(currentMetrics, oldLimit)) {
            // 计算新的限流阈值
            int newLimit = strategy.calculateLimit(currentMetrics, oldLimit);
            
            // 应用平滑调整
            if (config.isSmoothAdjustment()) {
                newLimit = applySmoothAdjustment(oldLimit, newLimit);
            }
            
            // 确保在合理范围内
            newLimit = Math.max(config.getMinLimit(), Math.min(config.getMaxLimit(), newLimit));
            
            // 更新限流阈值
            if (currentLimit.compareAndSet(oldLimit, newLimit)) {
                stats.setCurrentLimit(newLimit);
                stats.incrementAdjustmentCount();
                stats.setLastAdjustmentTime(System.currentTimeMillis());
                
                // 更新所有令牌桶的容量
                updateTokenBuckets(newLimit);
                
                logger.info("自适应调整完成 - 旧阈值: {}, 新阈值: {}, 调整次数: {}", 
                        oldLimit, newLimit, stats.getAdjustmentCount());
            }
        } else {
            logger.debug("系统状态正常，无需调整限流阈值");
        }
    }
    
    /**
     * 应用平滑调整
     */
    private int applySmoothAdjustment(int oldLimit, int newLimit) {
        double smoothFactor = config.getSmoothFactor();
        return (int) (oldLimit * (1 - smoothFactor) + newLimit * smoothFactor);
    }
    
    /**
     * 更新所有令牌桶的容量
     */
    private void updateTokenBuckets(int newLimit) {
        tokenBuckets.values().forEach(bucket -> bucket.updateCapacity(newLimit));
    }
    
    /**
     * 创建自适应策略
     */
    private AdaptiveStrategy createStrategy(AdaptiveStrategyType type) {
        switch (type) {
            case COMPREHENSIVE:
                return new ComprehensiveAdaptiveStrategy();
            case CPU_BASED:
                return new CpuBasedAdaptiveStrategy();
            case RESPONSE_TIME_BASED:
                return new ResponseTimeBasedAdaptiveStrategy();
            case ERROR_RATE_BASED:
                return new ErrorRateBasedAdaptiveStrategy();
            case LOAD_SCORE_BASED:
                return new LoadScoreBasedAdaptiveStrategy();
            default:
                logger.warn("未知的自适应策略类型: {}, 使用综合策略", type);
                return new ComprehensiveAdaptiveStrategy();
        }
    }
    
    /**
     * 关闭限流器
     */
    public void shutdown() {
        if (adjustmentTask != null && !adjustmentTask.isCancelled()) {
            adjustmentTask.cancel(false);
            logger.info("自适应限流器已关闭: {}", name);
        }
    }
    
    /**
     * 令牌桶实现
     */
    private static class TokenBucket {
        private final AtomicLong tokens;
        private final AtomicLong lastRefillTime;
        private final int capacity;
        private final long refillInterval;
        
        public TokenBucket(int capacity, int baseLimit) {
            this.capacity = capacity;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.refillInterval = 1000; // 1秒补充一次
        }
        
        public boolean tryAcquire(int permits) {
            refillTokens();
            return tokens.addAndGet(-permits) >= 0;
        }
        
        public void updateCapacity(int newCapacity) {
            long currentTokens = tokens.get();
            long newTokens = Math.min(currentTokens, newCapacity);
            tokens.set(newTokens);
        }
        
        private void refillTokens() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            
            if (now - lastRefill >= refillInterval) {
                long refillTokens = (now - lastRefill) / refillInterval;
                long currentTokens = tokens.get();
                long newTokens = Math.min(currentTokens + refillTokens, capacity);
                
                if (tokens.compareAndSet(currentTokens, newTokens)) {
                    lastRefillTime.set(now);
                }
            }
        }
    }
    
    /**
     * 系统指标收集器
     */
    private static class SystemMetricsCollector {
        private volatile SystemMetrics currentMetrics;
        
        public void updateMetrics(SystemMetrics metrics) {
            this.currentMetrics = metrics;
        }
        
        public SystemMetrics getCurrentMetrics() {
            return currentMetrics;
        }
    }
    
    // 其他策略的简单实现
    private static class CpuBasedAdaptiveStrategy implements AdaptiveStrategy {
        @Override
        public int calculateLimit(SystemMetrics metrics, int currentLimit) {
            double cpuUsage = metrics.getCpuUsage();
            if (cpuUsage > 80) {
                return (int) (currentLimit * 0.8);
            } else if (cpuUsage < 30) {
                return (int) (currentLimit * 1.2);
            }
            return currentLimit;
        }
        
        @Override
        public boolean shouldAdjust(SystemMetrics metrics, int currentLimit) {
            return metrics.getCpuUsage() > 70 || metrics.getCpuUsage() < 20;
        }
        
        @Override
        public AdaptiveStrategyType getType() {
            return AdaptiveStrategyType.CPU_BASED;
        }
    }
    
    private static class ResponseTimeBasedAdaptiveStrategy implements AdaptiveStrategy {
        @Override
        public int calculateLimit(SystemMetrics metrics, int currentLimit) {
            double responseTime = metrics.getAvgResponseTime();
            if (responseTime > 1000) {
                return (int) (currentLimit * 0.7);
            } else if (responseTime < 200) {
                return (int) (currentLimit * 1.3);
            }
            return currentLimit;
        }
        
        @Override
        public boolean shouldAdjust(SystemMetrics metrics, int currentLimit) {
            return metrics.getAvgResponseTime() > 800 || metrics.getAvgResponseTime() < 300;
        }
        
        @Override
        public AdaptiveStrategyType getType() {
            return AdaptiveStrategyType.RESPONSE_TIME_BASED;
        }
    }
    
    private static class ErrorRateBasedAdaptiveStrategy implements AdaptiveStrategy {
        @Override
        public int calculateLimit(SystemMetrics metrics, int currentLimit) {
            double errorRate = metrics.getErrorRate();
            if (errorRate > 0.1) {
                return (int) (currentLimit * 0.6);
            } else if (errorRate < 0.01) {
                return (int) (currentLimit * 1.4);
            }
            return currentLimit;
        }
        
        @Override
        public boolean shouldAdjust(SystemMetrics metrics, int currentLimit) {
            return metrics.getErrorRate() > 0.08 || metrics.getErrorRate() < 0.02;
        }
        
        @Override
        public AdaptiveStrategyType getType() {
            return AdaptiveStrategyType.ERROR_RATE_BASED;
        }
    }
    
    private static class LoadScoreBasedAdaptiveStrategy implements AdaptiveStrategy {
        @Override
        public int calculateLimit(SystemMetrics metrics, int currentLimit) {
            double loadScore = metrics.calculateLoadScore();
            if (loadScore > 80) {
                return (int) (currentLimit * 0.75);
            } else if (loadScore < 40) {
                return (int) (currentLimit * 1.25);
            }
            return currentLimit;
        }
        
        @Override
        public boolean shouldAdjust(SystemMetrics metrics, int currentLimit) {
            return metrics.calculateLoadScore() > 70 || metrics.calculateLoadScore() < 30;
        }
        
        @Override
        public AdaptiveStrategyType getType() {
            return AdaptiveStrategyType.LOAD_SCORE_BASED;
        }
    }
} 