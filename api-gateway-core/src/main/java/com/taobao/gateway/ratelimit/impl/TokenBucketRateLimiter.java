package com.taobao.gateway.ratelimit.impl;

import com.taobao.gateway.ratelimit.RateLimitConfig;
import com.taobao.gateway.ratelimit.RateLimiter;
import com.taobao.gateway.ratelimit.RateLimiterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器实现
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketRateLimiter.class);
    
    /** 令牌桶存储：key -> 令牌桶状态 */
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    /** 限流配置 */
    private final RateLimitConfig config;
    
    public TokenBucketRateLimiter() {
        this.config = new RateLimitConfig();
    }
    
    public TokenBucketRateLimiter(RateLimitConfig config) {
        this.config = config;
    }
    
    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }
    
    @Override
    public boolean tryAcquire(String key, int permits) {
        if (!config.isEnabled()) {
            return true;
        }
        
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(config));
        boolean acquired = bucket.tryAcquire(permits);
        
        if (!acquired) {
            logger.debug("限流触发: key={}, permits={}", key, permits);
        }
        
        return acquired;
    }
    
    @Override
    public RateLimiterType getType() {
        return RateLimiterType.TOKEN_BUCKET;
    }
    
    @Override
    public RateLimitConfig getConfig() {
        return config;
    }
    
    /**
     * 令牌桶内部类
     */
    private static class TokenBucket {
        
        /** 当前令牌数 */
        private final AtomicLong tokens;
        
        /** 令牌桶容量 */
        private final int capacity;
        
        /** 令牌填充速率（每秒） */
        private final long refillRate;
        
        /** 上次填充时间 */
        private volatile long lastRefillTime;
        
        public TokenBucket(RateLimitConfig config) {
            this.capacity = config.getCapacity();
            this.refillRate = config.getRefillRate();
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        public boolean tryAcquire(int permits) {
            refillTokens();
            
            long currentTokens = tokens.get();
            if (currentTokens >= permits) {
                return tokens.addAndGet(-permits) >= 0;
            }
            
            return false;
        }
        
        private void refillTokens() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            
            if (elapsed > 0) {
                // 计算需要填充的令牌数
                long tokensToAdd = (elapsed * refillRate) / 1000;
                
                if (tokensToAdd > 0) {
                    long currentTokens = tokens.get();
                    long newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                    tokens.set(newTokens);
                    lastRefillTime = now;
                }
            }
        }
        
        public long getCurrentTokens() {
            refillTokens();
            return tokens.get();
        }
    }
    
    /**
     * 获取令牌桶统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalBuckets", buckets.size());
        stats.put("config", config);
        
        // 统计当前令牌数
        long totalTokens = buckets.values().stream()
                .mapToLong(TokenBucket::getCurrentTokens)
                .sum();
        stats.put("totalTokens", totalTokens);
        
        return stats;
    }
} 