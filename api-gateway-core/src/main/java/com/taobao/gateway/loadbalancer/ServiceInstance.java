package com.taobao.gateway.loadbalancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务实例类
 * 
 * @author taobao
 * @version 1.0.0
 * @since 2024-01-01
 */
public class ServiceInstance {
    
    /**
     * 实例ID
     */
    private String id;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 主机地址
     */
    private String host;
    
    /**
     * 端口
     */
    private int port;
    
    /**
     * 权重
     */
    private int weight = 100;
    
    /**
     * 是否健康
     */
    private boolean healthy = true;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * 当前连接数
     */
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    
    /**
     * 总请求数
     */
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    /**
     * 成功请求数
     */
    private final AtomicLong successRequests = new AtomicLong(0);
    
    /**
     * 失败请求数
     */
    private final AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * 平均响应时间（毫秒）
     */
    private final AtomicLong averageResponseTime = new AtomicLong(0);
    
    /**
     * 最后活跃时间
     */
    private volatile long lastActiveTime = System.currentTimeMillis();
    
    /**
     * 元数据
     */
    private Map<String, String> metadata = new ConcurrentHashMap<>();
    
    public ServiceInstance() {
    }
    
    public ServiceInstance(String id, String serviceName, String host, int port) {
        this.id = id;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
    }
    
    /**
     * 获取服务URL
     */
    public String getUrl() {
        return "http://" + host + ":" + port;
    }
    
    /**
     * 增加连接数
     */
    public void incrementConnections() {
        currentConnections.incrementAndGet();
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 减少连接数
     */
    public void decrementConnections() {
        currentConnections.decrementAndGet();
    }
    
    /**
     * 记录请求成功
     */
    public void recordSuccess(long responseTime) {
        totalRequests.incrementAndGet();
        successRequests.incrementAndGet();
        updateAverageResponseTime(responseTime);
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 记录请求失败
     */
    public void recordFailure() {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 更新平均响应时间
     */
    private void updateAverageResponseTime(long responseTime) {
        long currentAvg = averageResponseTime.get();
        long total = totalRequests.get();
        long newAvg = (currentAvg * (total - 1) + responseTime) / total;
        averageResponseTime.set(newAvg);
    }
    
    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) successRequests.get() / total;
    }
    
    // Getter和Setter方法
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getCurrentConnections() {
        return currentConnections.get();
    }
    
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    public long getSuccessRequests() {
        return successRequests.get();
    }
    
    public long getFailedRequests() {
        return failedRequests.get();
    }
    
    public long getAverageResponseTime() {
        return averageResponseTime.get();
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return "ServiceInstance{" +
                "id='" + id + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                ", healthy=" + healthy +
                ", enabled=" + enabled +
                ", currentConnections=" + currentConnections.get() +
                '}';
    }
} 