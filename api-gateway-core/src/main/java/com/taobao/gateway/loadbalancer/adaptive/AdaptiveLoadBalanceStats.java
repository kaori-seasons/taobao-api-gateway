package com.taobao.gateway.loadbalancer.adaptive;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自适应负载均衡统计类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class AdaptiveLoadBalanceStats {
    
    /**
     * 总请求数
     */
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    /**
     * 成功请求数
     */
    private final AtomicLong successfulRequests = new AtomicLong(0);
    
    /**
     * 失败请求数
     */
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * 平均响应时间
     */
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    /**
     * 实例选择统计
     */
    private final ConcurrentHashMap<String, InstanceStats> instanceStats = new ConcurrentHashMap<>();
    
    /**
     * 权重调整次数
     */
    private final AtomicLong weightAdjustmentCount = new AtomicLong(0);
    
    /**
     * 健康检查次数
     */
    private final AtomicLong healthCheckCount = new AtomicLong(0);
    
    /**
     * 故障转移次数
     */
    private final AtomicLong failoverCount = new AtomicLong(0);
    
    /**
     * 最后更新时间
     */
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    
    /**
     * 构造函数
     */
    public AdaptiveLoadBalanceStats() {
        this.lastUpdateTime.set(System.currentTimeMillis());
    }
    
    // Getter和Setter方法
    public long getTotalRequests() { return totalRequests.get(); }
    public void incrementTotalRequests() { totalRequests.incrementAndGet(); }
    
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public void incrementSuccessfulRequests() { successfulRequests.incrementAndGet(); }
    
    public long getFailedRequests() { return failedRequests.get(); }
    public void incrementFailedRequests() { failedRequests.incrementAndGet(); }
    
    public long getTotalResponseTime() { return totalResponseTime.get(); }
    public void addResponseTime(long responseTime) { totalResponseTime.addAndGet(responseTime); }
    
    public long getWeightAdjustmentCount() { return weightAdjustmentCount.get(); }
    public void incrementWeightAdjustmentCount() { weightAdjustmentCount.incrementAndGet(); }
    
    public long getHealthCheckCount() { return healthCheckCount.get(); }
    public void incrementHealthCheckCount() { healthCheckCount.incrementAndGet(); }
    
    public long getFailoverCount() { return failoverCount.get(); }
    public void incrementFailoverCount() { failoverCount.incrementAndGet(); }
    
    public long getLastUpdateTime() { return lastUpdateTime.get(); }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime.set(lastUpdateTime); }
    
    /**
     * 获取实例统计
     */
    public InstanceStats getInstanceStats(String instanceId) {
        return instanceStats.computeIfAbsent(instanceId, k -> new InstanceStats(instanceId));
    }
    
    /**
     * 计算平均响应时间
     */
    public double getAverageResponseTime() {
        long total = totalRequests.get();
        return total > 0 ? (double) totalResponseTime.get() / total : 0.0;
    }
    
    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total : 0.0;
    }
    
    /**
     * 计算失败率
     */
    public double getFailureRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) failedRequests.get() / total : 0.0;
    }
    
    /**
     * 计算QPS
     */
    public double getQps() {
        long total = totalRequests.get();
        long lastUpdate = lastUpdateTime.get();
        long currentTime = System.currentTimeMillis();
        long timeWindow = currentTime - lastUpdate;
        
        return timeWindow > 0 ? (double) total / (timeWindow / 1000.0) : 0.0;
    }
    
    /**
     * 重置统计信息
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        weightAdjustmentCount.set(0);
        healthCheckCount.set(0);
        failoverCount.set(0);
        lastUpdateTime.set(System.currentTimeMillis());
        instanceStats.clear();
    }
    
    /**
     * 实例统计类
     */
    public static class InstanceStats {
        private final String instanceId;
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong lastRequestTime = new AtomicLong(0);
        
        public InstanceStats(String instanceId) {
            this.instanceId = instanceId;
        }
        
        public String getInstanceId() { return instanceId; }
        
        public long getRequestCount() { return requestCount.get(); }
        public void incrementRequestCount() { requestCount.incrementAndGet(); }
        
        public long getSuccessCount() { return successCount.get(); }
        public void incrementSuccessCount() { successCount.incrementAndGet(); }
        
        public long getFailureCount() { return failureCount.get(); }
        public void incrementFailureCount() { failureCount.incrementAndGet(); }
        
        public long getTotalResponseTime() { return totalResponseTime.get(); }
        public void addResponseTime(long responseTime) { totalResponseTime.addAndGet(responseTime); }
        
        public long getLastRequestTime() { return lastRequestTime.get(); }
        public void setLastRequestTime(long lastRequestTime) { this.lastRequestTime.set(lastRequestTime); }
        
        public double getAverageResponseTime() {
            long count = requestCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0.0;
        }
        
        public double getSuccessRate() {
            long count = requestCount.get();
            return count > 0 ? (double) successCount.get() / count : 0.0;
        }
        
        public double getFailureRate() {
            long count = requestCount.get();
            return count > 0 ? (double) failureCount.get() / count : 0.0;
        }
    }
    
    @Override
    public String toString() {
        return "AdaptiveLoadBalanceStats{" +
                "totalRequests=" + totalRequests.get() +
                ", successfulRequests=" + successfulRequests.get() +
                ", failedRequests=" + failedRequests.get() +
                ", averageResponseTime=" + getAverageResponseTime() +
                ", successRate=" + getSuccessRate() +
                ", failureRate=" + getFailureRate() +
                ", qps=" + getQps() +
                ", weightAdjustmentCount=" + weightAdjustmentCount.get() +
                ", healthCheckCount=" + healthCheckCount.get() +
                ", failoverCount=" + failoverCount.get() +
                ", instanceStatsCount=" + instanceStats.size() +
                '}';
    }
} 