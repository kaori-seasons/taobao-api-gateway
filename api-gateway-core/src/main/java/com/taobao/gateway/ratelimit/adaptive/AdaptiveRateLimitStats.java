package com.taobao.gateway.ratelimit.adaptive;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自适应限流统计类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveRateLimitStats {
    
    /**
     * 总请求数
     */
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    /**
     * 通过请求数
     */
    private final AtomicLong passedRequests = new AtomicLong(0);
    
    /**
     * 被限流请求数
     */
    private final AtomicLong blockedRequests = new AtomicLong(0);
    
    /**
     * 当前限流阈值
     */
    private final AtomicInteger currentLimit = new AtomicInteger(0);
    
    /**
     * 历史限流阈值
     */
    private final AtomicInteger previousLimit = new AtomicInteger(0);
    
    /**
     * 调整次数
     */
    private final AtomicLong adjustmentCount = new AtomicLong(0);
    
    /**
     * 最后调整时间
     */
    private final AtomicLong lastAdjustmentTime = new AtomicLong(0);
    
    /**
     * 当前系统负载分数
     */
    private final AtomicReference<Double> currentLoadScore = new AtomicReference<>(0.0);
    
    /**
     * 平均响应时间
     */
    private final AtomicReference<Double> avgResponseTime = new AtomicReference<>(0.0);
    
    /**
     * 错误率
     */
    private final AtomicReference<Double> errorRate = new AtomicReference<>(0.0);
    
    /**
     * 构造函数
     */
    public AdaptiveRateLimitStats() {
        this.lastAdjustmentTime.set(System.currentTimeMillis());
    }
    
    // Getter和Setter方法
    public long getTotalRequests() { return totalRequests.get(); }
    public void incrementTotalRequests() { totalRequests.incrementAndGet(); }
    
    public long getPassedRequests() { return passedRequests.get(); }
    public void incrementPassedRequests() { passedRequests.incrementAndGet(); }
    
    public long getBlockedRequests() { return blockedRequests.get(); }
    public void incrementBlockedRequests() { blockedRequests.incrementAndGet(); }
    
    public int getCurrentLimit() { return currentLimit.get(); }
    public void setCurrentLimit(int currentLimit) { 
        this.previousLimit.set(this.currentLimit.get());
        this.currentLimit.set(currentLimit); 
    }
    
    public int getPreviousLimit() { return previousLimit.get(); }
    
    public long getAdjustmentCount() { return adjustmentCount.get(); }
    public void incrementAdjustmentCount() { adjustmentCount.incrementAndGet(); }
    
    public long getLastAdjustmentTime() { return lastAdjustmentTime.get(); }
    public void setLastAdjustmentTime(long lastAdjustmentTime) { this.lastAdjustmentTime.set(lastAdjustmentTime); }
    
    public double getCurrentLoadScore() { return currentLoadScore.get(); }
    public void setCurrentLoadScore(double currentLoadScore) { this.currentLoadScore.set(currentLoadScore); }
    
    public double getAvgResponseTime() { return avgResponseTime.get(); }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime.set(avgResponseTime); }
    
    public double getErrorRate() { return errorRate.get(); }
    public void setErrorRate(double errorRate) { this.errorRate.set(errorRate); }
    
    /**
     * 计算通过率
     */
    public double getPassRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) passedRequests.get() / total : 0.0;
    }
    
    /**
     * 计算限流率
     */
    public double getBlockRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) blockedRequests.get() / total : 0.0;
    }
    
    /**
     * 计算QPS
     */
    public double getQps() {
        long total = totalRequests.get();
        long lastAdjustment = lastAdjustmentTime.get();
        long currentTime = System.currentTimeMillis();
        long timeWindow = currentTime - lastAdjustment;
        
        return timeWindow > 0 ? (double) total / (timeWindow / 1000.0) : 0.0;
    }
    
    /**
     * 计算限流阈值变化率
     */
    public double getLimitChangeRate() {
        int current = currentLimit.get();
        int previous = previousLimit.get();
        return previous > 0 ? (double) (current - previous) / previous : 0.0;
    }
    
    /**
     * 重置统计信息
     */
    public void reset() {
        totalRequests.set(0);
        passedRequests.set(0);
        blockedRequests.set(0);
        adjustmentCount.set(0);
        lastAdjustmentTime.set(System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return "AdaptiveRateLimitStats{" +
                "totalRequests=" + totalRequests.get() +
                ", passedRequests=" + passedRequests.get() +
                ", blockedRequests=" + blockedRequests.get() +
                ", currentLimit=" + currentLimit.get() +
                ", previousLimit=" + previousLimit.get() +
                ", adjustmentCount=" + adjustmentCount.get() +
                ", lastAdjustmentTime=" + lastAdjustmentTime.get() +
                ", currentLoadScore=" + currentLoadScore.get() +
                ", avgResponseTime=" + avgResponseTime.get() +
                ", errorRate=" + errorRate.get() +
                ", passRate=" + getPassRate() +
                ", blockRate=" + getBlockRate() +
                ", qps=" + getQps() +
                ", limitChangeRate=" + getLimitChangeRate() +
                '}';
    }
} 