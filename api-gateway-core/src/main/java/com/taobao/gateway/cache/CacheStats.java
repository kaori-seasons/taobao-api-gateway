package com.taobao.gateway.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计信息类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class CacheStats {
    
    /** 命中次数 */
    private final AtomicLong hitCount = new AtomicLong(0);
    
    /** 未命中次数 */
    private final AtomicLong missCount = new AtomicLong(0);
    
    /** 加载成功次数 */
    private final AtomicLong loadSuccessCount = new AtomicLong(0);
    
    /** 加载失败次数 */
    private final AtomicLong loadFailureCount = new AtomicLong(0);
    
    /** 总加载时间（纳秒） */
    private final AtomicLong totalLoadTime = new AtomicLong(0);
    
    /** 驱逐次数 */
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    /** 一级缓存命中次数 */
    private final AtomicLong l1HitCount = new AtomicLong(0);
    
    /** 二级缓存命中次数 */
    private final AtomicLong l2HitCount = new AtomicLong(0);
    
    /** 缓存大小 */
    private volatile long size = 0;
    
    /**
     * 记录命中
     */
    public void recordHits(long count) {
        hitCount.addAndGet(count);
    }
    
    /**
     * 记录未命中
     */
    public void recordMisses(long count) {
        missCount.addAndGet(count);
    }
    
    /**
     * 记录加载成功
     */
    public void recordLoadSuccess(long loadTime) {
        loadSuccessCount.incrementAndGet();
        totalLoadTime.addAndGet(loadTime);
    }
    
    /**
     * 记录加载失败
     */
    public void recordLoadFailure(long loadTime) {
        loadFailureCount.incrementAndGet();
        totalLoadTime.addAndGet(loadTime);
    }
    
    /**
     * 记录驱逐
     */
    public void recordEviction(long count) {
        evictionCount.addAndGet(count);
    }
    
    /**
     * 记录一级缓存命中
     */
    public void recordL1Hit() {
        l1HitCount.incrementAndGet();
    }
    
    /**
     * 记录二级缓存命中
     */
    public void recordL2Hit() {
        l2HitCount.incrementAndGet();
    }
    
    /**
     * 设置缓存大小
     */
    public void setSize(long size) {
        this.size = size;
    }
    
    /**
     * 获取命中率
     */
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total == 0 ? 1.0 : (double) hitCount.get() / total;
    }
    
    /**
     * 获取平均加载时间（毫秒）
     */
    public double getAverageLoadTime() {
        long totalLoads = loadSuccessCount.get() + loadFailureCount.get();
        return totalLoads == 0 ? 0.0 : (double) totalLoadTime.get() / totalLoads / 1_000_000;
    }
    
    /**
     * 获取一级缓存命中率
     */
    public double getL1HitRate() {
        long total = l1HitCount.get() + l2HitCount.get();
        return total == 0 ? 0.0 : (double) l1HitCount.get() / total;
    }
    
    /**
     * 获取二级缓存命中率
     */
    public double getL2HitRate() {
        long total = l1HitCount.get() + l2HitCount.get();
        return total == 0 ? 0.0 : (double) l2HitCount.get() / total;
    }
    
    // Getter方法
    public long getHitCount() {
        return hitCount.get();
    }
    
    public long getMissCount() {
        return missCount.get();
    }
    
    public long getLoadSuccessCount() {
        return loadSuccessCount.get();
    }
    
    public long getLoadFailureCount() {
        return loadFailureCount.get();
    }
    
    public long getTotalLoadTime() {
        return totalLoadTime.get();
    }
    
    public long getEvictionCount() {
        return evictionCount.get();
    }
    
    public long getL1HitCount() {
        return l1HitCount.get();
    }
    
    public long getL2HitCount() {
        return l2HitCount.get();
    }
    
    public long getSize() {
        return size;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CacheStats{hitRate=%.2f%%, l1HitRate=%.2f%%, l2HitRate=%.2f%%, " +
            "avgLoadTime=%.2fms, size=%d, evictions=%d}",
            getHitRate() * 100, getL1HitRate() * 100, getL2HitRate() * 100,
            getAverageLoadTime(), size, evictionCount.get()
        );
    }
} 