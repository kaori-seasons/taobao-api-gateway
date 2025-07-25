 
package com.taobao.gateway.loadbalancer.adaptive;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务实例指标类
 * 
 * @author taobao
 * @version 2.0.0
 * @since 2024-01-01
 */
public class InstanceMetrics {
    
    /**
     * 实例ID
     */
    private final String instanceId;
    
    /**
     * 响应时间 (毫秒)
     */
    private final AtomicReference<Double> responseTime = new AtomicReference<>(0.0);
    
    /**
     * 错误率 (0-1)
     */
    private final AtomicReference<Double> errorRate = new AtomicReference<>(0.0);
    
    /**
     * 活跃连接数
     */
    private final AtomicLong activeConnections = new AtomicLong(0);
    
    /**
     * 最大连接数
     */
    private final AtomicLong maxConnections = new AtomicLong(1000);
    
    /**
     * QPS (每秒查询数)
     */
    private final AtomicReference<Double> qps = new AtomicReference<>(0.0);
    
    /**
     * CPU使用率 (0-100)
     */
    private final AtomicReference<Double> cpuUsage = new AtomicReference<>(0.0);
    
    /**
     * 内存使用率 (0-100)
     */
    private final AtomicReference<Double> memoryUsage = new AtomicReference<>(0.0);
    
    /**
     * 健康状态
     */
    private final AtomicReference<HealthStatus> healthStatus = new AtomicReference<>(HealthStatus.HEALTHY);
    
    /**
     * 最后更新时间
     */
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    
    /**
     * 权重
     */
    private final AtomicReference<Double> weight = new AtomicReference<>(1.0);
    
    public InstanceMetrics(String instanceId) {
        this.instanceId = instanceId;
    }
    
    // Getter和Setter方法
    public String getInstanceId() { return instanceId; }
    
    public double getResponseTime() { return responseTime.get(); }
    public void setResponseTime(double responseTime) { this.responseTime.set(responseTime); }
    
    public double getErrorRate() { return errorRate.get(); }
    public void setErrorRate(double errorRate) { this.errorRate.set(errorRate); }
    
    public long getActiveConnections() { return activeConnections.get(); }
    public void setActiveConnections(long activeConnections) { this.activeConnections.set(activeConnections); }
    
    public long getMaxConnections() { return maxConnections.get(); }
    public void setMaxConnections(long maxConnections) { this.maxConnections.set(maxConnections); }
    
    public double getQps() { return qps.get(); }
    public void setQps(double qps) { this.qps.set(qps); }
    
    public double getCpuUsage() { return cpuUsage.get(); }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage.set(cpuUsage); }
    
    public double getMemoryUsage() { return memoryUsage.get(); }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage.set(memoryUsage); }
    
    public HealthStatus getHealthStatus() { return healthStatus.get(); }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus.set(healthStatus); }
    
    public long getLastUpdateTime() { return lastUpdateTime.get(); }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime.set(lastUpdateTime); }
    
    public double getWeight() { return weight.get(); }
    public void setWeight(double weight) { this.weight.set(weight); }
    
    /**
     * 计算负载分数 (0-100)
     */
    public double calculateLoadScore() {
        double connectionScore = maxConnections.get() > 0 ? 
                (double) activeConnections.get() / maxConnections.get() * 100 : 0;
        
        double cpuWeight = 0.4;
        double memoryWeight = 0.3;
        double connectionWeight = 0.3;
        
        return cpuUsage.get() * cpuWeight + 
               memoryUsage.get() * memoryWeight + 
               connectionScore * connectionWeight;
    }
    
    /**
     * 判断实例是否健康
     */
    public boolean isHealthy() {
        return healthStatus.get() == HealthStatus.HEALTHY &&
               errorRate.get() < 0.1 &&
               responseTime.get() < 1000 &&
               calculateLoadScore() < 80;
    }
    
    /**
     * 判断实例是否过载
     */
    public boolean isOverloaded() {
        return calculateLoadScore() > 80 ||
               errorRate.get() > 0.2 ||
               responseTime.get() > 2000;
    }
    
    /**
     * 计算综合评分 (用于负载均衡选择)
     */
    public double calculateScore() {
        double loadScore = calculateLoadScore();
        double errorPenalty = errorRate.get() * 100;
        double responseTimePenalty = Math.min(responseTime.get() / 100, 10);
        
        return weight.get() * (100 - loadScore - errorPenalty - responseTimePenalty);
    }
    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        HEALTHY("健康"),
        UNHEALTHY("不健康"),
        UNKNOWN("未知");
        
        private final String description;
        
        HealthStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Override
    public String toString() {
        return "InstanceMetrics{" +
                "instanceId='" + instanceId + '\'' +
                ", responseTime=" + responseTime.get() +
                ", errorRate=" + errorRate.get() +
                ", activeConnections=" + activeConnections.get() +
                ", maxConnections=" + maxConnections.get() +
                ", qps=" + qps.get() +
                ", cpuUsage=" + cpuUsage.get() +
                ", memoryUsage=" + memoryUsage.get() +
                ", healthStatus=" + healthStatus.get() +
                ", loadScore=" + calculateLoadScore() +
                ", score=" + calculateScore() +
                ", lastUpdateTime=" + lastUpdateTime.get() +
                '}';
    }
}