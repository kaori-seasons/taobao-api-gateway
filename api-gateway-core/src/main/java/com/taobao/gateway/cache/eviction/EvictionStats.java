package com.taobao.gateway.cache.eviction;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 驱逐统计信息
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class EvictionStats {
    
    /**
     * 驱逐次数
     */
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    /**
     * 命中次数
     */
    private final AtomicLong hitCount = new AtomicLong(0);
    
    /**
     * 未命中次数
     */
    private final AtomicLong missCount = new AtomicLong(0);
    
    /**
     * 设置次数
     */
    private final AtomicLong putCount = new AtomicLong(0);
    
    /**
     * 删除次数
     */
    private final AtomicLong removeCount = new AtomicLong(0);
    
    /**
     * 增加驱逐次数
     */
    public void incrementEvictionCount() {
        evictionCount.incrementAndGet();
    }
    
    /**
     * 增加命中次数
     */
    public void incrementHitCount() {
        hitCount.incrementAndGet();
    }
    
    /**
     * 增加未命中次数
     */
    public void incrementMissCount() {
        missCount.incrementAndGet();
    }
    
    /**
     * 增加设置次数
     */
    public void incrementPutCount() {
        putCount.incrementAndGet();
    }
    
    /**
     * 增加删除次数
     */
    public void incrementRemoveCount() {
        removeCount.incrementAndGet();
    }
    
    /**
     * 获取驱逐次数
     */
    public long getEvictionCount() {
        return evictionCount.get();
    }
    
    /**
     * 获取命中次数
     */
    public long getHitCount() {
        return hitCount.get();
    }
    
    /**
     * 获取未命中次数
     */
    public long getMissCount() {
        return missCount.get();
    }
    
    /**
     * 获取设置次数
     */
    public long getPutCount() {
        return putCount.get();
    }
    
    /**
     * 获取删除次数
     */
    public long getRemoveCount() {
        return removeCount.get();
    }
    
    /**
     * 获取命中率
     */
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) hitCount.get() / total : 0.0;
    }
    
    /**
     * 获取未命中率
     */
    public double getMissRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) missCount.get() / total : 0.0;
    }
    
    @Override
    public String toString() {
        return "EvictionStats{" +
                "evictionCount=" + evictionCount.get() +
                ", hitCount=" + hitCount.get() +
                ", missCount=" + missCount.get() +
                ", putCount=" + putCount.get() +
                ", removeCount=" + removeCount.get() +
                ", hitRate=" + String.format("%.2f", getHitRate()) +
                ", missRate=" + String.format("%.2f", getMissRate()) +
                '}';
    }
} 