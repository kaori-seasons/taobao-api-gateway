package com.taobao.gateway.ratelimit.adaptive;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统指标类
 * 收集和存储系统运行时的关键指标
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class SystemMetrics {
    
    /**
     * CPU使用率 (0-100)
     */
    private final AtomicReference<Double> cpuUsage = new AtomicReference<>(0.0);
    
    /**
     * 内存使用率 (0-100)
     */
    private final AtomicReference<Double> memoryUsage = new AtomicReference<>(0.0);
    
    /**
     * 平均响应时间 (毫秒)
     */
    private final AtomicReference<Double> avgResponseTime = new AtomicReference<>(0.0);
    
    /**
     * 错误率 (0-1)
     */
    private final AtomicReference<Double> errorRate = new AtomicReference<>(0.0);
    
    /**
     * 当前并发连接数
     */
    private final AtomicLong currentConnections = new AtomicLong(0);
    
    /**
     * 最大并发连接数
     */
    private final AtomicLong maxConnections = new AtomicLong(0);
    
    /**
     * QPS (每秒查询数)
     */
    private final AtomicReference<Double> qps = new AtomicReference<>(0.0);
    
    /**
     * 队列长度
     */
    private final AtomicLong queueLength = new AtomicLong(0);
    
    /**
     * 时间戳
     */
    private final long timestamp;
    
    public SystemMetrics() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getter和Setter方法
    public double getCpuUsage() { return cpuUsage.get(); }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage.set(cpuUsage); }
    
    public double getMemoryUsage() { return memoryUsage.get(); }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage.set(memoryUsage); }
    
    public double getAvgResponseTime() { return avgResponseTime.get(); }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime.set(avgResponseTime); }
    
    public double getErrorRate() { return errorRate.get(); }
    public void setErrorRate(double errorRate) { this.errorRate.set(errorRate); }
    
    public long getCurrentConnections() { return currentConnections.get(); }
    public void setCurrentConnections(long currentConnections) { this.currentConnections.set(currentConnections); }
    
    public long getMaxConnections() { return maxConnections.get(); }
    public void setMaxConnections(long maxConnections) { this.maxConnections.set(maxConnections); }
    
    public double getQps() { return qps.get(); }
    public void setQps(double qps) { this.qps.set(qps); }
    
    public long getQueueLength() { return queueLength.get(); }
    public void setQueueLength(long queueLength) { this.queueLength.set(queueLength); }
    
    public long getTimestamp() { return timestamp; }
    
    /**
     * 计算系统负载分数 (0-100)
     * 综合考虑CPU、内存、连接数等因素
     */
    public double calculateLoadScore() {
        double cpuWeight = 0.4;
        double memoryWeight = 0.3;
        double connectionWeight = 0.3;
        
        double connectionScore = maxConnections.get() > 0 ? 
                (double) currentConnections.get() / maxConnections.get() * 100 : 0;
        
        return cpuUsage.get() * cpuWeight + 
               memoryUsage.get() * memoryWeight + 
               connectionScore * connectionWeight;
    }
    
    /**
     * 判断系统是否过载
     */
    public boolean isOverloaded() {
        return calculateLoadScore() > 80 || 
               errorRate.get() > 0.1 || 
               avgResponseTime.get() > 1000;
    }
    
    /**
     * 判断系统是否健康
     */
    public boolean isHealthy() {
        return calculateLoadScore() < 60 && 
               errorRate.get() < 0.05 && 
               avgResponseTime.get() < 500;
    }
    
    @Override
    public String toString() {
        return "SystemMetrics{" +
                "cpuUsage=" + cpuUsage.get() +
                ", memoryUsage=" + memoryUsage.get() +
                ", avgResponseTime=" + avgResponseTime.get() +
                ", errorRate=" + errorRate.get() +
                ", currentConnections=" + currentConnections.get() +
                ", maxConnections=" + maxConnections.get() +
                ", qps=" + qps.get() +
                ", queueLength=" + queueLength.get() +
                ", loadScore=" + calculateLoadScore() +
                ", timestamp=" + timestamp +
                '}';
    }
} 